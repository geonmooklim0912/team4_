package com.example.team4uu.data.remote.dto

// 📌 POST /doll/stylize 의 응답(StylizeResponse)은 DollDto.kt 에 있다.
//    여기에도 있었는데 같은 이름이 두 번 선언돼 빌드가 깨져서 그쪽으로 합쳤다.

// 서버의 모든 실패는 이 형식으로 온다. HTTP 상태코드가 아니라 code 문자열을 보고 분기한다.
// 필드명을 틀리게 보내도 FastAPI 기본 422가 아니라 이 형식으로 변환해서 준다.
data class ApiError(
    val code: String,
    val message: String
)