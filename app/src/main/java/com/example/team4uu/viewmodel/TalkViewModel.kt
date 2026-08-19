package com.example.team4uu.viewmodel

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.team4uu.data.ChildProfileStore
import com.example.team4uu.data.remote.TalkEvent
import com.example.team4uu.data.remote.TalkException
import com.example.team4uu.data.remote.TalkSocket
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 대화 한 판의 상태를 들고 있는 곳.
//
//   Idle ──start()──> Connecting ──Connected──> Ready
//                          │                     │ startSpeaking()
//                          │                     ↓
//                          │                 Listening ──stopSpeaking()──> (activity_end)
//                          │                     │                              │
//                          │                     └──── 첫 Audio 프레임 ────> Speaking
//                          │                                                    │ turn_complete
//                          └────────── Failed <── error/onFailure               ↓
//                                                                             Ready
//
// 🔴 **start() 는 화면 진입이 아니라 "대화 시작"에 부른다.** 서버는 앱이 붙는 즉시
//    Live 연결을 열기 때문에 연결하는 순간부터 크레딧이 나간다.
class TalkViewModel(application: Application) : AndroidViewModel(application) {

    sealed interface TalkState {
        data object Idle : TalkState

        // 소켓을 여는 중. 인증 실패도 일단 열린 뒤에 error 프레임으로 오므로
        // 여기를 지났다고 대화가 되는 건 아니다.
        data object Connecting : TalkState

        // 아이가 말을 걸 수 있는 상태
        data object Ready : TalkState

        // 아이가 말하는 중 (activity_start ~ 인형의 첫 소리)
        data object Listening : TalkState

        // 인형이 말하는 중
        data object Speaking : TalkState

        data class Failed(val error: TalkException) : TalkState
    }

    private val socket = TalkSocket()
    private val childProfileStore = ChildProfileStore(application)

    private val _state = MutableStateFlow<TalkState>(TalkState.Idle)
    val state: StateFlow<TalkState> = _state.asStateFlow()

    // 인형이 지금 하고 있는 말. 서버가 조각으로 보내므로 여기서 이어붙인다.
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    // 인형이 방금 대화를 잊었다(서버가 Gemini 에 다시 붙음). 앱 WS 는 살아 있으므로
    // 끊긴 게 아니다 — 대화를 새로 시작하는 연출이 자연스럽다.
    private val _contextLost = MutableStateFlow(false)
    val contextLost: StateFlow<Boolean> = _contextLost.asStateFlow()

    // 인형 목소리(24kHz PCM). **재생은 다음 라운드다** — AudioTrack 이 여기를 구독한다.
    //
    // 지금 구독자가 없어도 대화 흐름은 그대로 돌아야 하므로 흘려보낸다(DROP_OLDEST).
    // 여기서 suspend 를 걸면 구독자가 없을 때 다운링크가 통째로 멎는다.
    private val _audio = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val audio: SharedFlow<ByteArray> = _audio.asSharedFlow()

    private var session: Job? = null

    // activity_end 를 보낸 시각. 여기부터 인형의 첫 소리까지가 TTFB 다.
    private var speechEndedAt = 0L

    // 대화를 시작한다.
    //
    // token 은 밖에서 받는다 — 앱에 로그인 API 배선이 아직 없어서(별도 라운드)
    // 이 화면이 그 작업을 기다리지 않게 하려는 것이다.
    // dollName 은 Friend.name 을 그대로 넘기면 된다.
    fun start(token: String, dollName: String? = null) {
        // 두 번 누르면 세션이 두 개 열리고 크레딧이 두 배로 나간다.
        //
        // ⚠️ null 검사가 아니라 isActive 로 본다. 끝난 Job 을 코루틴 안에서 null 로
        //    되돌리려 하면, 연결이 즉시 실패했을 때 그 대입이 아래 session= 대입보다
        //    먼저 일어나 **다시는 start() 가 안 먹는** 상태로 굳는다.
        if (session?.isActive == true) {
            Log.w(TAG, "이미 대화 중 — start() 무시")
            return
        }

        _state.value = TalkState.Connecting
        _transcript.value = ""
        _contextLost.value = false

        // 🔐 아이 이름이 들어 있다. 로그에 남기지 않는다 — 로그는 팀 전체가 보고
        //    시연 중 화면에 띄우기도 한다(서버가 safe_repr() 로 가리는 것과 같은 이유).
        val child = childProfileStore.load()

        session = viewModelScope.launch {
            socket.connect(token = token, child = child, dollName = dollName)
                .collect { onEvent(it) }
            // 흐름이 끝났다 = 소켓이 닫혔다. 실패로 끝났으면 그 상태를 덮지 않는다.
            if (_state.value !is TalkState.Failed) {
                _state.value = TalkState.Idle
            }
        }
    }

