package com.example.team4uu.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team4uu.R
import com.example.team4uu.data.ChildProfile
import com.example.team4uu.data.ChildProfileRules
import com.example.team4uu.ui.components.AuthField
import com.example.team4uu.ui.theme.FriendIconPink
import com.example.team4uu.ui.theme.FriendPink

// 입력 오류 한 줄. LoginScreen 의 로그인 실패 문구와 같은 스타일로 맞췄다.
@Composable
private fun FieldError(message: String) {
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = message,
        color = FriendPink,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
    )
}

// 회원가입 화면. 디자인 시안이 따로 없어 로그인 화면과 톤을 맞춰 간단히 구성함.
//
// 아이 이름·나이를 여기서 받는다. 계정 주인은 부모지만 **인형이 부르는 건 아이 이름**이라
// 라벨을 "아이 이름"으로 명시했다. 이 값이 AI 서버로 넘어가 인형이 "지우야" 하고 부른다.
@Composable
fun SignUpScreen(onSignUpComplete: (ChildProfile) -> Unit, onBackToLogin: () -> Unit) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordCheck by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var childName by remember { mutableStateOf("") }
    var childAge by remember { mutableStateOf("") }
    // 가입하기를 누르기 전에는 오류를 띄우지 않는다. 입력 중에 빨간 글씨가 계속 떠 있으면
    // 아직 다 쓰지도 않았는데 틀렸다고 하는 것처럼 보인다.
    var childNameError by remember { mutableStateOf<String?>(null) }
    var childAgeError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_main_clouds),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onBackToLogin,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "로그인으로 돌아가기", tint = Color.Black)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "회원가입",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))
            AuthField(label = "아이디", value = userId, onValueChange = { userId = it }, placeholder = "사용할 아이디를 입력해주세요")

            Spacer(modifier = Modifier.height(16.dp))
            AuthField(
                label = "비밀번호",
                value = password,
                onValueChange = { password = it },
                placeholder = "비밀번호를 입력해주세요",
                isPassword = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            AuthField(
                label = "비밀번호 확인",
                value = passwordCheck,
                onValueChange = { passwordCheck = it },
                placeholder = "비밀번호를 한번 더 입력해주세요",
                isPassword = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            AuthField(
                label = "이메일",
                value = email,
                onValueChange = { email = it },
                placeholder = "이메일을 입력해주세요"
            )

            Spacer(modifier = Modifier.height(16.dp))
            AuthField(
                label = "아이 이름",
                value = childName,
                onValueChange = {
                    childName = it
                    childNameError = null
                },
                placeholder = "인형이 부를 이름이에요"
            )
            childNameError?.let { FieldError(it) }

            Spacer(modifier = Modifier.height(16.dp))
            AuthField(
                label = "아이 나이",
                value = childAge,
                onValueChange = { input ->
                    // 숫자 키패드를 띄워도 붙여넣기로 문자가 들어올 수 있다. 여기서 막으면
                    // 사용자가 오류 메시지를 볼 일 자체가 없어진다.
                    childAge = input.filter { it.isDigit() }.take(2)
                    childAgeError = null
                },
                placeholder = "숫자만 입력해주세요",
                keyboardType = KeyboardType.Number
            )
            childAgeError?.let { FieldError(it) }

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = {
                    childNameError = ChildProfileRules.nameError(childName)
                    childAgeError = ChildProfileRules.ageError(childAge)
                    if (childNameError == null && childAgeError == null) {
                        onSignUpComplete(
                            ChildProfile(
                                name = childName.trim(),
                                age = childAge.trim().toInt()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "가입하기", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Text(text = "이미 계정이 있으신가요? ", fontSize = 13.sp, color = Color.DarkGray)
                Text(
                    text = "로그인",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FriendIconPink,
                    modifier = Modifier.clickable { onBackToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
