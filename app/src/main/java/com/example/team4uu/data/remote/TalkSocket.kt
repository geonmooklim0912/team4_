package com.example.team4uu.data.remote

import android.util.Log
import com.example.team4uu.BuildConfig
import com.example.team4uu.data.ChildProfile
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject

// WS /doll/talk 한 판. 연결하고, 프레임을 주고받고, 닫는다.
//
//   앱 ──16kHz PCM + activity_start/end──> 서버 ──> Gemini Live
//      <────── 24kHz PCM + transcript ────       <──
//
// 🔴 **연결하는 순간부터 과금이 시작된다.** 서버는 앱이 붙자마자 Live 연결을 연다
//    (첫 발화에 연결 시간을 얹지 않으려고). 화면에 들어갈 때가 아니라 **사용자가
//    대화를 시작할 때** connect() 하고, 화면을 벗어나면 반드시 닫는다.
//
// 이 클래스는 재연결하지 않는다(의도적). 세션 하나가 크레딧을 계속 먹는데 무료
// 크레딧이 $10 뿐이고 Live 분당 단가가 아직 미측정이라, 조용히 다시 붙으면
// 비용이 새는 걸 아무도 모른다. 다시 붙는 건 사용자가 명시적으로 누를 때만이다.
class TalkSocket {

    // 프레임을 보내려면 소켓 핸들이 필요하다. 콜백은 OkHttp 디스패처 스레드에서
    // 오고 send 는 UI 코루틴에서 부르므로 @Volatile 로 둔다.
    @Volatile
    private var socket: WebSocket? = null

    // 서버가 error 프레임을 보낸 뒤 닫으면 onClosing/onClosed 가 따라온다.
    // 그때 Closed 를 또 올리면 화면이 "실패"에서 "정상 종료"로 덮인다.
    @Volatile
    private var failureReported = false

