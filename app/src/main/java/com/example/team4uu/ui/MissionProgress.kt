package com.example.team4uu.ui

// 미션 로드맵 한 단계의 진행 상황. starsEarned는 0~STARS_TO_UNLOCK_LEVEL 사이 값이고,
// 거기 도달해야 다음 단계 잠금이 풀림. MissionViewModel이 실제 값을 들고 있고,
// MissionRoadmapScreen이 그걸 그대로 그려줌.
data class MissionLevelProgress(val level: Int, val starsEarned: Int)

const val TOTAL_MISSION_LEVELS = 8
const val STARS_TO_UNLOCK_LEVEL = 3
