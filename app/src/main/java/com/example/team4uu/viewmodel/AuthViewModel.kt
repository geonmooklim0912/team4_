package com.example.team4uu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team4uu.data.remote.UserRepository
import kotlinx.coroutines.launch

// 로그인 자체는 data.AuthRepository/TokenStore가 맡고(MainScreen 참고), 이 ViewModel은
// 그 외 사용자 프로필 관련 요청(회원가입, 관심사 변경)만 다룸.
class AuthViewModel : ViewModel() {
    private val repo = UserRepository()

    // 관심사 변경 팝업을 다시 열 때 기존 선택을 미리 보여주기 위해, 마지막으로
    // 서버에 반영된 관심사 목록을 기억해둠(회원가입 성공 시 / 변경 성공 시 갱신).
    var keywords: List<String> = emptyList()
        private set

    fun signUp(
        username: String,
        password: String,
        email: String,
        name: String,
        age: Int,
        keyword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.signUp(username, password, email, name, age, keyword)
                keywords = keyword.split(",").filter { it.isNotBlank() }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "회원가입 실패")
            }
        }
    }

    fun updateKeyword(
        keywords: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.updateKeyword(keywords)
                this@AuthViewModel.keywords = keywords
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "관심사 변경 실패")
            }
        }
    }
}
