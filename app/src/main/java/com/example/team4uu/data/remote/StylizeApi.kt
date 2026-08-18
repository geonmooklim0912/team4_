package com.example.team4uu.data.remote

import com.example.team4uu.data.remote.dto.StylizeResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// AI 서버의 인형 변환 API.
//
// ⚠️ 파트 이름은 반드시 "image" 여야 한다. 서버가 그 이름으로만 파일을 찾고,
//    틀리면 400 INVALID_IMAGE("사진이 첨부되지 않았어요")가 돌아온다.
interface StylizeApi {

    // 사진 1장 -> 스프라이트 5장. 정상 소요 약 28초.
    // 호출하는 쪽은 재시도하지 말 것 — 서버가 이미 내부에서 3회 재시도한다
    // (Gemini 가 6회 중 1회꼴로 빈 응답을 준다). 중복 재시도하면 대기가 2분을 넘는다.
    @Multipart
    @POST("doll/stylize")
    suspend fun stylize(@Part image: MultipartBody.Part): StylizeResponse
}