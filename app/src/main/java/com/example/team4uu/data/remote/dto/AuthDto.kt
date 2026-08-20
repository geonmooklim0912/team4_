package com.example.team4uu.data.remote.dto

import com.squareup.moshi.Json

// 로그인: 보낼 것 / 받을 것
data class LoginRequest(
    val username: String,
    val password: String
)
data class LoginResponse(
    // login()이 실패하면 HttpException으로 떨어지고 이 객체 자체가 안 만들어지므로,
    // 여기까지 왔다는 건 서버가 토큰을 준 것 — non-null로 둬서 TokenStore.save()에 바로 넘길 수 있음.
    val token: String,
    @Json(name = "user_id") val userId: Long? = null
)


// ── 회원가입 ──
// 보낼 것: 아이디 + 비번 + 이메일 (비번확인은 앱에서만 검사)

data class SignUpRequest(
    val username: String,
    val password: String,
    val email: String,
    val name: String,
    val age: Int,
    val keyword: String
)
// 받을 것 (★ 백엔드 명세에 맞춰 조정)
data class SignUpResponse(
    val response: String
)