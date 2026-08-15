package com.example.team4uu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team4uu.ui.theme.FriendPink

// 친구가 한 마리도 없을 때(온보딩) 보여지는 화면. "시작하기"를 누르면 카메라로 연결됨.
@Composable
fun EmptyFriendContent(onStartClick: () -> Unit, onTestClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "아직 친구가 없습니다.\n좋아하는 장남감을 지금 바로 등록해보세요.",
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStartClick,
            colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
            shape = RoundedCornerShape(30.dp),
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
