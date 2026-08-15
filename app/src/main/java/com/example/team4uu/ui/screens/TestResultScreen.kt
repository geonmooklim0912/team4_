package com.example.team4uu.ui.screens

import androidx.compose.runtime.Composable
import com.example.team4uu.data.Friend

// 백엔드 연동 전까지 example2 목업 캐릭터로 "친구 등록 후 뜨는 메인 홈" 화면을 미리 보여주는 테스트 진입점.
// 실제 메인 홈(MainHomeContent, HomeScreen.kt)과 동일하게 FriendHomeScreen을 그대로 쓰기 때문에,
// 여기서 새 친구를 등록하면 실제 친구 목록에도 반영되고 옵션 패널에도 그대로 나타남.
@Composable
fun TestResultScreen(friends: List<Friend>, onAddFriendClick: () -> Unit, onClose: () -> Unit) {
    FriendHomeScreen(friends = friends, onAddFriendClick = onAddFriendClick, onClose = onClose)
}
