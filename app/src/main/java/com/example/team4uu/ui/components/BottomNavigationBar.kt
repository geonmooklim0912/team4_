package com.example.team4uu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team4uu.ui.theme.FriendCardTan
import com.example.team4uu.ui.theme.FriendIconPink

// 친구가 아직 없는 온보딩 화면에서 쓰는 고정 하단바.
// 친구 등록 후에는 대신 HomeOptionsPanel 안에 같은 버튼들이 접었다 펼 수 있는 형태로 들어감.
@Composable
fun BottomNavigationBar() {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BottomNavItem(
                modifier = Modifier.weight(1f),
                text = "밥 먹기",
                icon = Icons.Default.Restaurant
            )
            BottomNavItem(
                modifier = Modifier.weight(1f),
                text = "놀기",
                icon = Icons.Default.Mood
            )
        }
    }
}

@Composable
fun BottomNavItem(modifier: Modifier = Modifier, text: String, icon: ImageVector) {
    Surface(
        color = FriendCardTan,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(100.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = FriendIconPink
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}
