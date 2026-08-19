package com.example.team4uu.data.remote

// 대화 중에 생긴 실패를 앱이 쓸 수 있는 형태로 바꾼 것.
// 기존 StylizeException 과 같은 패턴이다 — 다른 점만 아래에 적었다.
//
// 🔴 **close code 가 아니라 error 프레임의 code 로 분기한다.**
//    서버는 닫기 전에 {"type":"error","code":...} 를 먼저 보낸다. close code(1008/
//    1013/1011)만 보면 "정책 위반"·"나중에 다시"·"내부 오류"까지밖에 모르고,
//    사용자가 뭘 해야 하는지는 code 에만 들어 있다.
class TalkException(
    val code: String,
    // 아이·부모에게 그대로 보여줄 문구. 서버 message 를 쓰지 않는 이유는 stylize 와 같다 —
    // 크레딧 소진 같은 내부 사정을 사용자에게 노출하면 안 된다.
    val userMessage: String,
    val recovery: Recovery,
    cause: Throwable? = null
) : Exception("$code: $userMessage", cause) {

    enum class Recovery {
        // 토큰이 없거나 만료됐다. 다시 붙어봐야 똑같이 거절당하므로 로그인부터 해야 한다.
        RELOGIN,

        // 그대로 다시 시도하면 될 수 있다
        RETRY,

        // 대화를 끝내고 MainHome 으로 돌아간다. 재시도해도 같은 실패가 반복된다
        END
    }

    companion object {
        const val NETWORK = "NETWORK"

        // 서버가 준 code 가 아니라 앱에서 만든 것. 마이크를 못 열었을 때다.
        const val MIC_UNAVAILABLE = "MIC_UNAVAILABLE"

        fun of(code: String?, serverMessage: String? = null, cause: Throwable? = null) =
            when (code) {
                // WS 는 ?token= 으로 JWT 를 받는다(헤더가 아니다). 없거나 만료·위조면
                // 서버가 accept 직후 이 code 를 보내고 1008 로 닫는다.
                //
                // ⚠️ 이걸 LIVE_UNAVAILABLE 과 묶어서 "친구와 연결하지 못했어요"로 보여주면
                //    안 된다. 토큰만 만료된 것인데 사용자도 우리도 서버를 의심하게 된다.
                "UNAUTHORIZED" -> TalkException(
                    code, "다시 로그인해 주세요.", Recovery.RELOGIN, cause
                )

                // 서버 MAX_TALK_SESSIONS(기본 2) 초과. 큐에 세우지 않고 바로 거절한다 —
                // 앞선 대화가 밥 한 끼(18분) 동안 안 끝나기 때문이다.
                "TALK_BUSY" -> TalkException(
                    code,
                    "지금은 친구가 다른 아이와 이야기 중이에요.\n잠시 후 다시 불러주세요.",
                    Recovery.RETRY,
                    cause
                )

                // Gemini Live 연결 실패. 크레딧 소진·키 누락·모델명 오류가 다 여기로 온다.
                // 사용자가 할 수 있는 게 없다.
                "LIVE_UNAVAILABLE" -> TalkException(
                    code, "친구와 연결하지 못했어요.\n잠시 후 다시 시도해 주세요.",
                    Recovery.END, cause
                )

                // 핸드셰이크 실패·연결 끊김. 서버가 준 code 가 없을 때다.
                NETWORK -> TalkException(
                    code, "인터넷 연결을 확인해 주세요.", Recovery.RETRY, cause
                )

                // 마이크 권한이 없거나 다른 앱이 마이크를 쓰고 있다.
                // 권한을 주고 다시 누르면 되므로 재시도 가능으로 둔다.
                MIC_UNAVAILABLE -> TalkException(
                    code, "마이크를 쓸 수 없어요.\n마이크 권한을 확인해 주세요.",
                    Recovery.RETRY, cause
                )

                else -> TalkException(
                    code ?: "UNKNOWN",
                    "친구와 이야기할 수 없어요.\n다시 시도해 주세요.",
                    Recovery.RETRY,
                    cause
                )
            }
    }
}
