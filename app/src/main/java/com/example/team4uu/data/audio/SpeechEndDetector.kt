package com.example.team4uu.data.audio

// 아이가 말을 끝냈는지 앱이 판단한다.
//
// 🔴 **서버(Gemini Live)의 자동 감지를 쓰지 않는 이유가 있다.** 실측에서
//    audio_stream_end 는 응답이 아예 오지 않았고(35초), 자동 VAD 는 응답 완료가
//    18.7초로 느렸다. 수동 신호(activity_start/end)가 TTFB 0.77초를 내는 유일한
//    조합이라, 판단만 앱으로 가져오고 서버 계약은 그대로 둔다.
//
// 판단 재료는 립싱크와 같은 RMS 다. 추가 라이브러리도 AI 호출도 없다.
//
// ⚠️ 임계값은 아직 실측하지 않았다. 실제 아이 목소리와 식사 소음(R6)에서
//    확인해야 한다 — 소음이 계속 임계값을 넘으면 자동 종료가 안 되고, 반대로
//    낮게 잡으면 아이가 잠깐 숨 쉴 때 말을 끊는다. 그래서 아래 두 안전장치를 뒀다.
class SpeechEndDetector(
    // 이 세기 미만이면 "조용함"으로 본다. 마이크 입력 기준이라 립싱크(재생 오디오)의
    // 임계값과는 다른 값이다.
    private val silenceLevel: Float = 0.02f,

    // 조용함이 이만큼 이어지면 말이 끝난 것으로 본다.
    // 짧게 잡으면 아이가 생각하느라 쉬는 사이에 끊긴다.
    private val silenceHoldMs: Int = 900,

    // 이 길이 이상 말해야 "발화"로 친다. 문 닫히는 소리 한 번에 턴이
    // 시작됐다 끝나버리는 것을 막는다.
    private val minSpeechMs: Int = 400,

    // 🔴 안전장치. 소음이 계속 임계값을 넘으면 자동 종료가 영영 안 걸린다.
    //    그러면 아이는 말을 끝냈는데 인형이 대답하지 않고, 크레딧만 나간다.
    private val maxUtteranceMs: Int = 15_000
) {

    private var elapsedMs = 0
    private var speechMs = 0
    private var silenceMs = 0

    fun reset() {
        elapsedMs = 0
        speechMs = 0
        silenceMs = 0
    }

    // 마이크 청크 하나가 나올 때마다 부른다. true 면 말이 끝난 것으로 보고
    // activity_end 를 보내면 된다.
    fun onChunk(rms: Float, chunkMs: Int = DEFAULT_CHUNK_MS): Boolean {
        elapsedMs += chunkMs

        if (rms >= silenceLevel) {
            speechMs += chunkMs
            silenceMs = 0
        } else {
            silenceMs += chunkMs
        }

        // 너무 길어지면 무조건 끊는다(위 안전장치).
        if (elapsedMs >= maxUtteranceMs) return true

        // 아직 말이라고 할 만큼 소리가 안 났으면 기다린다. 버튼을 누르자마자
        // 조용하다고 끝내버리면 아이가 입을 떼기도 전에 턴이 닫힌다.
        if (speechMs < minSpeechMs) return false

        return silenceMs >= silenceHoldMs
    }

    private companion object {
        // MicRecorder 가 100ms 씩 보낸다.
        const val DEFAULT_CHUNK_MS = 100
    }
}
