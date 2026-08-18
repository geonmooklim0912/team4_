package com.example.team4uu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team4uu.ui.theme.FriendCardTan

// 로그인/가입 화면이 함께 쓰는 라벨 + 알약 모양 입력창
//
// keyboardType 은 기본값이 있어서 기존 호출부(아이디/비밀번호/이메일)는 그대로 둬도 된다.
// 아이 나이처럼 숫자만 받는 칸에서 KeyboardType.Number 를 넘기면 숫자 키패드가 뜬다.
// ⚠️ 키패드를 숫자로 바꿔도 입력값 검증은 따로 해야 한다 — 붙여넣기로 문자가 들어올 수 있다.
@Composable
fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Unspecified
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, color = Color(0xFFA89B6B)) },
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = when {
                isPassword -> KeyboardOptions(keyboardType = KeyboardType.Password)
                keyboardType != KeyboardType.Unspecified -> KeyboardOptions(keyboardType = keyboardType)
                else -> KeyboardOptions.Default
            },
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = FriendCardTan,
                focusedContainerColor = FriendCardTan,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}
