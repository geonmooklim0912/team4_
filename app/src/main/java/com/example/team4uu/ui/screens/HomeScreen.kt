package com.example.team4uu.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.team4uu.R
import com.example.team4uu.data.Friend
import com.example.team4uu.ui.CURRENT_MISSION_STAGE
import com.example.team4uu.ui.components.HomeOptionsPanel
import com.example.team4uu.ui.components.livingCharacterEffect
import com.example.team4uu.ui.components.rememberFriendCharacterPainter
import com.example.team4uu.ui.components.rememberTalkingCharacterPainter
import com.example.team4uu.data.remote.TalkSocket
import com.example.team4uu.data.remote.TokenManager
import com.example.team4uu.viewmodel.TalkViewModel
import com.example.team4uu.ui.theme.FriendPink
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import kotlin.math.roundToInt

// 옵션 패널의 둥근 위쪽 모서리(+ 패널이 배경 경계선 위로 살짝 얹히는 부분) 뒤로 방 배경이 끊기지 않게 보이도록,
// 배경 이미지를 패널 쪽으로 이 값만큼 더 늘림. HomeOptionsPanel의 모서리 반경(36.dp)보다 넉넉해야 함.
private val ROOM_BACKGROUND_PEEK = 56.dp

// 방 배경을 오른쪽에서 왼쪽으로 스와이프하면 순서대로 넘어가는 배경 목록
private val ROOM_BACKGROUNDS = listOf(
    R.drawable.bg_room_1,
    R.drawable.bg_room_2,
    R.drawable.bg_room_3,
    R.drawable.bg_room_4,
    R.drawable.bg_room_5
)

// 각 배경을 잠금 해제하려면 미션 로드맵에서 도달해야 하는 스테이지. 0번째(기본 배경)는 항상 해제 상태라 0.
private val ROOM_BACKGROUND_UNLOCK_STAGE = listOf(0, 1, 3, 5, 6)

