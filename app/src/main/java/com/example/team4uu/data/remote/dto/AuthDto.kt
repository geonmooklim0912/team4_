package com.example.team4uu.data.remote.dto


// 로그인: 보낼 것 / 받을 것
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val userId: Long)

// 회원가입: 비번확인은 앱에서만 검사 → 여기엔 password 하나
data class SignUpRequest(val username: String, val password: String, val email: String)
data class SignUpResponse(val token: String, val userId: Long)