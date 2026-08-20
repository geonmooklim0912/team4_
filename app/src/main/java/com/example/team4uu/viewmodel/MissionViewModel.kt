package com.example.team4uu.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.team4uu.data.remote.GoalRepository
import com.example.team4uu.ui.MissionLevelProgress
import com.example.team4uu.ui.STARS_TO_UNLOCK_LEVEL
import com.example.team4uu.ui.TOTAL_MISSION_LEVELS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 밥 먹기 목표 선택(최대 3개)부터, 그날 목표 달성 결과를 미션 로드맵 별 진행도로 반영하는 것까지 담당.
const val MAX_GOALS_PER_SESSION = 3

// 3개 중 이만큼 달성해야 별을 하나 얻음.
private const val GOALS_NEEDED_TO_EARN_STAR = 2

class MissionViewModel(application: Application) : AndroidViewModel(application) {
    private val goalRepository = GoalRepository()

    private val _levels = MutableStateFlow(
        (1..TOTAL_MISSION_LEVELS).map { level -> MissionLevelProgress(level = level, starsEarned = 0) }
    )
    val levels: StateFlow<List<MissionLevelProgress>> = _levels

    // 별 3개를 다 채운 단계 수. HomeScreen/HomeOptionsPanel이 배경·친구 칸 잠금 해제 여부를 판단할 때 씀.
    val currentStage: StateFlow<Int> = levels
        .map { list -> list.count { it.starsEarned >= STARS_TO_UNLOCK_LEVEL } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // 밥 먹기 시작 시 고른(최대 3개) 오늘의 목표. "끝내기"를 누르면 그대로 다시 보여줌.
    private val _currentGoals = MutableStateFlow<List<String>>(emptyList())
    val currentGoals: StateFlow<List<String>> = _currentGoals

    // 목표 선택 팝업에서 확인을 누르면 호출됨. 이번 밥먹기 세션 목표로 저장하고, 서버에도 기록함.
    fun selectGoals(goals: List<String>) {
        _currentGoals.value = goals
        if (goals.isEmpty()) return
        viewModelScope.launch {
            try {
                goalRepository.postGoalTags(goals)
            } catch (e: Exception) {
                // 목표 기록은 서버가 참고만 하는 값이라, 실패해도 밥 먹기 흐름 자체는 막지 않음.
                Log.w(TAG, "목표 전송 실패", e)
            }
        }
    }

    // "끝내기"에서 달성한 목표를 체크하고 확인을 누르면 호출됨. 3개 중 2개 이상 달성했으면
    // 미션 로드맵에서 아직 다 못 채운 첫 단계의 왼쪽 별부터 하나 채움.
    fun completeFeedingSession(achievedGoals: Set<String>) {
        if (achievedGoals.size >= GOALS_NEEDED_TO_EARN_STAR) {
            addStar()
        }
        _currentGoals.value = emptyList()
    }

    private fun addStar() {
        _levels.update { current ->
            val idx = current.indexOfFirst { it.starsEarned < STARS_TO_UNLOCK_LEVEL }
            if (idx == -1) return@update current
            current.toMutableList().apply {
                this[idx] = this[idx].copy(starsEarned = this[idx].starsEarned + 1)
            }
        }
    }

    private companion object {
        const val TAG = "MissionViewModel"
    }
}
