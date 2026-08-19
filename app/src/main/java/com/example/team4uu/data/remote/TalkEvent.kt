package com.example.team4uu.data.remote

// WS /doll/talk 의 다운링크에서 올라오는 것들.
//
// 서버는 두 가지 프레임을 섞어서 보낸다 — 바이너리는 오디오, 텍스트는 제어다.
// 그 둘을 앱 코드가 한 줄기로 다룰 수 있게 하나의 타입으로 묶었다.
//
// ⚠️ 오디오는 **24kHz** 다. 업링크(16kHz)와 다르다 — 같다고 가정하고 재생하면
//    인형 목소리가 낮게 늘어진다.
sealed interface TalkEvent {

    // 소켓이 열렸다. ⚠️ 이게 "대화 준비 완료"는 아니다 — 인증 실패도 101 로 열린 뒤
    // error 프레임이 따라온다(서버가 code 를 전달하려고 일부러 accept 한다).
    data object Connected : TalkEvent

    // 인형 목소리 조각. 받는 즉시 재생해야 한다.
    //
    // turn_complete 를 기다렸다가 한꺼번에 재생하면 안 된다. 인형 발화가 4.4초까지
    // 늘어지는 일이 있어서(R7), 다 받고 재생하면 TTFB 0.77초가 통째로 무의미해진다.
    data class Audio(val pcm: ByteArray) : TalkEvent {
        // data class 가 배열에 만들어주는 equals 는 내용이 아니라 주소를 비교한다.
        // 오디오 청크를 비교할 일은 없지만, 나중에 누군가 == 로 중복을 거르려 할 때
        // 조용히 틀리는 것보다 낫다.
        override fun equals(other: Any?) =
            this === other || (other is Audio && pcm.contentEquals(other.pcm))

        override fun hashCode() = pcm.contentHashCode()
    }

    // 인형이 한 말. **한 턴이 여러 조각으로 나뉘어 온다** — 이어붙여야 문장이 된다.
    data class Transcript(val text: String) : TalkEvent

    // 한 턴 끝. 여기서 이어붙이던 조각을 확정한다.
    data object TurnComplete : TalkEvent

    // 서버가 Gemini 에 다시 붙었다. **앱 WS 는 살아 있다** — 끊긴 게 아니라
    // 이전 대화 맥락만 사라진 것이다. 아이가 조금 전에 한 말을 인형이 기억하지 못한다.
    data object SessionReset : TalkEvent

    // 서버가 보낸 error 프레임, 또는 연결 자체의 실패.
    data class Failed(val error: TalkException) : TalkEvent

    // 정상 종료. Failed 가 이미 올라간 뒤에는 오지 않는다(TalkSocket 참조).
    data object Closed : TalkEvent
}
