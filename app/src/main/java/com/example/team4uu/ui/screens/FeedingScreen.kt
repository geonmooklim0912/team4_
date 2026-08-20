package com.example.team4uu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.team4uu.R
import com.example.team4uu.data.Friend
import com.example.team4uu.data.remote.TalkSocket
import com.example.team4uu.data.remote.TokenManager
import com.example.team4uu.ui.components.FeedingGoalCheckDialog
import com.example.team4uu.ui.components.livingCharacterEffect
import com.example.team4uu.ui.components.rememberTalkingCharacterPainter
import com.example.team4uu.viewmodel.TalkViewModel

// "밥 먹기" 버튼을 누르면 오는 화면. 진짜 AR(ARCore 바닥 인식 등)은 아직 아니고,
// 후면 카메라 실시간 화면 위에 지금 선택된 친구 캐릭터를 얹어서 보여주는 간단한 오버레이 방식.
// TODO: 나중에 ARCore/Sceneform으로 업그레이드하면 실제 공간에 캐릭터를 고정할 수 있음.
// + 인형과 실시간 음성 대화(WS /doll/talk).
// 서버는 앱이 붙는 즉시 Gemini Live연결을 열기 때문에 붙는 순간부터 크레딧이 나간다.
// 사용자가 "이야기하기"를 눌렀을 때만 연결하고, 화면을 벗어나면 반드시 끊는다.
@Composable
fun FeedingScreen(
        friend: Friend?,
        goals: List<String>,
        onClose: () -> Unit,
        onFinishSession: (achievedGoals: Set<String>) -> Unit,
        talkViewModel: TalkViewModel = viewModel()
) {
        val context = LocalContext.current
        // "끝내기"를 누르면 밥 먹기 시작할 때 골랐던 목표(goals)를 다시 보여주고 체크하게 함
        var showFinishDialog by remember { mutableStateOf(false) }
        var hasCameraPermission by remember {
                mutableStateOf(
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                )
        }
        val permissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                        granted ->
                        hasCameraPermission = granted
                }

        LaunchedEffect(Unit) {
                if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                }
        }

        // --- 대화 -----------------------------------------------------------------

        var hasMicPermission by remember {
                mutableStateOf(
                        ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                )
        }
        val micPermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                        granted ->
                        hasMicPermission = granted
                }

        // 로그인해서 받아둔 JWT. 서버 인증이 있어야 대화를 할 수 있음
        val token = TokenManager.token

        val talkState by talkViewModel.state.collectAsState()
        val transcript by talkViewModel.transcript.collectAsState()
        val mouth by talkViewModel.mouth.collectAsState()
        val contextLost by talkViewModel.contextLost.collectAsState()

        // 화면을 벗어나면 반드시 끊는다. ViewModel 은 Activity 에 붙어 있어서
        // 이 화면이 사라져도 onCleared() 가 불리지 않는다
        // 여기서 안 끊으면 Live 세션이 계속 실행되고,서버 동시 세션 한도(2)도 넘는다.
        DisposableEffect(Unit) { onDispose { talkViewModel.stop() } }

        // 홈 화면(HomeScreen.onTalkButtonClick)과 똑같은 버튼 하나로 시작 -> 듣기 -> 종료를
        // 전부 처리한다. 상태별로 버튼이 바뀌지 않는다.
        fun onTalkButtonClick() {
                if (!hasMicPermission) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        return
                }
                when (talkState) {
                        is TalkViewModel.TalkState.Idle ->
                                talkViewModel.start(
                                        token = token.orEmpty(),
                                        dollName = friend?.name,
                                        // 밥 먹기 화면. 음식 이야기를 꺼내도 되는 상황이다.
                                        mode = TalkSocket.MODE_MEAL,
                                        // 밥 먹기를 시작할 때 FeedMissionDialog 에서
                                        // 고른 오늘의 목표. 인형이 이걸 놀이처럼
                                        // 유도한다(서버 ai/dialog_test.goal_block).
                                        goals = goals
                                )
                        is TalkViewModel.TalkState.Listening -> talkViewModel.stopSpeaking()
                        is TalkViewModel.TalkState.Ready -> talkViewModel.startSpeaking()
                        is TalkViewModel.TalkState.Failed -> talkViewModel.dismissError()
                        else -> Unit
                }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                if (hasCameraPermission) {
                        // CameraPreview는 CameraScreen.kt에서도 쓰는 CameraX 후면 카메라 미리보기.
                        // 여기서는 사진을 찍을 게 아니라서 imageCapture는 그냥 바인딩용으로만 생성.
                        val imageCapture = remember { ImageCapture.Builder().build() }
                        CameraPreview(imageCapture = imageCapture)

                        if (friend != null) {
                                // 인형이 말하는 동안 입 모양 3장이 교체된다(초당 최대 30회).
                                val characterPainter =
                                        rememberTalkingCharacterPainter(friend, mouth)
                                Image(
                                        painter = characterPainter,
                                        contentDescription = friend.name,
                                        contentScale = ContentScale.Fit,
                                        modifier =
                                                Modifier.fillMaxWidth(0.60f)
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = 70.dp)
                                                        .livingCharacterEffect()
                                )
                        }
                } else {
                        Column(
                                modifier =
                                        Modifier.align(Alignment.Center)
                                                .padding(horizontal = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                Text(
                                        text = "밥 먹기를 하려면 카메라 권한이 필요해요",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                )
                        }
                }

                IconButton(
                        onClick = onClose,
                        modifier =
                                Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp)
                ) {
                        Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "닫기",
                                tint = Color.White
                        )
                }

                // 자막은 위쪽(인형과 겹칠 수 있음)
                // 서버가 인형 음성과 함께 텍스트도 보내줌(output_audio_transcription)
                TalkCaption(
                        state = talkState,
                        transcript = transcript,
                        contextLost = contextLost,
                        modifier =
                                Modifier.align(Alignment.TopCenter)
                                        .statusBarsPadding()
                                        .padding(top = 56.dp, start = 24.dp, end = 24.dp)
                )

                TalkControl(
                        isListening = talkState is TalkViewModel.TalkState.Listening,
                        onTalkClick = ::onTalkButtonClick,
                        modifier =
                                Modifier.align(Alignment.BottomEnd)
                                        .navigationBarsPadding()
                                        .padding(20.dp)
                )

                Button(
                        onClick = { showFinishDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91B71)),
                        shape = RoundedCornerShape(24.dp),
                        modifier =
                                Modifier.align(Alignment.BottomStart)
                                        .navigationBarsPadding()
                                        .padding(20.dp)
                ) {
                        Text(text = "식사 마치기", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
        }

        if (showFinishDialog) {
                FeedingGoalCheckDialog(
                        goals = goals,
                        onConfirm = { achievedGoals ->
                                showFinishDialog = false
                                onFinishSession(achievedGoals)
                        },
                        onDismiss = { showFinishDialog = false }
                )
        }
}

