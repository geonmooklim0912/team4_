package com.example.team4uu.data.audio

import kotlin.math.sqrt

// 인형이 낸 소리의 크기로 입 모양을 고른다.
//
// AI 파트에서 미리 검증한 방식이다 — WAV 의 RMS 엔벨로프를 30fps 로 뽑으면
// closed 59 / half 14 / open 69 로 고르게 갈렸다. 즉 **음성 인식도, 추가 AI 호출도
// 필요 없다.** 재생 중인 오디오의 세기만 계산하면 된다.
//
// 스프라이트 3장은 인형을 등록할 때 이미 만들어져 기기에 저장돼 있다(SpriteStorage).
enum class MouthShape {
    CLOSED,
    HALF,
    OPEN;

    // SpriteStorage 가 파일을 저장할 때 쓴 이름. 서버 sprite_map 의 키와 같다.
    val spriteName: String
        get() = when (this) {
            CLOSED -> "mouth_closed"
            HALF -> "mouth_half"
            OPEN -> "mouth_open"
        }
}

object LipSync {

    // 30fps. 입이 초당 30번까지 바뀔 수 있다는 뜻이고, 검증도 이 간격으로 했다.
    const val WINDOW_MS = 33

    // 정규화 RMS 임계값. AI 파트 실측에서 나온 값이라 임의로 바꾸지 말 것 —
    // 올리면 입이 거의 안 열리고, 내리면 숨소리에도 벌어진다.
    private const val CLOSED_BELOW = 0.08f
    private const val HALF_BELOW = 0.30f

    // 16-bit PCM 한 조각의 세기. 0.0 ~ 1.0 으로 정규화한다.
    //
    // ⚠️ 서버가 주는 건 little-endian 16-bit mono 다. 바이트를 그대로 제곱하면
    //    (부호 없는 8비트로 읽으면) 무음 구간이 0.5 쯤으로 나와서 입이 계속 열린다.
    fun rms(pcm: ByteArray, from: Int = 0, to: Int = pcm.size): Float {
        var sum = 0.0
        var count = 0
        var i = from
        // 홀수 바이트가 남으면 마지막 하나는 버린다. 반쪽 샘플을 읽으면 값이 튄다.
        while (i + 1 < to) {
            val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort()
            val normalized = sample / 32768.0
            sum += normalized * normalized
            count++
            i += 2
        }
        return if (count == 0) 0f else sqrt(sum / count).toFloat()
    }

    fun shapeOf(rms: Float): MouthShape = when {
        rms < CLOSED_BELOW -> MouthShape.CLOSED
        rms < HALF_BELOW -> MouthShape.HALF
        else -> MouthShape.OPEN
    }
}
