package com.example.team4uu.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit

object RetrofitClient{
//    private const val BASE_URL = "http://172.30.86.228:8000/"
    private const val BASE_URL = "http://1-201-116-93.sslip.io/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY //요청 및 응답을 Logcat에 찍음(디버깅용)
    }

    // 로그인 후 저장된 토큰이 있으면 모든 요청에 Authorization 헤더를 자동으로 붙여서 신원확인
    private val authInterceptor = okhttp3.Interceptor { chain ->
        val token = TokenManager.token
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        // 기본 10초 타임아웃으로는 /doll/stylize처럼 사진을 업로드하고 서버가 변환하는 데
        // 시간이 걸리는 요청이 중간에 끊길 수 있어서 넉넉하게 늘림.
        //
        // 🔴 readTimeout 은 60초로는 모자란다. 인형 등록은 정상적으로도 약 28초 걸리고
        //    (스프라이트 5장), Gemini 가 빈 응답을 주면(실측 6회 중 1회) 서버가 3회까지
        //    재시도한다. 서버 예산이 180초라 60초로 두면 **앱이 먼저 끊어서** 사용자는
        //    1분을 기다린 뒤 실패만 본다.
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create()

    // 서버가 sprite 경로를 절대 URL이 아니라 "/media/..." 같은 상대 경로로 줄 수도 있어서,
    // 이미 http(s)로 시작하면 그대로 쓰고 아니면 BASE_URL을 붙여서 완전한 URL로 만들어줌.
    fun resolveAssetUrl(path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
        }
}