package com.example.team4uu.data.remote

import com.example.team4uu.data.remote.dto.StylizeResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class DollRepository {
    // 촬영한 사진 파일 하나를 멀티파트로 "image" 필드에 담아 POST /doll/stylize로 전송
    suspend fun stylizeDoll(imageFile: File): StylizeResponse {
        val requestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestBody)

        val response = RetrofitClient.api.stylizeDollImage(imagePart)
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            throw Exception("사진을 인형으로 변환하는 데 실패했습니다.")
        }
        return body
    }
}
