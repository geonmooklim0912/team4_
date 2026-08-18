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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team4uu.R
import com.example.team4uu.ui.components.AuthField
import com.example.team4uu.ui.theme.FriendIconPink
import com.example.team4uu.ui.theme.FriendPink

// 회원가입 화면. 디자인 시안이 따로 없어 로그인 화면과 톤을 맞춰 간단히 구성함.
@Composable
fun SignUpScreen(onSignUpComplete: () -> Unit, onBackToLogin: () -> Unit) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordCheck by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

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

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onSignUpComplete,
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
