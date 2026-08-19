package com.example.team4uu.data

import android.util.Log
import com.example.team4uu.data.remote.NetworkModule
import com.example.team4uu.data.remote.dto.LoginRequest
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

// 로그인 한 번으로 토큰을 받아 저장하는 것까지.
//
// 서버는 토큰을 저장하지 않는다(무상태). 서명과 만료(24시간)만 검증하므로
// 서버를 재배포해도 받아둔 토큰은 계속 유효하다.
class AuthRepository(private val tokenStore: TokenStore) {

    suspend fun login(username: String, password: String) {
        val response = try {
            NetworkModule.apiService.login(LoginRequest(username, password))
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            // parseError 는 이름만 stylize 일 뿐 {code, message} 를 꺼내는 공통 처리다.
            // 여기서는 문구를 버리고 code 만 쓴다 — 로그인 실패 문구는 이 파일이 정한다.
            throw AuthException.of(NetworkModule.parseError(e).code, e)
        } catch (e: IOException) {
            throw AuthException.of(AuthException.NETWORK, e)
        }

        tokenStore.save(response.token)
        // 🔐 토큰도 아이디도 남기지 않는다. 성공 여부만 알면 된다.
        Log.i(TAG, "로그인 성공 — 토큰 저장됨")
    }

    fun logout() {
        tokenStore.clear()
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}

// 로그인 실패를 사용자에게 보여줄 수 있는 형태로 바꾼 것.
class AuthException(
    val code: String,
    val userMessage: String,
    cause: Throwable? = null
) : Exception("$code: $userMessage", cause) {

    companion object {
        const val NETWORK = "NETWORK"

        fun of(code: String?, cause: Throwable? = null) = when (code) {
            // 서버는 "아이디 없음"과 "비밀번호 틀림"을 구분해서 주지 않는다.
            // 구분해 주면 존재하는 아이디를 대입으로 알아낼 수 있기 때문이다(계정 열거).
            // 그러니 앱도 하나의 문구로 보여주는 게 맞다.
            "INVALID_CREDENTIALS" -> AuthException(
                code, "아이디 또는 비밀번호가 일치하지 않습니다.", cause
            )

            NETWORK -> AuthException(
                code, "인터넷 연결을 확인해 주세요.", cause
            )

            else -> AuthException(
                code ?: "UNKNOWN",
                "지금은 로그인할 수 없어요.\n잠시 후 다시 시도해 주세요.",
                cause
            )
        }
    }
}
