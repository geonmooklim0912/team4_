package com.example.team4uu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.team4uu.data.AppDatabase
import com.example.team4uu.data.Friend
import com.example.team4uu.data.FriendRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    init {
        // TODO(임시 테스트 데이터): HomeOptionsPanel의 "친구 모아보기"(책 아이콘) 3x3 칸(총 9칸) 잠금 UI를
        // 실제 앱 화면에서 바로 확인할 수 있도록, DB가 비어 있을 때만 한 번 더미 친구 9명을 채워 넣음.
        // 3번째 칸부터는 미션 스테이지 달성 전까지 화면에서 잠겨 보이므로(HomeOptionsPanel 참고),
        // 여기 데이터가 있어도 실제로는 2번째 칸까지만 곧바로 보임. 지워도 되는 코드임.
        viewModelScope.launch {
            if (repository.friends.first().isEmpty()) {
                seedSampleFriends()
            }
        }
    }

    fun addFriend(name: String, imagePath: String) {
        viewModelScope.launch {
            repository.addFriend(name, imagePath)
        }
    }

    // TODO(테스트용): EmptyFriendScreen(친구 없음 온보딩)을 실제 앱에서 바로 확인할 수 있도록
    // 로그인 화면의 test2 계정 전용으로 씀(MainScreen.kt 참고). 실제 계정별 데이터 분리가 생기면 제거.
    fun clearAllFriends() {
        viewModelScope.launch {
            repository.deleteAllFriends()
        }
    }

    private suspend fun seedSampleFriends() {
        repeat(9) { index ->
            repository.addFriend(name = "친구${index + 1}", imagePath = "")
        }
    }
}