package com.example.team4uu.data.remote

import com.example.team4uu.data.remote.dto.GoalTagsRequest

// 밥 먹기 시작 시 고른 오늘의 목표(최대 3개)를 서버에 기록. NetworkModule의 같은 Retrofit·인터셉터를 공유함.
class GoalRepository {
    suspend fun postGoalTags(goals: List<String>) {
        val response = NetworkModule.apiService.postGoalTags(GoalTagsRequest(goals))
        if (!response.isSuccessful) {
            throw Exception("목표 전송에 실패했습니다.")
        }
    }
}
