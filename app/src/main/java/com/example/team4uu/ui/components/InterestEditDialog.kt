package com.example.team4uu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.team4uu.ui.theme.FriendPink

// "관심사 변경" 팝업. initiallySelected(현재 선택된 관심사, INTEREST_OPTIONS와 같은
// "🦕 공룡" 형식)를 미리 체크된 상태로 보여주고, 다시 누르면 선택 해제됨.
// 확인을 누르면 선택된 관심사들을 그대로 콜백으로 넘김(서버 전송용 텍스트 변환은 호출부 책임).
@Composable
fun InterestEditDialog(
    initiallySelected: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selected by remember { mutableStateOf(initiallySelected.toSet()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "관심사 변경", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "관심사 (${selected.size}/$MAX_INTERESTS)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    INTEREST_OPTIONS.forEach { interest ->
                        InterestChip(
                            text = interest,
                            selected = interest in selected,
                            onClick = {
                                selected = when {
                                    interest in selected -> selected - interest
                                    selected.size < MAX_INTERESTS -> selected + interest
                                    else -> selected
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onConfirm(selected.toList()) },
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
