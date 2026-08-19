package com.example.team4uu.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team4uu.ui.theme.FriendCardTan
import com.example.team4uu.ui.theme.FriendPink

// 관심사로 고를 수 있는 항목(고정 10개). 회원가입 화면과 홈 화면의 관심사 변경 팝업에서 공용으로 씀.
val INTEREST_OPTIONS = listOf(
    "🦕 공룡",
    "🚗 탈것·자동차",
    "🪐 우주·행성",
    "👑 공주·마법",
    "🐰 동물 친구들",
    "🤖 로봇·히어로",
    "🐠 바다 생물",
    "🐛 자연·곤충",
    "🎵 음악·노래",
    "🎨 그리기·만들기"
)
const val MAX_INTERESTS = 3

// INTEREST_OPTIONS의 항목("🦕 공룡")에서 서버로 보낼 순수 텍스트("공룡")만 추출
fun interestKeyword(option: String): String = option.substringAfter(" ")

// 관심사 선택 칩 하나. 선택되면 핑크, 아니면 기본 톤(FriendCardTan).
@Composable
fun InterestChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) FriendPink else FriendCardTan,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.DarkGray,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
