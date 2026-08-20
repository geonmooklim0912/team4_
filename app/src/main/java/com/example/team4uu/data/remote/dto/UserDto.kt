package com.example.team4uu.data.remote.dto

// 관심사 변경(PATCH /api/users/me/keyword): 선택한 관심사 리스트를 그대로 보냄
data class UpdateKeywordRequest(
    val keyword: List<String>
)

// 자녀 이름 변경(PATCH /api/users/me/name): 새 이름을 그대로 보냄
data class UpdateNameRequest(
    val name: String
)