// 친구가 1마리 이상일 때 보이는 메인 홈: 위쪽은 현재 선택된 친구가 서 있는 방, 아래쪽은
// 화살표/스와이프로 접었다 펼 수 있는 새 친구 등록하기 + 친구 목록 + 밥먹기/놀기 옵션 패널.
@Composable
fun MainHomeContent(
    friends: List<Friend>,
    onAddFriendClick: () -> Unit,
    onMissionRoadmapClick: () -> Unit,
    onFeedClick: (Friend) -> Unit,
    onSettingsClick: () -> Unit
) {
    // FriendDao가 createdAt DESC로 정렬해서 주므로 **맨 앞이 가장 최근에 등록된 친구**다.
    // (lastOrNull()은 가장 오래된 친구라서 새로 등록한 인형이 선택되지 않았다)
    var selectedFriendId by remember { mutableStateOf(friends.firstOrNull()?.id) }
    // 새 친구가 등록됐는지 판단하려고 직전의 "가장 최근 친구"를 기억해 둔다.
    var newestFriendId by remember { mutableStateOf(friends.firstOrNull()?.id) }
    // 목록이 바뀌어도(친구 추가 등) 선택이 항상 유효하도록 유지하고, 새로 등록된 친구를 자동 선택
    LaunchedEffect(friends) {
        val newest = friends.firstOrNull()?.id
        when {
            // 맨 앞이 바뀌었다 = 방금 새 친구가 등록됐다. 그 친구를 보여준다.
            newest != newestFriendId -> {
                newestFriendId = newest
                selectedFriendId = newest
            }
            // 선택했던 친구가 사라졌으면(삭제 등) 최신 친구로 되돌린다.
            friends.none { it.id == selectedFriendId } -> selectedFriendId = newest
        }
    }
    val selectedFriend = friends.find { it.id == selectedFriendId }

    var selectorExpanded by remember { mutableStateOf(true) }
    // 방 배경을 오른쪽에서 왼쪽으로 스와이프하면 ROOM_BACKGROUNDS 순서대로 넘어감.
    // 패널 바로 위 점 인디케이터에서도 현재 페이지를 읽어야 해서 Column 최상단에서 선언.
    val backgroundPagerState = rememberPagerState(pageCount = { ROOM_BACKGROUNDS.size })

    // 인형이 마지막으로 서 있던(=잠금 해제된) 배경 페이지. 잠긴 배경 쪽으로 스와이프해도 인형은 여기 머무르고,
    // 잠금 해제된 다른 배경에 도착하면 그제서야 인형도 그 배경으로 같이 넘어감.
    var lastUnlockedPage by remember { mutableIntStateOf(0) }
    LaunchedEffect(backgroundPagerState.currentPage) {
        val settledPage = backgroundPagerState.currentPage
        if (ROOM_BACKGROUND_UNLOCK_STAGE[settledPage] <= CURRENT_MISSION_STAGE) {
            lastUnlockedPage = settledPage
        }
    }

    val context = LocalContext.current

    // TODO: 캐릭터가 실제로 말하게 되면(TTS) 이 값으로 소리를 켜고 끔. 지금은 아직 캐릭터가 말을 안 해서
    // 상태만 토글되는 껍데기 버튼.
    var isMuted by remember { mutableStateOf(false) }

    // 마이크로 사용자 말 받아쓰기(SpeechRecognizer) 관련 상태
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasAudioPermission = granted }

    // 인형과의 실시간 음성 대화(WS /doll/talk).
    //
    // 예전에는 여기서 안드로이드 SpeechRecognizer 로 받아쓰기만 하고 Toast 로 띄웠다
    // (팀원 주석: "인식된 문장을 캐릭터 반응에 연결해야 함 — 아직 그 기능이 없어서").
    // 그 자리에 실제 대화를 붙였다. 서버가 음성을 직접 처리하므로 별도 STT 가 필요 없다.
    val talkViewModel: TalkViewModel = viewModel()
    val talkState by talkViewModel.state.collectAsState()
    val talkTranscript by talkViewModel.transcript.collectAsState()
    val mouth by talkViewModel.mouth.collectAsState()

    // 버튼 색을 가르는 값. 기존 isListening 을 그대로 대체한다.
    val isListening = talkState is TalkViewModel.TalkState.Listening

    // 인형 말풍선을 지금 띄울지. 말을 마치면 잠시 뒤 사라진다 — 계속 떠 있으면
    // 말풍선이 아니라 안내판처럼 보이고, 대화를 끝낸 뒤에도 마지막 대사가 남는다.
    var speechVisible by remember { mutableStateOf(false) }
    LaunchedEffect(talkTranscript, talkState) {
        when {
            // 아이가 말하는 중엔 인형 말풍선을 치운다.
            talkState is TalkViewModel.TalkState.Listening -> speechVisible = false
            talkTranscript.isBlank() -> speechVisible = false
            else -> {
                speechVisible = true
                // 말하는 도중에는 조각이 올 때마다 이 블록이 다시 시작돼 타이머가
                // 리셋된다. 그래서 턴이 끝난(Ready) 시점부터만 카운트가 돈다.
                if (talkState is TalkViewModel.TalkState.Ready) {
                    delay(SPEECH_BUBBLE_HOLD_MS)
                    speechVisible = false
                }
            }
        }
    }

    // 🔴 화면을 벗어나면 반드시 끊는다. 연결돼 있는 동안 계속 과금되기 때문이다.
    DisposableEffect(Unit) {
        onDispose { talkViewModel.stop() }
    }

    fun onTalkButtonClick() {
        if (!hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        when (talkState) {
            // 아직 연결 전. 🔴 여기서부터 크레딧이 나간다 — 화면 진입이 아니라
            // 버튼을 눌렀을 때만 연결하는 이유다.
            is TalkViewModel.TalkState.Idle ->
                talkViewModel.start(
                    token = TokenManager.token.orEmpty(),
                    dollName = selectedFriend?.name,
                    // 여기는 홈(놀기)이다. 밥 이야기를 먼저 꺼내지 않게 한다.
                    mode = TalkSocket.MODE_PLAY
                )

            // 말하는 중. 다시 누르면 즉시 끝낸다 — 소음 때문에 자동 종료가
            // 안 걸릴 때를 위한 수동 탈출구다.
            is TalkViewModel.TalkState.Listening -> talkViewModel.stopSpeaking()

            // 대기 중. 말하기 시작하고, 끝은 SpeechEndDetector 가 잡는다.
            is TalkViewModel.TalkState.Ready -> talkViewModel.startSpeaking()

            // 실패했으면 확인 처리만 하고 다음 탭에 다시 시작할 수 있게 한다.
            is TalkViewModel.TalkState.Failed -> talkViewModel.dismissError()

            // 연결 중이거나 인형이 말하는 중 — 끼어들지 않는다.
            else -> Unit
        }
    }

    // 홈 배경을 방 이미지 대신 후면 카메라 실시간 화면으로 전환하는 토글
    var isCameraBackgroundActive by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) isCameraBackgroundActive = true
    }

    fun onCameraToggleClick() {
        when {
            isCameraBackgroundActive -> isCameraBackgroundActive = false
            hasCameraPermission -> isCameraBackgroundActive = true
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            if (isCameraBackgroundActive) {
                // 오른쪽 카메라 버튼을 누르면 방 배경 대신 후면 카메라 실시간 화면을 보여줌
                val cameraImageCapture = remember { ImageCapture.Builder().build() }
                CameraPreview(imageCapture = cameraImageCapture)
            } else {
            HorizontalPager(
                state = backgroundPagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val requiredStage = ROOM_BACKGROUND_UNLOCK_STAGE[page]
                val isLocked = requiredStage > CURRENT_MISSION_STAGE

                Box(modifier = Modifier.fillMaxSize()) {
                    // 아래로 ROOM_BACKGROUND_PEEK만큼 더 키워서, 옵션 패널의 둥근 모서리 뒤로 살짝 넘쳐 보이게 함
                    // (Box는 자식을 자기 경계에 맞춰 자르지 않으므로, 패널이 그 위에 그려지면서 둥근 모서리 부분만 배경이 드러남)
                    val backgroundHeight = screenHeight + ROOM_BACKGROUND_PEEK
                    Image(
                        painter = painterResource(id = ROOM_BACKGROUNDS[page]),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(backgroundHeight),
                        contentScale = ContentScale.Crop,
                        // 잠긴 배경은 채도를 확 낮춰서(흑백에 가깝게) 아직 못 쓰는 배경이라는 걸 알려줌
                        colorFilter = if (isLocked) {
                            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.15f) })
                        } else {
                            null
                        }
                    )

                    if (isLocked) {
                        // 채도 낮추기만으로는 부족해서, 반투명 검은 막을 한 겹 더 깔아 명도도 어둡게 함
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(backgroundHeight)
                                .background(Color.Black.copy(alpha = 0.5f))
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_lock),
                                contentDescription = "잠김",
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "미션 로드맵 ${requiredStage}스테이지 도달 시 잠금 해제",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
            }

            // 인형이 말하는 동안 입 모양 3장이 교체된다(초당 최대 30회).
            // 대화 중이 아니면 mouth 가 CLOSED 라 기존과 같은 그림이다.
            val characterPainter = rememberTalkingCharacterPainter(selectedFriend, mouth)

            val density = LocalDensity.current
            val pageWidthPx = with(density) { screenWidth.toPx() }

            Image(
                painter = characterPainter,
                contentDescription = selectedFriend?.name ?: "캐릭터 미리보기",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(screenWidth * 0.62f)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = screenHeight * 0.135f)
                    // 잠긴 배경 쪽으로 스와이프하면 인형은 따라가지 않고 마지막 잠금 해제 배경(lastUnlockedPage)에
                    // 머물러 있는 것처럼 보이도록, 페이저의 실시간 스크롤 위치를 기준으로 반대 방향 오프셋을 줌
                    .offset {
                        val viewportPagePosition =
                            backgroundPagerState.currentPage + backgroundPagerState.currentPageOffsetFraction
                        val offsetPages = lastUnlockedPage - viewportPagePosition
                        IntOffset(x = (offsetPages * pageWidthPx).roundToInt(), y = 0)
                    }
                    .livingCharacterEffect()
            )

            // 인형이 한 말을 머리 위 말풍선으로. 할 말이 없으면 아예 뜨지 않는다.
            //
            // 캐릭터가 화면 폭 62% / 하단 여백 13.5% 라 머리 끝이 대략 하단 52~53%
            // 지점이다. 54% 에 두면 머리 바로 위에 온다.
            val dollSpeech = dollSpeechText(talkTranscript)
            if (speechVisible && dollSpeech != null) {
                DollSpeechBubble(
                    text = dollSpeech,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = screenHeight * 0.54f, start = 28.dp, end = 28.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "설정", tint = Color.Black)
            }

            // 옵션 패널이 접혀 있을 때: 패널은 화면에서 완전히 빠지고, 방 위에 음소거/말하기/카메라 전환
            // 버튼 줄이 대신 떠 있음. 펼치기 화살표를 누르거나, 아래 점 인디케이터를 위로 쓸어올리면
            // 패널이 다시 펼쳐지면서 이 버튼 줄은 같이 사라짐.
            if (!selectorExpanded) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 84.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 지금 무엇을 하면 되는지 알려주는 자리. 인형이 한 말은 여기가
                    // 아니라 캐릭터 머리 위 말풍선에 뜬다 — 둘을 섞으면 안내인지
                    // 인형 대사인지 구분이 안 된다.
                    if (!isListening) {
                        VoiceHintBubble(text = talkHint(talkState))
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    VoiceControlRow(
                        isMuted = isMuted,
                        onMuteClick = { isMuted = !isMuted },
                        isListening = isListening,
                        onTalkClick = ::onTalkButtonClick,
                        isCameraBackgroundActive = isCameraBackgroundActive,
                        onCameraToggleClick = ::onCameraToggleClick
                    )
                }

                Surface(
                    onClick = { selectorExpanded = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .size(48.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "옵션 펼치기",
                            tint = Color.DarkGray
                        )
                    }
                }
            }
            BackgroundPageIndicator(
                pageCount = ROOM_BACKGROUNDS.size,
                currentPage = backgroundPagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    // 패널이 접혀 있을 때, 이 점들을 위로 쓸어올려도 패널이 다시 펼쳐짐(화살표와 동일 동작)
                    .pointerInput(selectorExpanded) {
                        if (!selectorExpanded) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -10f) selectorExpanded = true
                            }
                        }
                    }
            )
        }

        AnimatedVisibility(
            visible = selectorExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            HomeOptionsPanel(
                friends = friends,
                selectedFriendId = selectedFriendId,
                onCollapse = { selectorExpanded = false },
                onSelectFriend = { selectedFriendId = it.id },
                onAddFriendClick = onAddFriendClick,
                onMissionRoadmapClick = onMissionRoadmapClick,
                onFeedClick = { selectedFriend?.let(onFeedClick) }
            )
        }
    }
}

