package com.example.team4uu.data.remote.dto

import com.squareup.moshi.Json

// 로그인: 보낼 것 / 받을 것
data class LoginRequest(
    val username: String,
    val password: String
)
data class LoginResponse(
    val token: String? = null,
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