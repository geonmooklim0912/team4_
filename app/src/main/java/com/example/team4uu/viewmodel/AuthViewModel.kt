package com.example.team4uu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team4uu.data.remote.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()

    fun login(
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.login(username, password)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "로그인 실패")
            }
        }
    }

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
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "관심사 변경 실패")
            }
        }
    }
}