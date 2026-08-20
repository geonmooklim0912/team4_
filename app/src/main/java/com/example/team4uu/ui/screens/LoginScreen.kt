package com.example.team4uu.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.team4uu.R
import com.example.team4uu.ui.components.AuthField
import com.example.team4uu.ui.theme.FriendIconPink
import com.example.team4uu.ui.theme.FriendPink
import com.example.team4uu.viewmodel.AuthViewModel

// 신규 사용자 또는 로그아웃 상태에서 앱을 열었을 때 보여지는 로그인 화면.
// 로그인에 성공하면 기존 메인 화면("시작하기" -> 카메라)으로 넘어감.
// username은 로그인한 계정의 아이디로, 계정별로 친구 목록을 분리해서 보여주는 데 씀(FriendViewModel 참고).
// 친구가 하나도 없는 계정으로 로그인하면 MainScreen이 자동으로 EmptyFriendScreen(온보딩)을 보여줌.
@Composable
fun LoginScreen(onLoginClick: (username: String) -> Unit, onSignUpClick: () -> Unit) {
    val authViewModel: AuthViewModel = viewModel()

    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var loginError by remember { mutableStateOf(false) }

    //Toast 메시지 등을 띄울 때 필요한 Context
    val context = LocalContext.current

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
            AuthField(label = "아이디", value = account, onValueChange = { account = it }, placeholder = "아이디를 입력해주세요")

            Spacer(modifier = Modifier.height(16.dp))
            AuthField(
                label = "비밀번호",
                value = password,
                onValueChange = { password = it },
                placeholder = "비밀번호를 입력해주세요",
                isPassword = true
            )

            if (loginError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "아이디 또는 비밀번호가 일치하지 않습니다.",
                    color = FriendPink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    authViewModel.login(
                        username = account,
                        password = password,
                        onSuccess = {
                            loginError = false
                            onLoginClick(account)
                        },
                        onError = {
                            loginError = true
                            Toast.makeText(context, "네트워크 오류로 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "로그인",
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
