package com.example.team4uu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.team4uu.ui.theme.FriendPink

// 홈 화면 좌측 상단 톱니바퀴를 누르면 뜨는 설정 메뉴.
@Composable
fun SettingsMenuDialog(
    onDismiss: () -> Unit,
    onEditNameClick: () -> Unit,
    onEditInterestsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp).width(220.dp)) {
                Text(text = "설정", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                SettingsMenuRow(text = "자녀 이름 변경", onClick = onEditNameClick)
                Spacer(modifier = Modifier.height(4.dp))
                SettingsMenuRow(text = "관심사 변경", onClick = onEditInterestsClick)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFEEEEEE))
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsMenuRow(text = "로그아웃", color = FriendPink, onClick = onLogoutClick)
            }
        }
    }
}

@Composable
private fun SettingsMenuRow(text: String, color: Color = Color.Black, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    )
}
