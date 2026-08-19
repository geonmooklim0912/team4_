package com.example.team4uu.data.remote

import com.example.team4uu.data.remote.dto.LoginRequest
import com.example.team4uu.data.remote.dto.LoginResponse
import com.example.team4uu.data.remote.dto.SignUpRequest
import com.example.team4uu.data.remote.dto.SignUpResponse
import com.example.team4uu.data.remote.dto.UpdateKeywordRequest

class AuthRepository {
    suspend fun login(username: String, password: String): LoginResponse {
        val response = RetrofitClient.api.login(LoginRequest(username, password))
        val body = response.body()
        if (!response.isSuccessful || body?.token == null) {
            throw Exception("아이디 또는 비밀번호가 일치하지 않습니다.")
        }
        TokenManager.save(body.token, body.userId)
        return body
    }

    suspend fun signUp(
        username: String,
        password: String,
        email: String,
        name: String,
        age: Int,
        keyword: String
    ): SignUpResponse {
        val response = RetrofitClient.api.signUp(
            SignUpRequest(username, password, email, name, age, keyword)
        )
        TokenManager.keywords = keyword.split(",").filter { it.isNotBlank() }
        return response
    }

    suspend fun updateKeyword(keywords: List<String>) {
        val response = RetrofitClient.api.updateKeyword(UpdateKeywordRequest(keywords))
        if (!response.isSuccessful) {
            throw Exception("관심사 변경에 실패했습니다.")
        }
        TokenManager.keywords = keywords
    }
}