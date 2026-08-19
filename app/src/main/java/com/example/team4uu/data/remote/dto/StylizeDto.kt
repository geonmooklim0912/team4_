package com.example.team4uu.data.remote.dto

import com.squareup.moshi.Json

// POST /doll/stylize 의 응답.
//
// 서버가 인형 사진 1장을 받아 립싱크·표정용 스프라이트 5장을 만들어 준다.
// 응답 형식은 AI 서버와의 연동 계약이라 필드 이름을 함부로 바꾸면 안 된다.
// (서버 쪽 정의: server/routes/stylize.py)
data class StylizeResponse(
    // 스프라이트 URL 목록. [0]은 항상 mouth_closed 라는 것만 보장된다.
    // 이름으로 골라야 할 때는 sprites 대신 spriteMap 을 쓸 것.
    val sprites: List<String>,

    // {"mouth_closed": "https://...", "mouth_half": ...} 형태.
    // 립싱크는 이름이 필요하므로 이쪽이 실제로 쓰는 필드다.
    @Json(name = "sprite_map") val spriteMap: Map<String, String>,

    // 만들지 못한 스프라이트 이름. 비어 있지 않아도 등록 자체는 성공이다
    // (서버가 부분 실패를 200으로 내보낸다 — 변형 한 장 때문에 28초를 날리지 않기 위해).
    val failed: List<String> = emptyList(),

    @Json(name = "elapsed_ms") val elapsedMs: Long = 0
)

// 서버의 모든 실패는 이 형식으로 온다. HTTP 상태코드가 아니라 code 문자열을 보고 분기한다.
// 필드명을 틀리게 보내도 FastAPI 기본 422가 아니라 이 형식으로 변환해서 준다.
data class ApiError(
    val code: String,
    val message: String
)