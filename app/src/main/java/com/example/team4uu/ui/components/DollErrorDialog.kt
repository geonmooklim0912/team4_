package com.example.team4uu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.team4uu.data.remote.StylizeException
import com.example.team4uu.ui.theme.FriendCardTan
import com.example.team4uu.ui.theme.FriendPink

// 인형 등록이 실패했을 때 뜨는 모달.
//
// 버튼 구성이 실패 종류에 따라 달라진다 — 배경이 문제라 다시 찍어야 하는 경우와
// 그냥 한 번 더 눌러보면 되는 경우에 사용자가 해야 할 일이 다르기 때문이다.
@Composable
fun DollErrorDialog(
    error: StylizeException,
    onRetake: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = FriendCardTan,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error.userMessage,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "닫기", fontSize = 14.sp, color = Color.DarkGray)
                    }

                    // FATAL 은 사용자가 할 수 있는 게 없으므로 닫기만 남긴다.
                    // 재시도 버튼을 줘봐야 똑같이 실패하고, 그동안 호출 비용만 든다.
                    when (error.recovery) {
                        StylizeException.Recovery.RETAKE -> ActionButton(
                            text = "다시 촬영",
                            onClick = onRetake,
                            modifier = Modifier.weight(1f)
                        )

                        StylizeException.Recovery.RETRY -> ActionButton(
                            text = "다시 시도",
                            onClick = onRetry,
                            modifier = Modifier.weight(1f)
                        )

                        StylizeException.Recovery.FATAL -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(48.dp)
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
