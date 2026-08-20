package com.example.team4uu.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.team4uu.R
import com.example.team4uu.data.ChildProfile
import com.example.team4uu.data.ChildProfileStore
import com.example.team4uu.data.Friend
import com.example.team4uu.data.remote.GoalRepository
import com.example.team4uu.data.remote.TokenManager
import com.example.team4uu.ui.components.ChildNameEditDialog
import com.example.team4uu.ui.components.DEFAULT_MISSION_TAGS
import com.example.team4uu.ui.components.DollNameDialog
import com.example.team4uu.ui.components.FeedMissionDialog
import com.example.team4uu.ui.components.INTEREST_OPTIONS
import com.example.team4uu.ui.components.InterestEditDialog
import com.example.team4uu.ui.components.SettingsMenuDialog
import com.example.team4uu.ui.components.interestKeyword
import com.example.team4uu.ui.screens.CameraScreen
import com.example.team4uu.ui.screens.EmptyFriendContent
import com.example.team4uu.ui.screens.FeedingScreen
import com.example.team4uu.ui.screens.LoginScreen
import com.example.team4uu.ui.screens.MainHomeContent
import com.example.team4uu.ui.screens.MissionRoadmapScreen
import com.example.team4uu.ui.screens.SignUpScreen
import com.example.team4uu.viewmodel.AuthViewModel
import com.example.team4uu.viewmodel.FriendViewModel
import kotlinx.coroutines.launch

private enum class AuthStep {
    LOGIN,
    SIGNUP
}

