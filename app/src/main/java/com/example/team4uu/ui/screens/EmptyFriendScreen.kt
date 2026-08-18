package com.example.team4uu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team4uu.ui.theme.FriendCardTan
import com.example.team4uu.ui.theme.FriendPink

// 친구가 한 마리도 없을 때(온보딩) 보여지는 화면. "시작하기"를 누르면 카메라로 연결됨.
@Composable
fun EmptyFriendContent(onStartClick: () -> Unit, onTestClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(180.dp))

        SpeechBubble(text = "친구가 없어요..\n지금 촬영하고 등록 해보세요!")

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStartClick,
            colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(200.dp)
                .height(56.dp)
        ) {
            Text(text = "시작하기", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        // TODO: 백엔드에서 촬영->2D 변환 로직이 완성되면 이 임시 test 버튼은 제거
        TextButton(onClick = onTestClick) {
            Text(text = "test", fontSize = 12.sp, color = Color.DarkGray)
        }
    }
}

// 말풍선 모양 안내 문구: 둥근 카드 + 아래로 뾰족한 꼬리
@Composable
private fun SpeechBubble(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = FriendCardTan,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                lineHeight = 20.sp
            )
        }
        // 마름모를 절반만 겹치게 배치해서 말풍선 꼬리(삼각형)처럼 보이게 함
        Box(
            modifier = Modifier
                .offset(y = (-8).dp)
                .size(16.dp)
                .rotate(45f)
                .background(FriendCardTan)
        )
    }
}
