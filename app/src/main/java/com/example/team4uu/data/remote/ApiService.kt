package com.example.team4uu.data.remote

import com.example.team4uu.data.remote.dto.GoalTagsRequest
import com.example.team4uu.data.remote.dto.LoginRequest
import com.example.team4uu.data.remote.dto.LoginResponse
import com.example.team4uu.data.remote.dto.SignUpRequest
import com.example.team4uu.data.remote.dto.SignUpResponse
import com.example.team4uu.data.remote.dto.UpdateKeywordRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.Response

interface ApiService {
    // 2xx가 아니면 HttpException을 던짐(NetworkModule.parseError로 code를 꺼내 씀) — AuthRepository가 이 방식을 기대함
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): SignUpResponse

    @PATCH("api/users/me/keyword")
    suspend fun updateKeyword(@Body request: UpdateKeywordRequest): Response<ResponseBody>

    // 밥 먹기 시작 시 고른 오늘의 목표(최대 3개)를 서버에 기록
    @POST("api/goal-tags")
    suspend fun postGoalTags(@Body request: GoalTagsRequest): Response<ResponseBody>
}
