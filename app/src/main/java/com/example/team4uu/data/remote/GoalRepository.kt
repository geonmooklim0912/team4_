package com.example.team4uu.data.remote

import com.example.team4uu.data.remote.dto.GoalTagsRequest

class GoalRepository {
    // 밥 먹기 시작 시 고른 목표(최대 3개)를 하나의 문자열로 합쳐 서버에 기록
    suspend fun postGoalTags(goals: List<String>) {
        val response = RetrofitClient.api.postGoalTags(GoalTagsRequest(content = goals.joinToString(", ")))
        if (!response.isSuccessful) {
            throw Exception("목표 전송에 실패했습니다.")
        }
    }
}