// 앱 전체 라우팅: 로그인 여부 -> 카메라/테스트 화면 여부 -> 친구 유무에 따라 어떤 화면을 보여줄지 결정.
@Composable // 화면을 그리는 함수라는 의미의 어노테이션
fun MainScreen(friendViewModel: FriendViewModel = viewModel()) {
    var showCamera by remember { mutableStateOf(false) } // ShowCamera 값이 바뀌면 화면을 다시만드는 상태 객체
    var showMissionRoadmap by remember { mutableStateOf(false) }
    // "밥 먹기"를 누르면 이 친구를 들고 카메라 오버레이(FeedingScreen)로 이동. null이 아니면 그 화면을 보여줌.
    var feedingFriend by remember { mutableStateOf<Friend?>(null) }
    // "밥 먹기"를 누르면 먼저 오늘의 미션 선택 팝업을 띄움. 확인을 누르면 feedingFriend로 넘어가서 카메라가 열림.
    var missionSelectionFriend by remember { mutableStateOf<Friend?>(null) }
    // 촬영은 끝났고 이름을 기다리는 사진. null 이 아니면 이름 입력 팝업이 뜬다.
    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }
    // TODO: 실제로는 자주 쓰는 태그를 백엔드/DB에 저장해야 함. 아직 그게 없어서 세션 동안만 유지되는 상태로 관리.
    var savedMissionTags by remember { mutableStateOf(DEFAULT_MISSION_TAGS) }
    // 밥 먹기 시작 시 고른 오늘의 목표(최대 3개). "끝내기"를 누르면 그대로 다시 보여줌.
    var currentGoals by remember { mutableStateOf(listOf<String>()) }
    // 미션 로드맵 단계별 별 진행도. 1단계는 별 2개가 이미 채워진 상태로 시작함.
    var missionLevels by remember { mutableStateOf(initialMissionLevels()) }
    val goalRepository = remember { GoalRepository() }
    val coroutineScope = rememberCoroutineScope()
    var isLoggedIn by remember {
        mutableStateOf(false)
    } // TODO: 백엔드 인증 연동 후 실제 로그인 상태(토큰 존재 여부 등)로 교체
    var authStep by remember { mutableStateOf(AuthStep.LOGIN) }
    val context = LocalContext.current // 현재 앱의 컨텍스트를 갖고 옴(권환 확인 시스템 기능을 쓸 때 필요)
    // 회원가입에서 받은 아이 이름·나이를 담아둔다. 대화(WS /doll/talk)를 열 때 쿼리로 붙이면
    // 인형이 아이 이름을 불러준다. remember 로 감싸야 리컴포지션마다 다시 만들지 않는다.
    val childProfileStore = remember(context) { ChildProfileStore(context) }
    val friends by friendViewModel.friends.collectAsState() // Room DB에 저장된 친구 목록(실시간 반영)
    val isCreatingFriend by
            friendViewModel.isCreatingFriend.collectAsState() // 사진을 서버로 보내 인형으로 변환하는 중인지
    val authViewModel: AuthViewModel = viewModel()

    // 홈 화면 좌측 상단 톱니바퀴 -> 설정 메뉴 -> 관심사 변경/자녀 이름 변경 팝업으로 이어지는 흐름의 상태
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showInterestEditDialog by remember { mutableStateOf(false) }
    var showChildNameEditDialog by remember { mutableStateOf(false) }

    // 카메라 권한 요청을 띄우는 팝업
    val permissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted -> // 허용/거부를 누른 뒤 실행되는 콜백
                if (isGranted) {
                    showCamera = true
                }
            }

    fun requestCameraOrOpen() { // 친구 만들기 버튼(온보딩/메인 홈 공통)을 눌렀을 때
        // 카메라 권한이 있는지 확인
        val permissionCheckResult =
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        // 권한이 있을 때, 바로 카메라 화면을 띄움
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            showCamera = true
        } else { // 권한이 없으면 권한 요청 팝업을 띄움
            permissionLauncher.launch(
                    Manifest.permission.CAMERA
            ) // 위에서 생성한 런처 객체 (여기서는 카메라 권한을 요청으로 넣음)
        }
    }

    if (!isLoggedIn) { // 로그인 전에는 항상 로그인/가입 화면부터 보여줌
        // 로그인 -> 가입은 오른쪽에서 왼쪽으로, 가입 -> 로그인(뒤로가기)은 반대로 슬라이드
        AnimatedContent(
                targetState = authStep,
                transitionSpec = {
                    if (targetState == AuthStep.SIGNUP) {
                        (slideInHorizontally(initialOffsetX = { it }) + fadeIn()).togetherWith(
                                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                        )
                    } else {
                        (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()).togetherWith(
                                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        )
                    }
                },
                label = "authStep"
        ) { step ->
            when (step) {
                AuthStep.LOGIN ->
                        LoginScreen(
                                onLoginClick = { username ->
                                    // 계정별로 친구 목록을 분리해서 보여주기 위해, 로그인한 아이디를 현재 사용자로 설정.
                                    // 이 계정에 친구가 하나도 없으면(처음 로그인) friends가 빈 목록이라
                                    // 아래 Scaffold 분기에서 자동으로 EmptyFriendContent(온보딩)가 뜸.
                                    friendViewModel.setCurrentUser(username)
                                    isLoggedIn = true
                                },
                                onSignUpClick = { authStep = AuthStep.SIGNUP }
                        )
                AuthStep.SIGNUP ->
                        SignUpScreen(
                                onSignUpComplete = { authStep = AuthStep.LOGIN },
                                onBackToLogin = { authStep = AuthStep.LOGIN }
                        )
            }
        }
    } else if (showCamera) {
        CameraScreen(
                onClose = { showCamera = false }, // 카메라 화면을 띄우지만 닫기를 누르면 showCamera를 false
                onPhotoCaptured = { imagePath ->
                    showCamera = false
                    // 여기서 바로 등록하지 않는다. 이름을 먼저 물어본 뒤 등록한다
                    // 사용자가 확인하기 전에 시작하면 안 된다.
                    pendingPhotoPath = imagePath
                }
        )
    } else if (showMissionRoadmap) {
        MissionRoadmapScreen(levels = missionLevels, onClose = { showMissionRoadmap = false })
    } else if (feedingFriend != null) {
        FeedingScreen(
                friend = feedingFriend,
                goals = currentGoals,
                onClose = { feedingFriend = null },
                onFinishSession = { achievedGoals ->
                    // 3개 중 2개 이상 달성했으면 미션 로드맵에서 아직 다 못 채운 첫 단계의
                    // 왼쪽 별부터 하나 채운다.
                    if (achievedGoals.size >= 2) {
                        val idx = missionLevels.indexOfFirst { it.starsEarned < STARS_TO_UNLOCK_LEVEL }
                        if (idx != -1) {
                            missionLevels = missionLevels.toMutableList().apply {
                                this[idx] = this[idx].copy(starsEarned = this[idx].starsEarned + 1)
                            }
                        }
                    }
                    feedingFriend = null
                }
        )
    } else { // 카메라를 보여줄 상황이 아니면 메인 화면을 띄움
        Scaffold( // 화면의 기본 뼈대를 잡아주는 compose의 부품임
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent // 상태 바 영역까지 배경 이미지가 비치도록 흰 배경을 없앰
                // 온보딩(친구 없음)에는 하단바가 없고, 친구가 있으면 MainHomeContent가 밥먹기/놀기 버튼까지
                // 접었다 펼 수 있는 옵션 패널로 직접 그리므로 별도의 bottomBar는 쓰지 않음
                ) { _ ->
            // Scaffold의 innerPadding(상태바/네비게이션 바 여백)을 그대로 적용하면 그 틈으로 뒤에 깔린
            // bg_main_clouds(로그인 화면 배경)가 위아래로 비쳐 보임. 다른 화면들처럼 배경은 항상 화면 전체를
            // 꽉 채우고, 상태바를 피해야 하는 요소(닫기 버튼 등)만 개별적으로 statusBarsPadding()을 씀.
            Box(modifier = Modifier.fillMaxSize()) { // 메인 화면의 본문 영역
                Image(
                        painter = painterResource(id = R.drawable.bg_main_clouds),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    // 친구 목록 조회 결과로 분기: 0마리면 온보딩, 1마리 이상이면 메인 홈
                    if (friends.isEmpty()) {
                        EmptyFriendContent(onStartClick = ::requestCameraOrOpen)
                    } else {
                        MainHomeContent(
                                friends = friends,
                                onAddFriendClick = ::requestCameraOrOpen,
                                onMissionRoadmapClick = { showMissionRoadmap = true },
                                onFeedClick = { friend -> missionSelectionFriend = friend },
                                onSettingsClick = { showSettingsMenu = true }
                        )
                    }
                }
            }
        }
    }

    // "밥 먹기"를 누르면 카메라(FeedingScreen)로 바로 가지 않고, 오늘의 미션을 먼저 고르는 팝업을 띄움.
    // 확인을 누르면 그 친구를 데리고 FeedingScreen으로 넘어감.
    // 촬영 직후 인형 이름 짓기. 저장을 누르면 그때 서버로 보낸다.
    pendingPhotoPath?.let { photoPath ->
        DollNameDialog(
                // 취소하면 등록하지 않는다. 찍은 사진은 버려진다.
                onDismiss = { pendingPhotoPath = null },
                onConfirm = { name ->
                    pendingPhotoPath = null
                    friendViewModel.createFriendFromPhoto(
                            name = name,
                            imagePath = photoPath,
                            onError = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                    )
                }
        )
    }

    val missionFriend = missionSelectionFriend
    if (missionFriend != null) {
        FeedMissionDialog(
                savedTags = savedMissionTags,
                onDeleteTag = { tag -> savedMissionTags = savedMissionTags - tag },
                onSaveTagPermanently = { tag ->
                    if (tag !in savedMissionTags) savedMissionTags = savedMissionTags + tag
                },
                onDismiss = { missionSelectionFriend = null },
                onConfirm = { goals ->
                    currentGoals = goals
                    coroutineScope.launch {
                        try {
                            goalRepository.postGoalTags(goals)
                        } catch (e: Exception) {
                            // 목표 기록은 서버가 참고만 하는 값이라, 실패해도 밥 먹기 흐름 자체는 막지 않음.
                            Toast.makeText(context, "목표 전송에 실패했어요.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    feedingFriend = missionFriend
                    missionSelectionFriend = null
                }
        )
    }

    // 홈 화면 좌측 상단 톱니바퀴를 누르면 뜨는 설정 메뉴.
    if (showSettingsMenu) {
        SettingsMenuDialog(
                onDismiss = { showSettingsMenu = false },
                onEditNameClick = {
                    showSettingsMenu = false
                    showChildNameEditDialog = true
                },
                onEditInterestsClick = {
                    showSettingsMenu = false
                    showInterestEditDialog = true
                },
                onLogoutClick = {
                    showSettingsMenu = false
                    TokenManager.clear()
                    friendViewModel.logout()
                    isLoggedIn = false
                }
        )
    }

    if (showInterestEditDialog) {
        InterestEditDialog(
                // TokenManager.keywords는 서버로 보낸 순수 텍스트("공룡")라서, INTEREST_OPTIONS의
                // 이모지 붙은 표기("🦕 공룡")로 다시 매칭해서 미리 체크된 상태로 보여줌
                initiallySelected =
                        INTEREST_OPTIONS.filter { interestKeyword(it) in TokenManager.keywords },
                onDismiss = { showInterestEditDialog = false },
                onConfirm = { selected ->
                    authViewModel.updateKeyword(
                            keywords = selected.map { interestKeyword(it) },
                            onSuccess = {
                                showInterestEditDialog = false
                                Toast.makeText(context, "관심사가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                    )
                }
        )
    }

    if (showChildNameEditDialog) {
        ChildNameEditDialog(
                initialName = childProfileStore.load()?.name.orEmpty(),
                onDismiss = { showChildNameEditDialog = false },
                onConfirm = { newName ->
                    authViewModel.updateName(
                            name = newName,
                            onSuccess = {
                                // 나이·관심사는 그대로 두고 이름만 바꿔서 로컬(ChildProfileStore)에도 반영.
                                // TalkViewModel이 대화 연결 시 이 값을 읽어 인형이 부르는 이름으로 쓴다.
                                val existing = childProfileStore.load()
                                childProfileStore.save(
                                        ChildProfile(
                                                name = newName,
                                                age = existing?.age ?: ChildProfileStore.DEFAULT_AGE,
                                                interests = existing?.interests ?: emptyList()
                                        )
                                )
                                showChildNameEditDialog = false
                                Toast.makeText(context, "자녀 이름이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                    )
                }
        )
    }

    // 촬영한 사진을 서버로 보내 인형으로 변환하는 동안 뜨는 전체 화면 로딩. 몇 초 걸릴 수 있어서
    // 그동안 다른 화면(온보딩/홈)이 어색하게 비어 보이지 않도록 위에 덮어씀.
    if (isCreatingFriend) {
        Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "인형을 만들고 있어요...", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
