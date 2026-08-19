package com.example.team4uu.data

import android.content.Context

// 로그인해서 받은 JWT 를 보관하는 곳.
//
// 이 토큰 하나가 서버의 두 곳을 연다:
//   HTTP  Authorization: Bearer <토큰>   (stylize, friends, meals …)
//   WS    /doll/talk?token=<토큰>        (핸드셰이크에 헤더를 못 싣는 클라이언트 대응)
//
// 📌 ChildProfileStore 와 같은 패턴이다 — SharedPreferences 에 두고, 백엔드 저장소가
//    생기거나 암호화가 필요해지면 **이 파일만** 갈아끼운다.
//
// 🔐 토큰은 그 자체가 신분증이다(24시간 유효). 로그에 찍지 않는다.
class TokenStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // 앱을 다시 켰을 때 디스크의 값을 캐시로 올린다. 이게 있어야 첫 네트워크
        // 호출부터 헤더가 붙는다(아래 current() 설명 참조).
        cached = prefs.getString(KEY_TOKEN, null)
    }

    fun save(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
        cached = token
    }

    // 로그인한 적이 없으면 null. 이 경우 서버 호출은 401 로 떨어진다.
    fun load(): String? = cached ?: prefs.getString(KEY_TOKEN, null)?.also { cached = it }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
        cached = null
    }

    companion object {
        private const val PREFS_NAME = "auth"
        private const val KEY_TOKEN = "jwt"

        // 🔴 NetworkModule 의 인터셉터가 읽는 창구.
        //
        // 인터셉터는 OkHttp 스레드에서 돌고 **Context 를 못 받는다**(NetworkModule 은
        // Context 없는 object 싱글턴이다). 그래서 값을 프로세스 메모리에 한 벌 둔다.
        //
        // ⚠️ 화면 어딘가에서 TokenStore 인스턴스가 한 번은 만들어져야 이 캐시가 찬다
        //    (MainScreen 이 만든다). 안 만들어지면 헤더가 안 붙어 서버가 401 을 준다 —
        //    지금과 같은 상태로 돌아갈 뿐이라 조용히 잘못된 값을 보내지는 않는다.
        @Volatile
        private var cached: String? = null

        fun current(): String? = cached
    }
}