// 인형이 한 말과 상태문구
@Composable
private fun TalkCaption(
        state: TalkViewModel.TalkState,
        transcript: String,
        contextLost: Boolean,
        modifier: Modifier = Modifier
) {
        val message =
                when {
                        state is TalkViewModel.TalkState.Failed -> state.error.userMessage
                        contextLost -> "친구가 방금 한 이야기를 잊어버렸어요."
                        state is TalkViewModel.TalkState.Connecting -> "친구를 부르는 중이에요..."
                        transcript.isNotBlank() -> transcript
                        state is TalkViewModel.TalkState.Listening -> "듣고 있어요..."
                        else -> null
                }
                        ?: return

        Text(
                text = message,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier =
                        modifier.clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
}

// 홈 화면(HomeScreen.VoiceControlRow)과 똑같은 말하기 버튼 — 아이콘 하나로
// 시작 -> 듣기 -> 종료를 전부 처리한다. 상태별로 버튼 모양이 바뀌지 않는다.
@Composable
private fun TalkControl(
        isListening: Boolean,
        onTalkClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        // 듣고 있는 동안 살짝 두근거리는 느낌을 주는 펄스 애니메이션
        val infiniteTransition = rememberInfiniteTransition(label = "talkPulse")
        val listeningScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                ),
                label = "talkPulseScale"
        )
        Image(
                painter = painterResource(id = R.drawable.talk_icon),
                contentDescription = if (isListening) "말하기 중지" else "마이크로 말하기 시작",
                modifier =
                        modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onTalkClick)
                                .graphicsLayer {
                                        val scale = if (isListening) listeningScale else 1f
                                        scaleX = scale
                                        scaleY = scale
                                }
        )
}
