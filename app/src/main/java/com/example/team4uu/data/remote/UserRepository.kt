package com.example.team4uu.data.remote

import com.example.team4uu.data.remote.dto.SignUpRequest
import com.example.team4uu.data.remote.dto.SignUpResponse
import com.example.team4uu.data.remote.dto.UpdateKeywordRequest

// 로그인/토큰(AuthRepository, TokenStore)과는 별개로, 회원가입·관심사 변경처럼
// 사용자 프로필에 관련된 API. NetworkModule의 같은 Retrofit·인터셉터를 공유함.
class UserRepository {
    suspend fun signUp(
        username: String,
        password: String,
        email: String,
        name: String,
        age: Int,
        keyword: String
    ): SignUpResponse = NetworkModule.apiService.signUp(
        SignUpRequest(username, password, email, name, age, keyword)
    )

    suspend fun updateKeyword(keywords: List<String>) {
        val response = NetworkModule.apiService.updateKeyword(UpdateKeywordRequest(keywords))
        if (!response.isSuccessful) {
            throw Exception("관심사 변경에 실패했습니다.")
        }
    }
}