    // 아이가 말하기 시작했다 (푸시투토크 버튼을 누른 순간).
    //
    // 🔴 발화의 시작·끝은 앱이 정한다. 서버는 마이크를 못 보므로 대신 정해줄 수 없다.
    fun startSpeaking() {
        if (_state.value != TalkState.Ready) {
            Log.w(TAG, "지금은 들을 수 없음 (${_state.value})")
            return
        }
        if (socket.sendActivityStart()) {
            _state.value = TalkState.Listening
        }
    }

    // 아이가 말을 끝냈다 (버튼에서 손을 뗀 순간).
    fun stopSpeaking() {
        if (_state.value != TalkState.Listening) return
        speechEndedAt = SystemClock.elapsedRealtime()
        socket.sendActivityEnd()
        // 상태는 Listening 그대로 둔다. 인형의 첫 소리가 와야 Speaking 이다.
    }

    // 마이크 PCM 한 조각(16kHz / 16-bit / mono, 100ms = 3200바이트).
    // 지금은 호출부가 없다 — AudioRecord 를 붙이는 다음 라운드의 진입점이다.
    fun sendAudio(pcm: ByteArray) {
        socket.sendAudio(pcm)
    }

    // 대화를 끝낸다. 🔴 미루면 그동안 크레딧이 계속 나간다.
    fun stop() {
        socket.close()
        session?.cancel()
        session = null
        if (_state.value !is TalkState.Failed) {
            _state.value = TalkState.Idle
        }
    }

    // "인형이 잊었어요" 안내를 화면이 보여준 뒤 부른다.
    fun acknowledgeContextLost() {
        _contextLost.value = false
    }

    // 실패를 화면이 처리한 뒤(다시 시도·로그인 이동) 부른다.
    fun dismissError() {
        if (_state.value is TalkState.Failed) {
            _state.value = TalkState.Idle
        }
    }

    override fun onCleared() {
        // 화면이 사라져도 소켓은 알아서 닫히지 않는다. 여기서 끊지 않으면
        // 서버 MAX_TALK_SESSIONS(2)를 먹은 채로 남아 다음 대화가 TALK_BUSY 로 거절된다.
        stop()
        super.onCleared()
    }

    private fun onEvent(event: TalkEvent) {
        when (event) {
            is TalkEvent.Connected -> _state.value = TalkState.Ready

            is TalkEvent.Audio -> {
                // 인형이 입을 뗐다. turn_complete 를 기다리지 않고 바로 재생 상태로 간다 —
                // 다 받고 재생하면 TTFB 0.77초가 통째로 무의미해진다(R7).
                //
                // Ready 에서도 넘어간다. 인형이 아이보다 먼저 말을 거는 경우가 있어서
                // (인사) Listening 만 보고 있으면 그때 상태가 안 바뀐다.
                if (_state.value == TalkState.Listening || _state.value == TalkState.Ready) {
                    reportTtfb()
                    _state.value = TalkState.Speaking
                }
                _audio.tryEmit(event.pcm)
            }

            is TalkEvent.Transcript -> _transcript.value += event.text

            is TalkEvent.TurnComplete -> {
                Log.i(TAG, "턴 완료 (${_transcript.value.length}자)")
                _state.value = TalkState.Ready
            }

            is TalkEvent.SessionReset -> {
                // 서버가 Gemini 에 다시 붙었다. 소켓은 살아 있으니 상태는 유지하고
                // 맥락이 사라졌다는 것만 알린다.
                _contextLost.value = true
                _transcript.value = ""
            }

            is TalkEvent.Failed -> {
                Log.w(TAG, "대화 실패 ${event.error.code}", event.error)
                _state.value = TalkState.Failed(event.error)
            }

            is TalkEvent.Closed -> _state.value = TalkState.Idle
        }
    }

    // activity_end 를 보낸 시각 → 인형의 첫 소리. 서버 경유 실측이 0.77초였다.
    // 1.2초를 넘으면 아이가 "인형이 죽었다"로 받아들인다 — 문제로 보고 원인을 찾아야 한다.
    private fun reportTtfb() {
        if (speechEndedAt == 0L) return
        val ttfb = SystemClock.elapsedRealtime() - speechEndedAt
        speechEndedAt = 0L
        if (ttfb > TTFB_BUDGET_MS) {
            Log.w(TAG, "TTFB ${ttfb}ms — 예산(${TTFB_BUDGET_MS}ms) 초과")
        } else {
            Log.i(TAG, "TTFB ${ttfb}ms")
        }
    }

    private companion object {
        const val TAG = "TalkViewModel"
        const val TTFB_BUDGET_MS = 1_200L
    }
}
