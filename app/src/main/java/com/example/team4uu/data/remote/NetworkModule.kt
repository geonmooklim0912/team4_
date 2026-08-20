package com.example.team4uu.data.remote

import com.example.team4uu.BuildConfig
import com.example.team4uu.data.remote.dto.ApiError
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

// 서버 통신에 필요한 것들을 한 곳에 모아둔 곳.
// DI 라이브러리를 안 쓰는 프로젝트라 object 싱글턴으로 둔다.
object NetworkModule {

    private val moshi: Moshi = Moshi.Builder()
        // 코드젠(moshi-kotlin-codegen) 대신 리플렉션 어댑터를 쓴다.
        // 프로젝트에 이미 들어있는 moshi-kotlin 만으로 동작하므로 의존성을 늘리지 않는다.
        .add(KotlinJsonAdapterFactory())
        .build()

    // 📌 Authorization 헤더는 여기서 붙이지 않는다. 로그인 토큰은 TokenManager 가 들고
    //    있고, 헤더를 붙이는 인터셉터는 RetrofitClient 에 있다. 여기에도 두면 인증
    //    경로가 둘이 되어, 토큰이 어느 쪽 통에 들어갔는지에 따라 되기도 안 되기도 한다.

    // 🔴 타임아웃이 이 파일에서 가장 중요하다.
    //    변환은 정상적으로도 약 28초 걸린다(스프라이트 5장). OkHttp 기본 readTimeout 은
    //    10초라 그대로 두면 100% 실패한다. 서버 예산은 180초다.
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                // BODY 로 두면 스프라이트 PNG 바이트가 통째로 로그에 찍힌다.
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            // baseUrl 은 반드시 "/" 로 끝나야 한다. 아니면 Retrofit 이 예외를 던진다.
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val stylizeApi: StylizeApi by lazy { retrofit.create(StylizeApi::class.java) }

    // 인증·계정 API. stylize 와 **같은 Retrofit·같은 클라이언트**를 쓴다 —
    // 인터셉터와 커넥션 풀을 공유해야 로그인 직후 요청부터 헤더가 붙는다.
    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }

    private val errorAdapter by lazy { moshi.adapter(ApiError::class.java) }

    // Retrofit 은 2xx 가 아니면 HttpException 을 던지는데, 거기엔 code 가 안 들어 있다.
    // 본문을 직접 읽어서 {code, message} 를 꺼낸다.
    //
    // ⚠️ errorBody 는 한 번만 읽을 수 있고, 실패해도 예외를 밖으로 내면 안 된다 —
    //    원래 실패 원인이 파싱 오류로 덮여서 사라진다.
    fun parseError(e: HttpException): StylizeException {
        val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val parsed = raw?.let { body -> runCatching { errorAdapter.fromJson(body) }.getOrNull() }
        return StylizeException.of(parsed?.code, parsed?.message, e)
    }
}