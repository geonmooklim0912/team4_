package com.example.team4uu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.team4uu.data.AppDatabase
import com.example.team4uu.data.Friend
import com.example.team4uu.data.FriendRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FriendViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FriendRepository(AppDatabase.getInstance(application).friendDao())

    // DB에 저장된 친구 목록을 실시간으로 반영하는 상태.
    // 화면이 안 보이는 동안 구독을 5초 유예 후 해제(WhileSubscribed)해서 화면 전환 시 불필요한 재조회를 줄임.
    val friends: StateFlow<List<Friend>> = repository.friends.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun addFriend(name: String, imagePath: String) {
        viewModelScope.launch {
            repository.addFriend(name, imagePath)
        }
    }
}