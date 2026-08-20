package com.example.team4uu.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.team4uu.R
import com.example.team4uu.data.ChildProfile
import com.example.team4uu.data.ChildProfileRules
import com.example.team4uu.ui.components.AuthField
import com.example.team4uu.ui.components.INTEREST_OPTIONS
import com.example.team4uu.ui.components.InterestChip
import com.example.team4uu.ui.components.MAX_INTERESTS
import com.example.team4uu.ui.components.interestKeyword
import com.example.team4uu.ui.theme.FriendIconPink
import com.example.team4uu.ui.theme.FriendPink
import com.example.team4uu.viewmodel.AuthViewModel

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
fun SignUpScreen(onSignUpComplete: () -> Unit, onBackToLogin: () -> Unit) {
    val authViewModel: AuthViewModel = viewModel()

    val context = LocalContext.current

    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordCheck by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var childName by remember { mutableStateOf("") }
    var childAge by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }

    var signUpError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun submit() {
        signUpError = when {
            userId.isBlank() || password.isBlank() || email.isBlank() -> "아이디/비밀번호/이메일을 입력해주세요."
            password != passwordCheck -> "비밀번호가 일치하지 않습니다."
            else -> null
        }
        if (signUpError != null) return

        isSubmitting = true
        authViewModel.signUp(
            username = userId,
            password = password,
            email = email,
            name = childName,
            age = childAge.toIntOrNull() ?: 0,
            // 관심사 칩은 "🦕 공룡"처럼 이모지가 붙어있어서, 서버로는 이모지를 뺀 텍스트만 콤마로 이어서 보냄
            keyword = selectedInterests.joinToString(",") { interestKeyword(it) },
            onSuccess = {
                isSubmitting = false
                Toast.makeText(context, "요청 성공", Toast.LENGTH_SHORT).show()
                onSignUpComplete()
            },
            onError = { message ->
                isSubmitting = false
                signUpError = message
            }
        )
    }

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
                label = "자녀 이름",
                value = childName,
                onValueChange = { childName = it },
                placeholder = "자녀 이름을 입력해주세요"
            )

            Spacer(modifier = Modifier.height(16.dp))
            AuthField(
                label = "나이",
                value = childAge,
                onValueChange = { input -> if (input.all { it.isDigit() }) childAge = input },
                placeholder = "아이의 나이를 입력해주세요(숫자만)",
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "관심사 (${selectedInterests.size}/$MAX_INTERESTS)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                INTEREST_OPTIONS.forEach { interest ->
                    InterestChip(
                        text = interest,
                        selected = interest in selectedInterests,
                        onClick = {
                            selectedInterests = when {
                                interest in selectedInterests -> selectedInterests - interest
                                selectedInterests.size < MAX_INTERESTS -> selectedInterests + interest
                                else -> selectedInterests
                            }
                        }
                    )
                }
            }

            if (signUpError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = signUpError!!,
                    color = FriendPink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { submit() },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (isSubmitting) "가입 중..." else "가입하기",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
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
