package com.example.team4uu.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// 인형 목소리를 재생하면서, 지금 나오는 소리에 맞는 입 모양을 같이 알려준다.
//
// 🔴 다운링크는 **24kHz** 다. 업링크(마이크, 16kHz)와 다르다 —
//    16kHz 로 재생하면 인형 목소리가 낮게 늘어진다.
//
// 받는 즉시 재생한다. turn_complete 를 기다렸다가 한꺼번에 틀면 안 된다 —
// 인형 발화가 4.4초까지 늘어지는 일이 있어서(R7), 다 받고 재생하면 서버가 벌어둔
// TTFB 0.77초가 통째로 사라진다.
class DollSpeaker {

    private val _mouth = MutableStateFlow(MouthShape.CLOSED)
    val mouth: StateFlow<MouthShape> = _mouth.asStateFlow()

    private var track: AudioTrack? = null
    private var chunks: Channel<ByteArray>? = null
    private var scope: CoroutineScope? = null

    // "몇 번째 프레임까지 재생하면 이 입 모양" 목록.
    // 쓰는 쪽(writer)과 읽는 쪽(mouth 갱신)이 다른 코루틴이라 락으로 감싼다.
    private val windows = ArrayDeque<Window>()
    private var writtenFrames = 0L

    private class Window(val endFrame: Long, val shape: MouthShape)

    fun start() {
        if (track != null) return

        val player = build()
        player.play()
        track = player

        val queue = Channel<ByteArray>(Channel.UNLIMITED)
        chunks = queue

        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = newScope
        newScope.launch { writeLoop(player, queue) }
        newScope.launch { mouthLoop(player) }

        Log.i(TAG, "재생 준비 (${SAMPLE_RATE}Hz)")
    }

    // 서버에서 온 24kHz PCM 조각. 재생 순서는 넣은 순서 그대로다.
    //
    // 큐가 무제한이라 여기서는 절대 막히지 않는다 — 다운링크를 읽는 쪽이 재생 속도에
    // 발이 묶이면 그동안 서버 프레임이 쌓여서 나중에 한꺼번에 터진다.
    fun write(pcm: ByteArray) {
        if (chunks?.trySend(pcm)?.isSuccess != true) {
            Log.w(TAG, "재생 큐에 넣지 못함 — 시작되지 않았거나 이미 멈춤")
        }
    }

    fun stop() {
        // 순서가 중요하다. 큐를 먼저 닫고 pause/flush 로 대기 중인 쓰기를 풀어준 뒤에
        // 코루틴을 취소한다. 반대로 하면 재생 버퍼가 빌 때까지 붙잡혀 있다가 취소된다.
        chunks?.close()
        chunks = null

        track?.let { player ->
            runCatching {
                player.pause()
                player.flush()
                player.stop()
            }.onFailure { Log.d(TAG, "정지 무시됨: ${it.message}") }
        }

        scope?.cancel()
        scope = null

        track?.release()
        track = null

        synchronized(windows) {
            windows.clear()
            writtenFrames = 0
        }
        _mouth.value = MouthShape.CLOSED
    }

    private suspend fun writeLoop(player: AudioTrack, queue: Channel<ByteArray>) {
        for (pcm in queue) {
            enqueueWindows(pcm)

            var offset = 0
            while (offset < pcm.size && currentScopeActive()) {
                // 🔴 NON_BLOCKING 을 쓴다. 기본(블로킹) 모드로 쓰면 버퍼가 찼을 때
                //    스레드가 붙잡혀서, 대화를 끝내도 재생이 다 끝날 때까지 안 멈춘다.
                val written = player.write(
                    pcm, offset, pcm.size - offset, AudioTrack.WRITE_NON_BLOCKING
                )
                if (written < 0) {
                    Log.w(TAG, "write 실패($written)")
                    return
                }
                offset += written
                // 버퍼가 꽉 찼다. 재생이 조금 진행될 때까지 기다린다(취소 가능한 대기).
                if (written == 0) delay(WINDOW_MS.toLong())
            }
        }
    }

    // 재생 헤드를 따라가며 지금 들리는 소리의 입 모양을 고른다.
    //
    // ⚠️ 큐에 넣는 시점이 아니라 **실제로 재생되는 시점**에 맞춰야 한다.
    //    쓰자마자 입을 바꾸면 버퍼(약 200ms)만큼 입이 먼저 움직여서 어긋나 보인다.
    private suspend fun mouthLoop(player: AudioTrack) {
        while (currentScopeActive()) {
            delay(WINDOW_MS.toLong())
            val played = player.playbackHeadPosition.toLong() and 0xFFFFFFFFL
            val shape = synchronized(windows) {
                while (windows.isNotEmpty() && windows.first().endFrame <= played) {
                    windows.removeFirst()
                }
                // 남은 것 중 첫 번째가 지금 재생 중인 구간이다. 없으면 조용한 것.
                windows.firstOrNull()?.shape ?: MouthShape.CLOSED
            }
            _mouth.value = shape
        }
    }

    // 청크를 33ms 씩 잘라 각 구간의 입 모양을 미리 계산해 둔다.
    private fun enqueueWindows(pcm: ByteArray) {
        synchronized(windows) {
            var i = 0
            while (i < pcm.size) {
                val end = minOf(i + WINDOW_BYTES, pcm.size)
                writtenFrames += (end - i) / BYTES_PER_FRAME
                windows.addLast(
                    Window(writtenFrames, LipSync.shapeOf(LipSync.rms(pcm, i, end)))
                )
                i = end
            }
        }
    }

    private fun currentScopeActive() = scope?.isActive == true

    private fun build(): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    // USAGE_MEDIA 라야 스피커로 나간다. VOICE_COMMUNICATION 으로 두면
                    // 기기에 따라 통화용 수화부로 빠져서, 밥상에 세워둔 폰에서 소리가
                    // 거의 안 들린다.
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL)
                    .build()
            )
            // 약 200ms. 더 줄이면 네트워크가 잠깐 늦을 때 소리가 끊기고,
            // 늘리면 입 모양이 따라가는 지연이 커진다.
            .setBufferSizeInBytes(maxOf(minBuffer, BUFFER_BYTES))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private companion object {
        const val TAG = "DollSpeaker"

        // 🔴 서버 계약. 다운링크는 24kHz 다.
        const val SAMPLE_RATE = 24_000
        const val CHANNEL = AudioFormat.CHANNEL_OUT_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        const val BYTES_PER_FRAME = 2
        const val WINDOW_MS = LipSync.WINDOW_MS
        const val WINDOW_BYTES = SAMPLE_RATE * WINDOW_MS / 1000 * BYTES_PER_FRAME
        const val BUFFER_BYTES = SAMPLE_RATE * 200 / 1000 * BYTES_PER_FRAME
    }
}
