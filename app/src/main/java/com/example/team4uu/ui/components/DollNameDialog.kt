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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.team4uu.ui.theme.FriendPink

// 인형 사진을 찍은 직후 이름을 짓는 팝업. 기획서의 DollNameInput 화면이다.
//
// 이 이름은 화면에만 쓰이는 게 아니라 **대화에서 인형이 자기를 부르는 이름**이 된다
// (WS /doll/talk?doll=... → 서버 페르소나). 그래서 아무 값이나 받으면 안 되고,
// 서버가 정화하는 기준(한글·영문, 길이 상한)과 같은 선에서 미리 막는다.
//
// ⚠️ 취소하면 등록 자체를 하지 않는다. 등록 1회에 약 325원이 들고 28초가 걸려서,
//    이름을 안 지었는데 진행되면 사용자가 원치 않은 비용이 나간다.
@Composable
fun DollNameDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    val trimmed = name.trim()
    val error = when {
        trimmed.isEmpty() -> null                       // 아직 안 쳤을 뿐이라 오류로 보지 않는다
        trimmed.length > MAX_NAME_LEN -> "이름은 ${MAX_NAME_LEN}자까지 지을 수 있어요."
        !NAME_PATTERN.matches(trimmed) -> "한글 또는 영문으로 지어주세요."
        else -> null
    }
    val canConfirm = trimmed.isNotEmpty() && error == null

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "이름을 지어주세요!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "친구가 이 이름으로 대답해요.",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    // 상한을 넘겨 치는 것 자체를 막지 않고 오류 문구로 알린다 —
                    // 입력이 조용히 잘리면 사용자는 왜 이름이 달라졌는지 모른다.
                    onValueChange = { name = it },
                    singleLine = true,
                    isError = error != null,
                    placeholder = { Text("예: 초록이", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black,
                        cursorColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
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
                    Spacer(modifier = Modifier.height(0.dp))
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

// 서버(server/profile.py 의 _clean_name)와 같은 기준. 앱이 통과시킨 값을 서버가
// 조용히 잘라내면 사용자는 왜 인형 이름이 달라졌는지 알 수 없다.
private const val MAX_NAME_LEN = 10
private val NAME_PATTERN = Regex("^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ]+$")
