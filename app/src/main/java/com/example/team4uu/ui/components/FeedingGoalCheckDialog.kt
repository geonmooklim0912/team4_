package com.example.team4uu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.team4uu.ui.theme.FriendPink

// 밥 먹기 화면에서 "끝내기"를 누르면 뜨는 팝업. 밥 먹기를 시작할 때 골랐던 목표(최대 3개)를
// 체크박스로 다시 보여주고, 몇 개나 해냈는지 체크한 뒤 확인을 누르면 그 결과를 그대로 넘김
// (2개 이상 체크하면 호출부(MissionViewModel)가 미션 로드맵 별을 하나 채움).
@Composable
fun FeedingGoalCheckDialog(
    goals: List<String>,
    onConfirm: (achievedGoals: Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var achieved by remember { mutableStateOf(setOf<String>()) }

    fun toggle(goal: String) {
        achieved = if (goal in achieved) achieved - goal else achieved + goal
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "오늘의 목표, 얼마나 해냈나요?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "2개 이상 체크하면 미션 로드맵 별을 하나 모을 수 있어요!",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (goals.isEmpty()) {
                    Text(
                        text = "이번 밥 먹기에는 설정된 목표가 없어요.",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    goals.forEach { goal ->
                        val checked = goal in achieved
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { toggle(goal) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { toggle(goal) },
                                colors = CheckboxDefaults.colors(checkedColor = FriendPink)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = goal, fontSize = 14.sp, color = Color.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onConfirm(achieved) },
                    colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(text = "확인", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
