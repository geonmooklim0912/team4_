package com.example.team4uu.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

// TODO: 백엔드 로그인 API 연동 전까지 쓰는 테스트 계정 (아이디: test / 비밀번호: 1234)
private const val TEST_USER_ID = "test"
private const val TEST_USER_PASSWORD = "1234"

// 테스트용 두 번째 계정: 친구가 하나도 없는 상태(EmptyFriendScreen)를 바로 확인하고 싶을 때 사용
private const val TEST_USER_ID_2 = "test2"
private const val TEST_USER_PASSWORD_2 = "12345"

// 신규 사용자 또는 로그아웃 상태에서 앱을 열었을 때 보여지는 로그인 화면.
// 로그인에 성공하면 기존 메인 화면("시작하기" -> 카메라)으로 넘어감.
// onLoginClick의 isEmptyTestAccount가 true면 test2 계정으로 로그인한 것 — 친구 목록을 비워서
// EmptyFriendScreen(온보딩)을 바로 볼 수 있게 함(MainScreen.kt에서 처리).
// 📌 로그인 경로가 둘이다.
//   - 테스트 계정(test/test2): 서버를 안 거친다. 화면 흐름만 확인하는 용도로 남겨 뒀다.
//     ⚠️ **토큰이 없으므로 인형 등록(stylize)과 대화(talk)는 401 로 실패한다.**
//   - 그 외: 서버에 로그인해 JWT 를 받는다. 실제 기능은 이쪽으로만 동작한다.
//
// 서버 호출 자체는 이 화면이 하지 않는다(MainScreen 이 코루틴을 들고 있다).
// 여기는 입력을 넘기고 결과 문구를 보여줄 뿐이다.
@Composable
fun LoginScreen(
    onLoginClick: (isEmptyTestAccount: Boolean) -> Unit,
    onSignUpClick: () -> Unit,
    onServerLogin: (id: String, password: String) -> Unit = { _, _ -> },
    isLoggingIn: Boolean = false,
    serverError: String? = null
) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 서버까지 갈 것도 없이 앱에서 막는 경우(빈 칸)만 여기서 쓴다.
    // 아이디·비밀번호가 틀렸는지는 서버만 알 수 있으므로 serverError 로 온다.
    var inputError by remember { mutableStateOf<String?>(null) }

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
            Spacer(modifier = Modifier.height(140.dp))
            Text(
                text = "친구와의 특별한 추억을 쌓고싶어.",
                fontSize = 15.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "인형을 핸드폰 세상 속으로\n데려와 볼까?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(40.dp))
            AuthField(label = "아이디", value = userId, onValueChange = { userId = it }, placeholder = "아이디를 입력해주세요")

            Spacer(modifier = Modifier.height(16.dp))
            AuthField(
                label = "비밀번호",
                value = password,
                onValueChange = { password = it },
                placeholder = "비밀번호를 입력해주세요",
                isPassword = true
            )

            // 입력 문제(빈 칸)가 있으면 그걸 먼저 보여준다. 서버까지 갔다 온 실패는
            // 그다음이다 — 방금 누른 것에 대한 답이 먼저 보여야 한다.
            (inputError ?: serverError)?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    color = FriendPink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    inputError = null
                    when {
                        // 테스트 계정은 서버를 안 거친다(화면 흐름 확인용).
                        // ⚠️ 토큰이 없으므로 인형 등록·대화는 401 로 실패한다.
                        userId == TEST_USER_ID && password == TEST_USER_PASSWORD ->
                            onLoginClick(false)

                        userId == TEST_USER_ID_2 && password == TEST_USER_PASSWORD_2 ->
                            onLoginClick(true)

                        userId.isBlank() || password.isBlank() ->
                            inputError = "아이디와 비밀번호를 입력해주세요."

                        // 나머지는 전부 서버로. 맞는지 틀린지는 서버만 안다.
                        else -> onServerLogin(userId.trim(), password)
                    }
                },
                // 연타하면 로그인 요청이 여러 번 나간다.
                enabled = !isLoggingIn,
                colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (isLoggingIn) "로그인 중..." else "로그인",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "아이디 찾기",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.clickable { /* TODO: 백엔드 준비 후 아이디 찾기 구현 */ }
                )
                Text(text = "   |   ", fontSize = 13.sp, color = Color.DarkGray)
                Text(
                    text = "비밀번호 찾기",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.clickable { /* TODO: 백엔드 준비 후 비밀번호 찾기 구현 */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "처음 오셨나요?", fontSize = 14.sp, color = Color.DarkGray)
                Text(
                    text = "가입하기",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FriendIconPink,
                    modifier = Modifier.clickable { onSignUpClick() }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
