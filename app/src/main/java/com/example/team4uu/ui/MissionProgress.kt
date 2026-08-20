package com.example.team4uu.ui

// TODO: 실제로는 미션 로드맵(MissionRoadmapScreen)에서 도달한 최고 스테이지를 저장하는 공용 상태
// (ViewModel/DB 등)에서 받아와야 함. 아직 그 저장소가 없어서, 배경/친구 칸 잠금 해제 로직이
// 전부 이 값 하나를 함께 참조하도록 임시로 묶어둠(아무 것도 클리어하지 않은 상태인 0으로 고정).
const val CURRENT_MISSION_STAGE = 0

// 미션 로드맵 한 단계의 진행 상황. starsEarned는 0~STARS_TO_UNLOCK_LEVEL 사이 값이고,
// 거기 도달해야 다음 단계 잠금이 풀림. MainScreen이 상태를 들고 있고(밥 먹기 완료 시 갱신),
// MissionRoadmapScreen이 그걸 그대로 그려줌.
data class MissionLevelProgress(val level: Int, val starsEarned: Int)

const val TOTAL_MISSION_LEVELS = 8
const val STARS_TO_UNLOCK_LEVEL = 3

// 미션 로드맵 초기 상태: 1단계는 왼쪽 별 2개가 이미 채워진 채로 시작함(밥 먹기에서 3개 중
// 2개 이상 목표를 달성한 것과 같은 상태). 나머지 단계는 아직 아무것도 안 채워짐.
fun initialMissionLevels(): List<MissionLevelProgress> =
    (1..TOTAL_MISSION_LEVELS).map { level ->
        MissionLevelProgress(level = level, starsEarned = if (level == 1) 2 else 0)
    }