    // 대화를 시작한다. **collect 하는 순간 연결된다**(cold flow).
    //
    // token 은 이 클래스가 만들지 않고 밖에서 받는다. 앱에 로그인 API 배선이 아직
    // 없어서(별도 라운드) 그 작업을 기다리지 않으려는 것이다 — 지금은 수동으로 발급한
    // JWT 를 넣어 실제 왕복을 검증할 수 있다.
    fun connect(
        token: String,
        child: ChildProfile? = null,
        dollName: String? = null
    ): Flow<TalkEvent> = callbackFlow {
        failureReported = false

        val listener = object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "연결됨")
                trySend(TalkEvent.Connected)
            }

            // 바이너리 = 인형 목소리(24kHz PCM). 그대로 재생하면 된다.
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                trySend(TalkEvent.Audio(bytes.toByteArray()))
            }

            // 텍스트 = 제어 프레임.
            override fun onMessage(webSocket: WebSocket, text: String) {
                // Moshi 대신 org.json 을 쓴다. NetworkModule 의 Moshi 인스턴스는 private 이고,
                // 그걸 꺼내려면 팀원 파일을 고쳐야 한다. 프레임이 4종뿐이라 이걸로 충분하다.
                val frame = runCatching { JSONObject(text) }.getOrNull()
                if (frame == null) {
                    // 프레임 하나 깨졌다고 대화를 끊지 않는다. 아이는 그동안 말하는 중이다.
                    Log.w(TAG, "제어 프레임 파싱 실패, 무시")
                    return
                }

                when (val type = frame.optString("type")) {
                    "transcript" -> trySend(TalkEvent.Transcript(frame.optString("text")))
                    "turn_complete" -> trySend(TalkEvent.TurnComplete)
                    "session_reset" -> {
                        Log.i(TAG, "session_reset — 인형이 이전 대화를 잊었다")
                        trySend(TalkEvent.SessionReset)
                    }

                    "error" -> {
                        val code = frame.optString("code")
                        // 서버 message 는 로그에만 남긴다. 사용자 문구는 앱이 정한다.
                        Log.w(TAG, "서버 오류 code=$code msg=${frame.optString("message")}")
                        failureReported = true
                        trySend(TalkEvent.Failed(TalkException.of(code)))
                    }

                    else -> Log.w(TAG, "알 수 없는 프레임 무시: $type")
                }
            }

            // 서버가 먼저 닫자고 했다. 답례로 닫아줘야 onClosed 까지 간다.
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "서버가 닫는 중 code=$code")
                webSocket.close(CLOSE_NORMAL, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "닫힘 code=$code")
                if (!failureReported) {
                    trySend(TalkEvent.Closed)
                }
                this@callbackFlow.close()
            }

            // 핸드셰이크 실패·연결 끊김. 여기서는 서버 code 를 알 수 없다.
            //
            // ⚠️ 인증 실패는 여기로 오지 않는다. 서버가 code 를 전달하려고 일부러
            //    accept 한 뒤 error 프레임을 보내기 때문에 위 onMessage 로 간다.
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "연결 실패 (http=${response?.code})", t)
                if (!failureReported) {
                    failureReported = true
                    trySend(TalkEvent.Failed(TalkException.of(TalkException.NETWORK, cause = t)))
                }
                this@callbackFlow.close()
            }
        }

        val url = buildUrl(token, child, dollName)
        // 🔐 URL 을 통째로 찍으면 쿼리에 JWT 와 아이 이름이 들어 있다. 경로만 남긴다.
        Log.i(TAG, "연결 시도 ${url.host}${url.encodedPath}")

        val ws = wsClient.newWebSocket(Request.Builder().url(url).build(), listener)
        socket = ws

        // 흐름을 그만 collect 하면(화면 이탈·ViewModel 정리) 여기가 불린다.
        // 🔴 이걸 빠뜨리면 화면을 나가도 Live 세션이 살아서 크레딧을 계속 먹는다.
        awaitClose {
            ws.close(CLOSE_NORMAL, null)
            socket = null
        }
    }

    // --- 업링크 -------------------------------------------------------------

    // 마이크 PCM 한 조각(16kHz / 16-bit / mono, 100ms = 3200바이트).
    // 지금은 호출부가 없다 — AudioRecord 를 붙이는 다음 라운드의 진입점이다.
    //
    // 보낼 곳이 없으면 false. 실패해도 예외를 던지지 않는 이유는, 오디오 한 조각이
    // 빠지는 건 짧은 무음일 뿐이라 대화를 끊을 이유가 못 되기 때문이다.
    fun sendAudio(pcm: ByteArray): Boolean =
        socket?.send(pcm.toByteString()) ?: false

    // 🔴 발화의 시작·끝은 **앱이 정한다.** 서버가 대신 못 한다 — 마이크가 앱에 있다.
    //    실측에서 audio_stream_end 는 응답이 아예 안 왔고(35초), 자동 VAD 는 응답
    //    완료가 18.7초로 느렸다. 수동 신호가 유일하게 동작한 조합이다.
    //
    // ⚠️ 오디오와 달리 이 두 개는 잃으면 안 된다. 턴의 시작·끝이 사라지면 그 턴이
    //    통째로 죽는다(서버도 이 신호만은 최대 3초 기다린다).
    fun sendActivityStart(): Boolean = sendControl(FRAME_ACTIVITY_START)

    fun sendActivityEnd(): Boolean = sendControl(FRAME_ACTIVITY_END)

    private fun sendControl(frame: String): Boolean {
        val sent = socket?.send(frame) ?: false
        if (!sent) Log.w(TAG, "제어 프레임을 보내지 못함 (소켓 없음)")
        return sent
    }

    // 대화를 끝낸다. 서버가 Live 를 닫아야 크레딧이 멎으므로 미루지 말 것.
    fun close() {
        socket?.close(CLOSE_NORMAL, null)
    }

    // --- 내부 ---------------------------------------------------------------

    private fun buildUrl(token: String, child: ChildProfile?, dollName: String?): HttpUrl {
        // ② 스킴을 wss:// 로 바꾸지 않는다. OkHttp 가 http(s) URL 을 받아 알아서
        //    업그레이드한다. addQueryParameter 가 percent-encoding 도 처리하므로
        //    한글 이름이 깨지지 않는다(서버 talk_client.py 의 urlencode 와 같은 결과).
        val builder = BuildConfig.API_BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments(PATH)
            // 🔴 필수. 없거나 만료면 서버가 UNAUTHORIZED + 1008 로 즉시 거절한다.
            //    stylize 와 달리 Authorization 헤더가 아니라 쿼리파라미터다 —
            //    WS 핸드셰이크에 커스텀 헤더를 못 싣는 클라이언트가 있어서 서버가 그렇게 정했다.
            .addQueryParameter("token", token)

        // 프로필이 없으면 네 개를 통째로 생략한다. 서버가 기본 페르소나('초록이', 4살)로
        // 도므로 대화 자체는 된다 — 프로필이 없다고 연결을 막지 않는다.
        if (child != null) {
            builder.addQueryParameter("child", child.name)
            builder.addQueryParameter("age", child.age.toString())
            if (child.interests.isNotEmpty()) {
                builder.addQueryParameter("interests", child.interests.joinToString(","))
            }
        }
        if (!dollName.isNullOrBlank()) {
            builder.addQueryParameter("doll", dollName)
        }
        return builder.build()
    }

    private companion object {
        const val TAG = "TalkSocket"
        const val PATH = "doll/talk"
        const val CLOSE_NORMAL = 1000

        // 서버가 보고 있는 값. 오타가 나면 "알 수 없는 제어 프레임"으로 조용히 버려진다.
        const val FRAME_ACTIVITY_START = """{"type":"activity_start"}"""
        const val FRAME_ACTIVITY_END = """{"type":"activity_end"}"""

        // 🔴 WS 전용 클라이언트. 이게 이 파일에서 가장 중요하다.
        //
        // NetworkModule.client 는 stylize(28초)에 맞춘 값이라 callTimeout 150초 /
        // readTimeout 120초다. 그대로 쓰면 **대화가 150초에 강제 종료된다.**
        // 밥 한 끼는 18분이다.
        //
        // newBuilder() 로 파생시키면 커넥션 풀과 디스패처를 그대로 공유하면서
        // 타임아웃만 갈아끼운다(팀원 파일인 NetworkModule 은 건드리지 않는다).
        val wsClient = NetworkModule.client.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)   // 무제한 — 아이가 조용한 구간이 길다
            .callTimeout(0, TimeUnit.MILLISECONDS)   // 무제한
            .pingInterval(30, TimeUnit.SECONDS)      // 중간 장비가 유휴 연결을 끊는 걸 막는다
            .build()
    }
}
