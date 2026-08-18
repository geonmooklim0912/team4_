package com.example.team4uu.ui

// TODO: 실제로는 미션 로드맵(MissionRoadmapScreen)에서 도달한 최고 스테이지를 저장하는 공용 상태
// (ViewModel/DB 등)에서 받아와야 함. 아직 그 저장소가 없어서, 배경/친구 칸 잠금 해제 로직이
// 전부 이 값 하나를 함께 참조하도록 임시로 묶어둠(아무 것도 클리어하지 않은 상태인 0으로 고정).
const val CURRENT_MISSION_STAGE = 0
