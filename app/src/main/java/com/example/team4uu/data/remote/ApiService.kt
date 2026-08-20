package com.example.team4uu.data.remote

import com.example.team4uu.data.remote.dto.GoalTagsRequest
import com.example.team4uu.data.remote.dto.LoginRequest
import com.example.team4uu.data.remote.dto.LoginResponse
import com.example.team4uu.data.remote.dto.SignUpRequest
import com.example.team4uu.data.remote.dto.SignUpResponse
import com.example.team4uu.data.remote.dto.StylizeResponse
import com.example.team4uu.data.remote.dto.UpdateKeywordRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.Response

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): SignUpResponse

    @PATCH("api/users/me/keyword")
    suspend fun updateKeyword(@Body request: UpdateKeywordRequest): Response<ResponseBody>

    // 밥 먹기 시작 시 고른 오늘의 목표(최대 3개)를 서버에 기록
    @POST("api/goal-tags")
    suspend fun postGoalTags(@Body request: GoalTagsRequest): Response<ResponseBody>

    // Authorization 헤더는 RetrofitClient의 인터셉터가 TokenManager에 저장된 토큰으로 자동으로 붙여줌
    @Multipart
    @POST("doll/stylize")
    suspend fun stylizeDollImage(
        @Part image: MultipartBody.Part // Request DTO 대신 실제 파일(Part)을 바로 넣음
    ): Response<StylizeResponse>
}