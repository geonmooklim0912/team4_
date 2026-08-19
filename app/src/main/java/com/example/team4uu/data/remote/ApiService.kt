package com.example.team4uu.data.remote

import com.example.team4uu.data.remote.dto.LoginRequest
import com.example.team4uu.data.remote.dto.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

// 인증·계정 API.
//
// ⚠️ 경로 앞에 "/" 를 붙이지 않는다. baseUrl 뒤에 이어붙는 상대경로이고,
//    "/api/..." 로 쓰면 baseUrl 의 경로가 통째로 무시된다(지금은 결과가 같지만
//    베이스에 경로가 생기는 순간 조용히 깨진다).
interface ApiService {

    // 아이디·비밀번호 -> JWT. 서버는 토큰을 저장하지 않는다(무상태) —
    // 이후 요청은 이 토큰의 서명과 만료(24시간)만 검증받는다.
    //
    // ⚠️ 요청 필드는 account 가 아니라 **username** 이다. 서버 저장소 최신 코드는
    //    account 로 되어 있지만 **배포된 서버는 username** 을 받는다(실측 확인).
    //
    // 실패는 401 INVALID_CREDENTIALS 하나로 온다. "아이디 없음"과 "비밀번호 틀림"을
    // 구분해서 주지 않는 것은 의도된 것이다(계정 존재 여부 열거 방지).
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse
}