package com.example.team4uu.data.remote

// 로그인 후 발급받은 토큰을 앱 실행 중에 들고 있다가, 다른 API 요청 보낼 때
// RetrofitClient의 인터셉터가 Authorization 헤더에 실어 보냄(AuthInterceptor 참고).
object TokenManager {
    var token: String? = null
    var userId: Long? = null

    // 최근에 서버로 보낸 관심사 목록(회원가입 또는 관심사 변경 시 갱신). 관심사 변경 팝업을
    // 열 때 서버에서 다시 조회하지 않고 이 값으로 기존 선택 상태를 미리 보여주는 용도.
    var keywords: List<String> = emptyList()

    fun save(token: String, userId: Long?) {
        this.token = token
        this.userId = userId
    }

    fun clear() {
        token = null
        userId = null
        keywords = emptyList()
    }
}
