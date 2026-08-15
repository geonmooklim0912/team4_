package com.example.team4uu

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.team4uu.data.Friend
import com.example.team4uu.viewmodel.FriendViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.team4uu.ui.theme.CameraBackgroundDark
import com.example.team4uu.ui.theme.FriendCardTan
import com.example.team4uu.ui.theme.FriendIconPink
import com.example.team4uu.ui.theme.FriendPink
import com.example.team4uu.ui.theme.FriendYellow
import com.example.team4uu.ui.theme.Team4UUTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.launch


//메인 화면(앱을 실행했을 때 보이는 화면)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() //화면 전체(상태바까지)
        setContent { //이 액티비티가 보여줄 화면(UI)를 지정하는 함수
            Team4UUTheme {
                MainScreen() //실제로 화면 내용을 그리는 Composable 함수
            }
        }
    }
}

private enum class AuthStep { LOGIN, SIGNUP }

// TODO: 백엔드 로그인 API 연동 전까지 쓰는 테스트 계정 (아이디: test / 비밀번호: 1234)
private const val TEST_USER_ID = "test"
private const val TEST_USER_PASSWORD = "1234"

@Composable //화면을 그리는 함수라는 의미의 어노테이션
fun MainScreen(friendViewModel: FriendViewModel = viewModel()) {
    var showCamera by remember { mutableStateOf(false) } //ShowCamera 값이 바뀌면 화면을 다시만드는 상태 객체
    // TODO: 실제 촬영 -> 2D 변환 로직이 백엔드와 함께 구현되기 전까지, 결과 화면을 미리 볼 수 있는 테스트용 상태
    var showTestResult by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) } // TODO: 백엔드 인증 연동 후 실제 로그인 상태(토큰 존재 여부 등)로 교체
    var authStep by remember { mutableStateOf(AuthStep.LOGIN) }
    val context = LocalContext.current //현재 앱의 컨텍스트를 갖고 옴(권환 확인 시스템 기능을 쓸 때 필요)
    val friends by friendViewModel.friends.collectAsState() // Room DB에 저장된 친구 목록(실시간 반영)

    //카메라 권한 요청을 띄우는 팝업
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> // 허용/거부를 누른 뒤 실행되는 콜백
        if (isGranted) {
            showCamera = true
        }

    }

    fun requestCameraOrOpen() { //친구 만들기 버튼(온보딩/메인 홈 공통)을 눌렀을 때
        //카메라 권한이 있는지 확인
        val permissionCheckResult = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )
        // 권한이 있을 때, 바로 카메라 화면을 띄움
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            showCamera = true
        } else { //권한이 없으면 권한 요청 팝업을 띄움
            permissionLauncher.launch(Manifest.permission.CAMERA) //위에서 생성한 런처 객체 (여기서는 카메라 권한을 요청으로 넣음)
        }
    }

    if (!isLoggedIn) { // 로그인 전에는 항상 로그인/가입 화면부터 보여줌
        when (authStep) {
            AuthStep.LOGIN -> LoginScreen(
                onLoginClick = {
                    // TODO: 백엔드 로그인 API 연동 전까지는 입력값과 무관하게 로그인 성공으로 간주
                    isLoggedIn = true
                },
                onSignUpClick = { authStep = AuthStep.SIGNUP }
            )
            AuthStep.SIGNUP -> SignUpScreen(
                onSignUpComplete = {
                    // TODO: 백엔드 회원가입 API 연동 전까지는 입력값과 무관하게 가입 성공으로 간주
                    isLoggedIn = true
                },
                onBackToLogin = { authStep = AuthStep.LOGIN }
            )
        }
    } else if (showCamera) {
        CameraScreen(
            onClose = { showCamera = false }, //카메라 화면을 띄우지만 닫기를 누르면 showCamera를 false
            onPhotoCaptured = { imagePath ->
                // TODO(F4): 지금은 임시 이름으로 저장. 이름 입력 다이얼로그가 만들어지면 그 값으로 대체.
                friendViewModel.addFriend(name = "친구 ${friends.size + 1}", imagePath = imagePath)
                showCamera = false
            }
        )
    } else if (showTestResult) {
        // TODO: 백엔드 연동 전까지 example2 목업 캐릭터로 결과 화면 연출을 미리 보여주는 테스트 화면
        TestResultScreen(
            friends = friends,
            onAddFriendClick = ::requestCameraOrOpen,
            onClose = { showTestResult = false }
        )
    } else { //카메라를 보여줄 상황이 아니면 메인 화면을 띄움
        Scaffold(//화면의 기본 뼈대를 잡아주는 compose의 부품임
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // 상태 바 영역까지 배경 이미지가 비치도록 흰 배경을 없앰
            // 친구가 있으면 MainHomeContent가 밥먹기/놀기 버튼까지 접었다 펼 수 있는 옵션 패널로 직접 그림
            bottomBar = { if (friends.isEmpty()) BottomNavigationBar() }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) { //메인 화면의 본문 영역
                // 상태 바 아래로도 이어지도록 innerPadding 적용 전에 전체 화면을 채움
                Image(
                    painter = painterResource(id = R.drawable.bg_main_clouds),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // 친구 목록 조회 결과로 분기: 0마리면 온보딩, 1마리 이상이면 메인 홈
                    if (friends.isEmpty()) {
                        EmptyFriendContent(
                            onStartClick = ::requestCameraOrOpen,
                            onTestClick = { showTestResult = true }
                        )
                    } else {
                        MainHomeContent(friends = friends, onAddFriendClick = ::requestCameraOrOpen)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyFriendContent(onStartClick: () -> Unit, onTestClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "아직 친구가 없습니다.\n좋아하는 장남감을 지금 바로 등록해보세요.",
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStartClick,
            colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .width(200.dp)
                .height(56.dp)
        ) {
            Text(text = "시작하기", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        // TODO: 백엔드에서 촬영->2D 변환 로직이 완성되면 이 임시 test 버튼은 제거
        TextButton(onClick = onTestClick) {
            Text(text = "test", fontSize = 12.sp, color = Color.DarkGray)
        }
    }
}

// TODO: 실제로는 백엔드가 돌려주는 사용자 2D 캐릭터 이미지를 받아서 그려야 함.
// 지금은 백엔드 연동 전이라 design_img/example2(배경 제거된 캐릭터 목업)를 대신 배경 위에 올려서 보여줌.
// 토킹톰처럼 "살아있는 느낌"을 주기 위해 평소엔 숨쉬듯 살짝 커졌다 작아지고,
// 탭하면 통통 튀는 스쿼시 애니메이션이 재생됨(사운드/백엔드 없이 순수 애니메이션으로만 구현).
// 토킹톰처럼 "살아있는 느낌"을 주는 캐릭터 공용 효과.
// 평소엔 숨쉬듯 살짝 커졌다 작아지고, 탭하면 통통 튀는 스쿼시 애니메이션이 재생됨.
// TestResultScreen과 MainHomeContent(방 무대)가 함께 사용.
fun Modifier.livingCharacterEffect(): Modifier = composed {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    val bounceOffsetPx = remember { Animatable(0f) }
    val squishX = remember { Animatable(1f) }
    val squishY = remember { Animatable(1f) }

    fun onTapped() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            bounceOffsetPx.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialVelocity = with(density) { (-900).dp.toPx() }
            )
        }
        scope.launch {
            squishY.animateTo(0.78f, animationSpec = tween(70))
            squishY.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        scope.launch {
            squishX.animateTo(1.18f, animationSpec = tween(70))
            squishX.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    this
        .offset { IntOffset(0, bounceOffsetPx.value.roundToInt()) }
        .graphicsLayer {
            scaleX = breathScale * squishX.value
            scaleY = breathScale * squishY.value
            transformOrigin = TransformOrigin(0.5f, 1f) // 바닥에 붙어 눌리는 느낌
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onTapped() }
}

// 백엔드 연동 전까지 example2 목업 캐릭터로 "친구 등록 후 뜨는 메인 홈" 화면을 미리 보여주는 테스트 진입점.
// 실제 메인 홈(MainHomeContent)과 동일하게 FriendHomeScreen을 그대로 쓰기 때문에,
// 여기서 새 친구를 등록하면 실제 친구 목록에도 반영되고 옵션 패널에도 그대로 나타남.
@Composable
fun TestResultScreen(friends: List<Friend>, onAddFriendClick: () -> Unit, onClose: () -> Unit) {
    FriendHomeScreen(friends = friends, onAddFriendClick = onAddFriendClick, onClose = onClose)
}

// 신규 사용자 또는 로그아웃 상태에서 앱을 열었을 때 보여지는 로그인 화면.
// 로그인에 성공하면 기존 메인 화면("시작하기" -> 카메라)으로 넘어감.
@Composable
fun LoginScreen(onLoginClick: () -> Unit, onSignUpClick: () -> Unit) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

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
                text = "인형과의 특별한 추억을 쌓고싶어.",
                fontSize = 15.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "친구를 핸드폰 세상 속으로\n데려와 볼까?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(40.dp))
            AuthField(label = "아이디", value = userId, onValueChange = { userId = it }, placeholder = "아이디를 입력해주세요")

            Spacer(modifier = Modifier.height(20.dp))
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
                    // TODO: 백엔드 로그인 API 연동 전까지는 테스트 계정(test / 1234)으로만 통과
                    if (userId == TEST_USER_ID && password == TEST_USER_PASSWORD) {
                        loginError = false
                        onLoginClick()
                    } else {
                        loginError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "로그인", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            Surface(
                color = FriendCardTan,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "처음이에요", fontSize = 14.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "가입하기",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FriendIconPink,
                        modifier = Modifier.clickable { onSignUpClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// 회원가입 화면. 디자인 시안이 따로 없어 로그인 화면과 톤을 맞춰 간단히 구성함.
@Composable
fun SignUpScreen(onSignUpComplete: () -> Unit, onBackToLogin: () -> Unit) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordCheck by remember { mutableStateOf("") }

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
                text = "환영해요!",
                fontSize = 15.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "친구를 만나기 위한\n계정을 만들어볼까요?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(36.dp))
            AuthField(label = "아이디", value = userId, onValueChange = { userId = it }, placeholder = "사용할 아이디를 입력해주세요")

            Spacer(modifier = Modifier.height(20.dp))
            AuthField(
                label = "비밀번호",
                value = password,
                onValueChange = { password = it },
                placeholder = "비밀번호를 입력해주세요",
                isPassword = true
            )

            Spacer(modifier = Modifier.height(20.dp))
            AuthField(
                label = "비밀번호 확인",
                value = passwordCheck,
                onValueChange = { passwordCheck = it },
                placeholder = "비밀번호를 한번 더 입력해주세요",
                isPassword = true
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

// 로그인/가입 화면이 함께 쓰는 라벨 + 알약 모양 입력창
@Composable
fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
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
            keyboardOptions = if (isPassword) {
                KeyboardOptions(keyboardType = KeyboardType.Password)
            } else {
                KeyboardOptions.Default
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

// F6(메인 홈 연출)이 만들어지기 전까지 쓰는 임시 화면.
// 친구가 1마리 이상일 때 보이는 메인 홈. TestResultScreen과 완전히 같은 화면(FriendHomeScreen)을 씀 -
// 차이는 닫기(X) 버튼 유무뿐(메인 홈은 최상위 화면이라 닫을 필요가 없음).
@Composable
fun MainHomeContent(friends: List<Friend>, onAddFriendClick: () -> Unit) {
    FriendHomeScreen(friends = friends, onAddFriendClick = onAddFriendClick, onClose = null)
}

// 위쪽은 현재 선택된 친구가 서 있는 방, 아래쪽은 화살표/스와이프로 접었다 펼 수 있는
// 새 친구 등록하기 + 친구 목록 + 밥먹기/놀기 옵션 패널.
@Composable
private fun FriendHomeScreen(
    friends: List<Friend>,
    onAddFriendClick: () -> Unit,
    onClose: (() -> Unit)?
) {
    var selectedFriendId by remember { mutableStateOf(friends.lastOrNull()?.id) }
    // 목록이 바뀌어도(친구 추가 등) 선택이 항상 유효하도록 유지하고, 새로 등록된 친구를 자동 선택
    LaunchedEffect(friends) {
        if (friends.none { it.id == selectedFriendId }) {
            selectedFriendId = friends.lastOrNull()?.id
        }
    }
    val selectedFriend = friends.find { it.id == selectedFriendId }

    var selectorExpanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            Image(
                painter = painterResource(id = R.drawable.bg_window_room),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // TODO: 백엔드가 친구별 배경 제거 2D 이미지(characterAssetPath)를 내려주면 그걸로 교체.
            // 지금은 백엔드 연동 전이라, 선택된 친구가 없거나 이미지가 없으면 example2 목업 캐릭터를 보여줌.
            val characterAssetPath = selectedFriend?.characterAssetPath
            val characterPainter = if (characterAssetPath != null) {
                val bitmap = remember(characterAssetPath) {
                    BitmapFactory.decodeFile(characterAssetPath)?.asImageBitmap()
                }
                bitmap?.let { BitmapPainter(it) } ?: painterResource(id = R.drawable.character_example2)
            } else {
                painterResource(id = R.drawable.character_example2)
            }

            Image(
                painter = characterPainter,
                contentDescription = selectedFriend?.name ?: "캐릭터 미리보기",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(screenWidth * 0.62f)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = screenHeight * 0.135f)
                    .livingCharacterEffect()
            )

            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = Color.Black)
                }
            }
        }

        HomeOptionsPanel(
            friends = friends,
            selectedFriendId = selectedFriendId,
            expanded = selectorExpanded,
            onExpandedChange = { selectorExpanded = it },
            onSelectFriend = { selectedFriendId = it.id },
            onAddFriendClick = onAddFriendClick
        )
    }
}

// 새 친구 등록하기/친구 목록/밥먹기/놀기를 담은 옵션 패널.
// 화살표 버튼(또는 위아래 스와이프)으로 접었다 펼 수 있고, 접으면 그만큼 위 방 화면이 자연스럽게 넓어짐.
@Composable
private fun HomeOptionsPanel(
    friends: List<Friend>,
    selectedFriendId: Long?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectFriend: (Friend) -> Unit,
    onAddFriendClick: () -> Unit
) {
    Surface(color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 10f) onExpandedChange(false)
                        else if (dragAmount < -10f) onExpandedChange(true)
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 옵션 접기/펼치기 화살표: 접혀 있을 때도 항상 보여서 언제든 다시 펼칠 수 있음
            IconButton(onClick = { onExpandedChange(!expanded) }) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (expanded) "옵션 접기" else "옵션 펼치기",
                    tint = Color.DarkGray
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AddFriendButton(onClick = onAddFriendClick)
                        friends.forEach { friend ->
                            FriendThumbnail(
                                friend = friend,
                                selected = friend.id == selectedFriendId,
                                onClick = { onSelectFriend(friend) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BottomNavItem(modifier = Modifier.weight(1f), text = "밥 먹기", icon = Icons.Default.Restaurant)
                        BottomNavItem(modifier = Modifier.weight(1f), text = "놀기", icon = Icons.Default.Mood)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFriendButton(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(FriendPink)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "새 친구 등록하기", tint = Color.White)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "새 친구 등록하기",
            fontSize = 11.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp)
        )
    }
}

@Composable
private fun FriendThumbnail(friend: Friend, selected: Boolean, onClick: () -> Unit) {
    // TODO: 지금은 촬영 원본 사진(imagePath)을 썸네일로 보여줌. 배경 제거 캐릭터 이미지가 생기면 그걸로 교체.
    val bitmap = remember(friend.imagePath) { BitmapFactory.decodeFile(friend.imagePath)?.asImageBitmap() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(FriendCardTan)
                .border(
                    width = if (selected) 3.dp else 0.dp,
                    color = FriendPink,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = friend.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = friend.name,
            fontSize = 11.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp)
        )
    }
}

@Composable
fun BottomNavigationBar() {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BottomNavItem(
                modifier = Modifier.weight(1f),
                text = "밥 먹기",
                icon = Icons.Default.Restaurant
            )
            BottomNavItem(
                modifier = Modifier.weight(1f),
                text = "놀기",
                icon = Icons.Default.Mood
            )
        }
    }
}

@Composable
fun BottomNavItem(modifier: Modifier = Modifier, text: String, icon: ImageVector) {
    Surface(
        color = FriendCardTan,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(100.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = FriendIconPink
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun CameraScreen(onClose: () -> Unit, onPhotoCaptured: (String) -> Unit) {
    val context = LocalContext.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraBackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Text(
            text = "촬영(스캔)",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
        )

        // 카메라 프리뷰 영역: 화면 대부분을 차지하고, 셔터 버튼도 그 위에 겹쳐서 표시
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black)
        ) {
            CameraPreview(imageCapture = imageCapture)

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            // 가이드 영역(위쪽, 남는 공간을 채움)과 TIP/셔터 영역(아래쪽, 고정 높이)을 분리해
            // 화면 크기와 상관없이 TIP 박스가 가이드 모서리와 겹치지 않도록 함
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CameraOverlayGuides(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(top = 56.dp, bottom = 12.dp)
                            .width(260.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CameraTipBox()
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(6.dp, FriendPink, CircleShape)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(FriendCardTan)
                            .clickable {
                                takePhoto(context, imageCapture, onPhotoCaptured)
                            }
                    )
                }
            }
        }
    }
}

