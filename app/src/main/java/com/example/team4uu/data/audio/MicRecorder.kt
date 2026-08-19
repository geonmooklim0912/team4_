package com.example.team4uu.data.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

// 아이 목소리를 서버가 받을 수 있는 모양으로 잘라서 흘려보낸다.
//
// 🔴 규격은 서버(Gemini Live)가 정한 것이라 하나도 바꿀 수 없다.
//    16kHz / 16-bit / mono PCM, 100ms(3200바이트) 청크.
//    ⚠️ 다운링크(인형 목소리)는 24kHz 다. 같다고 착각하기 쉬운데 다르다.
//
// 마이크는 **아이가 말하는 동안에만** 돈다(푸시투토크). 계속 켜두지 않는 이유:
//   - 스피커로 나가는 인형 목소리가 다시 마이크로 들어가 인형이 자기 말에 대답한다
//   - 식사 중에는 식기·TV 소리가 섞인다(R6 미검증)
//   - 안 보내면 그만큼 크레딧을 안 쓴다
class MicRecorder {

    // 마이크를 열고 100ms 씩 끊어서 내보낸다. 수집을 멈추면(코루틴 취소) 마이크가 닫힌다.
    //
    // ⚠️ RECORD_AUDIO 권한이 없으면 여기서 MicUnavailableException 이 난다.
    //    권한 요청은 화면이 해야 한다 — 여기서는 실패를 분명히 알리는 것까지만.
    fun record(): Flow<ByteArray> = flow {
        val recorder = open()
        try {
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw MicUnavailableException("마이크를 시작하지 못했습니다")
            }
            Log.i(TAG, "녹음 시작 (${SAMPLE_RATE}Hz, ${CHUNK_BYTES}바이트 청크)")

            val chunk = ByteArray(CHUNK_BYTES)
            while (currentCoroutineContext().isActive) {
                // read 는 요청한 만큼을 한 번에 안 채워줄 수 있다. 덜 채운 채로 보내면
                // 청크 경계가 어긋나서 서버 쪽 오디오가 조금씩 밀린다.
                var filled = 0
                while (filled < CHUNK_BYTES) {
                    val read = recorder.read(chunk, filled, CHUNK_BYTES - filled)
                    if (read <= 0) {
                        Log.w(TAG, "read 실패($read) — 녹음 종료")
                        return@flow
                    }
                    filled += read
                }
                // 버퍼를 재사용하면 소비 쪽이 읽기 전에 다음 녹음이 덮어쓴다.
                emit(chunk.copyOf())
            }
        } finally {
            // 취소로 빠져나올 때도 반드시 닫는다. 안 닫으면 마이크가 물린 채로 남아
            // 다음 대화에서 열리지 않는다.
            runCatching { recorder.stop() }
            recorder.release()
            Log.i(TAG, "녹음 종료")
        }
    }.flowOn(Dispatchers.IO)

    private fun open(): AudioRecord {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuffer <= 0) {
            throw MicUnavailableException("이 기기가 ${SAMPLE_RATE}Hz 녹음을 지원하지 않습니다")
        }

        val recorder = try {
            AudioRecord.Builder()
                // VOICE_COMMUNICATION 은 기기의 에코 제거·잡음 억제를 켠다.
                // 푸시투토크라 에코는 이미 막히지만, 식사 소음에는 이쪽이 낫다.
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(ENCODING)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL)
                        .build()
                )
                // 청크 2개분은 확보한다. 최소 버퍼가 100ms 보다 작으면 읽기 사이에
                // 녹음이 밀려서 앞부분이 잘린다.
                .setBufferSizeInBytes(maxOf(minBuffer, CHUNK_BYTES * 2))
                .build()
        } catch (e: SecurityException) {
            throw MicUnavailableException("마이크 권한이 없습니다", e)
        } catch (e: UnsupportedOperationException) {
            throw MicUnavailableException("마이크를 열 수 없습니다", e)
        }

        // 권한이 없을 때 생성자가 예외 대신 '초기화 안 됨' 상태를 주는 기기가 있다.
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw MicUnavailableException("마이크가 초기화되지 않았습니다 (권한 확인)")
        }
        return recorder
    }

    private companion object {
        const val TAG = "MicRecorder"

        // 🔴 서버 계약. 업링크는 16kHz 다.
        const val SAMPLE_RATE = 16_000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        // 100ms = 1600 샘플 × 2바이트
        const val CHUNK_BYTES = 3_200
    }
}

// 마이크를 못 여는 상황. 권한 없음이 대부분이다.
class MicUnavailableException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
