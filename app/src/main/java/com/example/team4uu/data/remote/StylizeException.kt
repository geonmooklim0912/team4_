package com.example.team4uu.data.remote

// 서버가 준 실패를 앱이 쓸 수 있는 형태로 바꾼 것.
//
// 서버는 모든 실패를 {code, message} 로 준다. HTTP 상태코드가 아니라 code 로 분기한다
// (같은 502 라도 GEMINI_EMPTY 와 SPRITE_INVALID 는 사용자가 해야 할 일이 다르다).
class StylizeException(
    val code: String,
    // 아이·부모에게 그대로 보여줄 문구. 서버 message 를 쓰지 않고 앱에서 정하는 이유는
    // 크레딧 소진 같은 내부 사정을 사용자에게 노출하면 안 되기 때문이다.
    val userMessage: String,
    val recovery: Recovery,
    cause: Throwable? = null
) : Exception("$code: $userMessage", cause) {

    enum class Recovery {
        // 사진을 다시 찍어야 풀린다
        RETAKE,

        // 그대로 다시 시도하면 될 수 있다
        RETRY,

        // 사용자가 할 수 있는 게 없다 (서버 설정 문제 등). 로그로만 원인을 남긴다
        FATAL
    }

    companion object {
        const val NETWORK = "NETWORK"

        fun of(code: String?, serverMessage: String? = null, cause: Throwable? = null) =
            when (code) {
                "INVALID_IMAGE" -> StylizeException(
                    code, "사진을 읽을 수 없어요.\n다시 촬영해 주세요.", Recovery.RETAKE, cause
                )

                // 누끼 결과가 전부 불투명하거나 전부 투명한 경우.
                // 흰색·크림색 인형에서 몸통이 배경으로 판정되면 여기 온다.
                "SPRITE_INVALID" -> StylizeException(
                    code,
                    "인형을 잘 오려내지 못했어요.\n밝고 단색인 배경에서 다시 찍어주세요.",
                    Recovery.RETAKE,
                    cause
                )

                "GEMINI_EMPTY", "GEMINI_TIMEOUT" -> StylizeException(
                    code, "친구를 데려오지 못했어요.\n다시 시도해 주세요.", Recovery.RETRY, cause
                )

                // 크레딧 소진·서버 키 누락. 사용자가 할 수 있는 게 없고,
                // 원인을 그대로 보여주면 아이에게도 부모에게도 도움이 안 된다.
                "QUOTA_EXHAUSTED", "MISSING_API_KEY" -> StylizeException(
                    code, "지금은 친구를 데려올 수 없어요.\n잠시 후 다시 시도해 주세요.",
                    Recovery.FATAL, cause
                )

                NETWORK -> StylizeException(
                    code, "인터넷 연결을 확인해 주세요.", Recovery.RETRY, cause
                )

                else -> StylizeException(
                    code ?: "UNKNOWN",
                    "알 수 없는 문제가 생겼어요.\n다시 시도해 주세요.",
                    Recovery.RETRY,
                    cause
                )
            }
    }
}