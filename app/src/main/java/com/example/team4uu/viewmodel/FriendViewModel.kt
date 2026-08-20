package com.example.team4uu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.team4uu.data.AppDatabase
import com.example.team4uu.data.ConvertedDoll
import com.example.team4uu.data.DollRegistrationRepository
import com.example.team4uu.data.Friend
import com.example.team4uu.data.FriendRepository
import com.example.team4uu.data.SpriteStorage
import com.example.team4uu.data.remote.StylizeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

// 촬영 -> AI 서버 변환 -> (이름 입력) -> 스프라이트 저장까지의 진행 상태. MainScreen이 이 값에 따라
// DollLoadingDialog/DollNameDialog/DollErrorDialog를 띄움.
sealed interface DollRegistrationState {
    data object Idle : DollRegistrationState
    data object Loading : DollRegistrationState
    // 변환은 끝났고 스프라이트도 임시 폴더에 받아둔 상태. 이름을 입력받아 confirmFriendName으로 넘기면 저장됨.
    data class NamePending(val converted: ConvertedDoll) : DollRegistrationState
    // photoPath를 들고 있어야 "다시 시도"를 같은 사진으로 재요청할 수 있음.
    data class Error(val exception: StylizeException, val photoPath: String) : DollRegistrationState
}

class FriendViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FriendRepository(AppDatabase.getInstance(application).friendDao())
    private val spriteStorage = SpriteStorage(application)
    private val dollRegistrationRepository = DollRegistrationRepository(
        friendRepository = repository,
        spriteStorage = spriteStorage
    )

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

    private val _registrationState = MutableStateFlow<DollRegistrationState>(DollRegistrationState.Idle)
    val registrationState: StateFlow<DollRegistrationState> = _registrationState

    // 촬영한 사진(photoPath)을 서버로 보내 인형 캐릭터로 변환함. 성공하면 이름을 물어보기 위해
    // NamePending 상태로 넘어감(아직 친구 목록에는 저장되지 않음). 진행 상황은 registrationState로 노출됨.
    fun registerFriendFromPhoto(photoPath: String) {
        viewModelScope.launch {
            _registrationState.value = DollRegistrationState.Loading
            try {
                val converted = dollRegistrationRepository.convert(photoPath)
                _registrationState.value = DollRegistrationState.NamePending(converted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: StylizeException) {
                _registrationState.value = DollRegistrationState.Error(e, photoPath)
            } catch (e: Exception) {
                _registrationState.value = DollRegistrationState.Error(StylizeException.of(null, cause = e), photoPath)
            }
        }
    }

    // 이름 입력 다이얼로그에서 확인을 누르면 호출됨. 그 이름으로 실제 친구 목록에 저장함.
    fun confirmFriendName(name: String) {
        val state = _registrationState.value
        if (state !is DollRegistrationState.NamePending) return
        val owner = ownerUsername.value ?: return
        viewModelScope.launch {
            _registrationState.value = DollRegistrationState.Loading
            try {
                dollRegistrationRepository.save(ownerUsername = owner, name = name, converted = state.converted)
                _registrationState.value = DollRegistrationState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: StylizeException) {
                _registrationState.value = DollRegistrationState.Error(e, state.converted.photoPath)
            } catch (e: Exception) {
                _registrationState.value = DollRegistrationState.Error(StylizeException.of(null, cause = e), state.converted.photoPath)
            }
        }
    }

    // 이름 입력 다이얼로그를 취소하면 호출됨. 받아둔 스프라이트 임시 파일을 정리함.
    fun cancelNamePending() {
        val state = _registrationState.value
        if (state is DollRegistrationState.NamePending) {
            dollRegistrationRepository.discard(state.converted)
        }
        _registrationState.value = DollRegistrationState.Idle
    }

    fun dismissRegistrationError() {
        _registrationState.value = DollRegistrationState.Idle
    }

    // 친구 관리 팝업의 "이름 바꾸기"에서 호출됨.
    fun renameFriend(friend: Friend, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == friend.name) return
        viewModelScope.launch {
            repository.updateFriend(friend.copy(name = trimmed))
        }
    }

    // 친구 관리 팝업의 "삭제"에서 확인을 누르면 호출됨. Room 에서 지우고, 그 친구가 쓰던
    // 스프라이트/원본 사진 파일도 함께 정리한다.
    fun deleteFriend(friend: Friend) {
        viewModelScope.launch {
            repository.deleteFriend(friend)
            spriteStorage.deleteSprites(friend.id)
            File(friend.imagePath).delete()
        }
    }
}
