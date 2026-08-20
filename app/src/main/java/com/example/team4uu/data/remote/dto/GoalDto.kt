package com.example.team4uu.data.remote.dto

// 밥 먹기 시작 시 고른 오늘의 목표(최대 3개)를 하나의 문자열로 합쳐 서버에 기록
data class GoalTagsRequest(
    val context: String
)