// 지금 몇 번째 방 배경인지 보여주는 점 인디케이터. 현재 페이지만 진하게(핑크) 표시.
@Composable
private fun BackgroundPageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { page ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(if (page == currentPage) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (page == currentPage) FriendPink else Color.White.copy(alpha = 0.6f))
            )
        }
    }
}

// 인형이 말을 마친 뒤 말풍선을 얼마나 더 띄워둘지.
// 인형 발화가 20자 안팎이라 읽는 데 2~3초면 충분하고, 여유를 뒀다.
private const val SPEECH_BUBBLE_HOLD_MS = 4_000L

// 하단 말풍선 — **지금 무엇을 하면 되는지** 알려준다. 인형 대사는 여기 넣지 않는다.
private fun talkHint(state: TalkViewModel.TalkState): String = when (state) {
    is TalkViewModel.TalkState.Failed -> state.error.userMessage
    is TalkViewModel.TalkState.Connecting -> "친구를 부르는 중이에요..."
    is TalkViewModel.TalkState.Ready,
    is TalkViewModel.TalkState.Speaking -> "버튼을 누르고 이야기해보세요!"
    else -> "아래 버튼을 클릭 후 자유롭게 말해보세요!"
}

// 상단 말풍선 — **인형이 한 말**만. 없으면 null 이고 말풍선 자체가 안 뜬다.
//
// 서버가 음성과 함께 텍스트로도 보내주기 때문에(output_audio_transcription)
// 추가 비용 없이 얻는다. 아이는 글을 못 읽지만 부모·시연자가 대화를 확인할 수 있다.
private fun dollSpeechText(transcript: String): String? = transcript.ifBlank { null }

