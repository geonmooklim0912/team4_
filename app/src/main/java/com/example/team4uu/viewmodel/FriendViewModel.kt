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
import com.example.team4uu.data.remote.StylizeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FriendViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FriendRepository(AppDatabase.getInstance(application).friendDao())
    private val registration = DollRegistrationRepository(
        friendRepository = repository,
        spriteStorage = SpriteStorage(application)
    )

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

    // --- 인형 등록 (촬영 -> AI 서버 -> 스프라이트 저장) --------------------------

    sealed interface RegistrationState {
        data object Idle : RegistrationState

        // 서버가 변환하는 동안. 정상적으로도 약 28초 걸리므로 화면에 모달을 띄워야 한다.
        data object InProgress : RegistrationState

        data class Failed(val error: StylizeException) : RegistrationState
    }

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    // "다시 시도"를 누르면 같은 사진으로 한 번 더 보내야 하므로 마지막 요청을 기억해 둔다.
    private var lastRequest: Pair<String, String>? = null

    fun registerFriend(name: String, photoPath: String) {
        // 진행 중에 또 누르면 서버 호출이 두 번 나가고 비용도 두 배로 든다(등록 1회 325원).
        if (_registrationState.value is RegistrationState.InProgress) return

        lastRequest = name to photoPath
        _registrationState.value = RegistrationState.InProgress

        viewModelScope.launch {
            try {
                val id = registration.register(name, photoPath)
                Log.i(TAG, "친구 등록 완료 id=$id")
                _registrationState.value = RegistrationState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: StylizeException) {
                Log.w(TAG, "등록 실패 ${e.code}", e)
                _registrationState.value = RegistrationState.Failed(e)
            } catch (e: Exception) {
                Log.e(TAG, "등록 중 예상 못한 오류", e)
                _registrationState.value =
                    RegistrationState.Failed(StylizeException.of(null, cause = e))
            }
        }
    }

    fun retryRegistration() {
        lastRequest?.let { (name, path) ->
            _registrationState.value = RegistrationState.Idle
            registerFriend(name, path)
        }
    }

    fun dismissRegistrationError() {
        _registrationState.value = RegistrationState.Idle
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

    private companion object {
        const val TAG = "FriendViewModel"
    }
}