private fun takePhoto(context: Context, imageCapture: ImageCapture, onPhotoCaptured: (String) -> Unit) {
    val outputDirectory = context.cacheDir
    val photoFile = File(
        outputDirectory,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
                Toast.makeText(context, "사진 촬영 실패", Toast.LENGTH_SHORT).show()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d("CameraScreen", "Photo capture succeeded: ${photoFile.absolutePath}")
                Toast.makeText(context, "사진이 저장되었습니다!", Toast.LENGTH_SHORT).show()
                onPhotoCaptured(photoFile.absolutePath)
            }
        }
    )
}

@Composable
fun CameraOverlayGuides(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 6.dp.toPx()
        val cornerSize = 30.dp.toPx()
        val color = Color.White.copy(alpha = 0.8f)

        drawPath(
            path = Path().apply {
                moveTo(0f, cornerSize)
                lineTo(0f, 0f)
                lineTo(cornerSize, 0f)
            },
            color = color,
            style = Stroke(width = strokeWidth)
        )

        drawPath(
            path = Path().apply {
                moveTo(size.width - cornerSize, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, cornerSize)
            },
            color = color,
            style = Stroke(width = strokeWidth)
        )

        drawPath(
            path = Path().apply {
                moveTo(0f, size.height - cornerSize)
                lineTo(0f, size.height)
                lineTo(cornerSize, size.height)
            },
            color = color,
            style = Stroke(width = strokeWidth)
        )

        drawPath(
            path = Path().apply {
                moveTo(size.width - cornerSize, size.height)
                lineTo(size.width, size.height)
                lineTo(size.width, size.height - cornerSize)
            },
            color = color,
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun CameraTipBox(modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth(0.95f)
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TIP",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "대상을 단색 배경(흰 이불, 바닥)에 놓고 정면에서 촬영해 주세요!",
                color = Color.White,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "그림자가 심하게 지지 않는 밝은 곳이 좋아요.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CameraPreview(imageCapture: ImageCapture) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Binding failed", e)
                }
            }, executor)
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
