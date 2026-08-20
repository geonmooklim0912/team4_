package com.example.team4uu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.team4uu.data.ChildProfileRules
import com.example.team4uu.ui.theme.FriendPink

// "자녀 이름 변경" 팝업. 현재 이름(initialName)을 미리 채워서 보여주고, 확인을 누르면
// 새 이름을 콜백으로 넘김(서버 전송·로컬 저장은 호출부 책임). 검증 기준은 회원가입/인형
// 이름 짓기와 같은 ChildProfileRules(서버 정화 기준과 동일)를 그대로 씀.
@Composable
fun ChildNameEditDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val trimmed = name.trim()
    val error = ChildProfileRules.nameError(name)
    val canConfirm = trimmed.isNotEmpty() && trimmed != initialName && error == null

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "자녀 이름 변경",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "인형이 이 이름으로 자녀를 불러요.",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    isError = trimmed.isNotEmpty() && error != null,
                    placeholder = { Text("이름 입력", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (trimmed.isNotEmpty() && error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = error, color = FriendPink, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "취소", color = Color.DarkGray)
                    }
                    Button(
                        onClick = { onConfirm(trimmed) },
                        enabled = canConfirm,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FriendPink)
                    ) {
                        Text(text = "저장", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
