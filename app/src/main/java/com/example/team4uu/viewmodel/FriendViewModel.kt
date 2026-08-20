package com.example.team4uu.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.team4uu.data.AppDatabase
import com.example.team4uu.data.DollRegistrationRepository
import com.example.team4uu.data.Friend
import com.example.team4uu.data.FriendRepository
import com.example.team4uu.data.SpriteStorage
import com.example.team4uu.data.remote.DollRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class FriendViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FriendRepository(AppDatabase.getInstance(application).friendDao())
    private val dollRepository = DollRepository()

    // 대화 중 입 모양 교체(립싱크)에 쓸 스프라이트를 기기에 보관한다.
    private val spriteStorage = SpriteStorage(application)

    // 현재 로그인한 계정의 아이디. 계정마다 친구 목록을 분리해서 보여주기 위한 기준 키로,
    // 로그인할 때 setCurrentUser로 설정됨.
    private val ownerUsername = MutableStateFlow<String?>(null)

    // DB에 저장된 친구 목록을 실시간으로 반영하는 상태. ownerUsername이 바뀌면(계정 전환)
    // 그 계정의 친구 목록으로 자동으로 다시 조회됨.
    // 화면이 안 보이는 동안 구독을 5초 유예 후 해제(WhileSubscribed)해서 화면 전환 시 불필요한 재조회를 줄임.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val friends: StateFlow<List<Friend>> = ownerUsername
        .flatMapLatest { owner ->
            if (owner == null) flowOf(emptyList()) else repository.friendsForOwner(owner)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // 로그인 성공 시 호출.
    fun setCurrentUser(username: String) {
        ownerUsername.value = username
    }

    // 로그아웃 시 호출. 현재 계정을 비워서 friends가 다시 빈 목록이 되게 함.
    fun logout() {
        ownerUsername.value = null
    }

    // 카메라 촬영 후 서버로 사진을 보내 인형 스타일로 변환하는 동안 true. 로딩 UI에 씀.
    private val _isCreatingFriend = MutableStateFlow(false)
    val isCreatingFriend: StateFlow<Boolean> = _isCreatingFriend

    // 촬영한 사진(imagePath)을 POST /doll/stylize로 업로드해서 인형 캐릭터로 변환한 뒤,
    // 그 결과와 함께 새 친구로 저장함.
    fun createFriendFromPhoto(name: String, imagePath: String, onError: (String) -> Unit) {
        val owner = ownerUsername.value ?: return
        viewModelScope.launch {
            _isCreatingFriend.value = true
            try {
                val result = dollRepository.stylizeDoll(File(imagePath))
                // sprites의 첫 번째 항목이 홈 화면에 띄울 대표 이미지
                val friendId = repository.addFriend(
                    ownerUsername = owner,
                    name = name,
                    imagePath = imagePath,
                    characterAssetPath = result.sprites.firstOrNull()
                )

                // 립싱크는 입 모양 3장을 초당 30번까지 교체한다. 네트워크로는 못 따라가서
                // 등록할 때 한 번 내려받아 둔다(TalkingCharacterPainter 가 이 파일을 읽는다).
                // 실패해도 등록 자체는 성공으로 둔다 — 립싱크만 빠지고 대화는 그대로 된다.
                runCatching {
                    val temp = spriteStorage.downloadToTemp(result.sprite_map)
                    spriteStorage.commit(temp, friendId)
                }.onFailure { Log.w(TAG, "스프라이트 저장 실패 — 립싱크 없이 진행", it) }
            } catch (e: Exception) {
                onError(e.message ?: "인형 만들기에 실패했습니다.")
            } finally {
                _isCreatingFriend.value = false
            }
        }
    }

    // 설정 > 친구 목록 관리에서 이름 변경/삭제. 친구는 서버에 등록되지 않고 로컬(Room)에만
    // 있어서 네트워크 요청 없이 바로 반영된다 — friends가 Room을 구독하는 StateFlow라
    // 화면도 자동으로 갱신됨.
    fun renameFriend(friend: Friend, newName: String) {
        viewModelScope.launch {
            repository.updateFriend(friend.copy(name = newName))
        }
    }

    fun deleteFriend(friend: Friend) {
        viewModelScope.launch {
            repository.deleteFriend(friend)
        }
    }

    private companion object {
        const val TAG = "FriendViewModel"
    }
}