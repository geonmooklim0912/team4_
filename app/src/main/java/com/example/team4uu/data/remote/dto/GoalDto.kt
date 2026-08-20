package com.example.team4uu.data.remote.dto

// 밥 먹기 시작 시 고른 오늘의 목표(최대 3개)를 서버에 기록 (★ 필드명은 백엔드 명세에 맞춰 조정)
data class GoalTagsRequest(
    val content: List<String>
)