// 캐릭터 머리 위에 뜨는 인형 대사 말풍선.
@Composable
private fun DollSpeechBubble(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White,
        // 왼쪽 아래만 각지게 해서 말풍선 꼬리처럼 보이게 한다.
        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

// 옵션 패널이 접혀 있을 때 말하기 버튼 위에 뜨는 안내 말풍선
@Composable
private fun VoiceHintBubble(text: String = "아래 버튼을 클릭 후 자유롭게 말해보세요!") {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

// 음소거(왼쪽) / 말하기(가운데, 마이크 입력 시작·중지) / 카메라 배경 전환(오른쪽) 버튼 줄
@Composable
private fun VoiceControlRow(
    isMuted: Boolean,
    onMuteClick: () -> Unit,
    isListening: Boolean,
    onTalkClick: () -> Unit,
    isCameraBackgroundActive: Boolean,
    onCameraToggleClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_mute),
            contentDescription = if (isMuted) "음소거 해제" else "음소거",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onMuteClick)
                .graphicsLayer { alpha = if (isMuted) 1f else 0.55f }
        )

        // 마이크로 듣고 있는 동안 살짝 두근거리는 느낌을 주는 펄스 애니메이션
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
            painter = painterResource(id = R.drawable.ic_talk_button),
            contentDescription = if (isListening) "말하기 중지" else "마이크로 말하기 시작",
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .clickable(onClick = onTalkClick)
                .graphicsLayer {
                    val scale = if (isListening) listeningScale else 1f
                    scaleX = scale
                    scaleY = scale
                }
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onCameraToggleClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_camera_toggle),
                contentDescription = if (isCameraBackgroundActive) "배경으로 돌아가기" else "카메라 배경으로 전환",
                modifier = Modifier.size(35.dp)
            )
        }
    }
}
