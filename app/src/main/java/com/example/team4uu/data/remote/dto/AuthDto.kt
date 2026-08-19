package com.example.team4uu.data.remote.dto

import com.squareup.moshi.Json

// 로그인: 보낼 것 / 받을 것
data class LoginRequest(val username: String, val password: String)

// ⚠️ 서버는 snake_case 로 준다: {"token": "...", "user_id": 1}
//    Moshi 리플렉션 어댑터는 프로퍼티 이름 그대로 찾으므로 @Json 이 없으면
//    "Required value 'userId' missing" 으로 **로그인이 통째로 실패**한다.
//
// 앱은 user_id 를 쓸 데가 없다(서버가 토큰의 sub 로 사용자를 판별한다). 그래서
// nullable + 기본값으로 둔다 — 서버가 이 필드를 빼도 로그인이 죽지 않는다.
data class LoginResponse(
    val token: String,
    @Json(name = "user_id") val userId: Long? = null
)

// 회원가입: 비번확인은 앱에서만 검사 → 여기엔 password 하나
//
// 🔴 미완성이다. 서버(POST /api/auth/signup)는 name / age / keyword 까지
//    **6개 전부 필수**라 이대로 보내면 400 VALIDATION_ERROR 가 난다.
//    - name/age 가 아이 것인지 부모 것인지 합의가 안 됐다(가입 폼 라벨은 "아이 이름")
//    - keyword(관심사)는 앱에 입력칸 자체가 없다. 서버가 미리 정해둔 10개 중 하나다
//    둘 다 정해지면 필드를 추가한다. 지금은 로그인 경로만 동작한다.
data class SignUpRequest(val username: String, val password: String, val email: String)

data class SignUpResponse(
    val token: String,
    @Json(name = "user_id") val userId: Long? = null
)