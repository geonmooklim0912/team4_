package com.example.team4uu.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.team4uu.R
import com.example.team4uu.data.Friend
import com.example.team4uu.ui.components.DEFAULT_MISSION_TAGS
import com.example.team4uu.ui.components.FeedMissionDialog
import com.example.team4uu.ui.screens.CameraScreen
import com.example.team4uu.ui.screens.EmptyFriendContent
import com.example.team4uu.ui.screens.FeedingScreen
import com.example.team4uu.ui.screens.LoginScreen
import com.example.team4uu.ui.screens.MainHomeContent
import com.example.team4uu.ui.screens.MissionRoadmapScreen
import com.example.team4uu.ui.screens.SignUpScreen
import com.example.team4uu.ui.screens.TestResultScreen
import com.example.team4uu.viewmodel.FriendViewModel

private enum class AuthStep { LOGIN, SIGNUP }

// 앱 전체 라우팅: 로그인 여부 -> 카메라/테스트 화면 여부 -> 친구 유무에 따라 어떤 화면을 보여줄지 결정.
@Composable //화면을 그리는 함수라는 의미의 어노테이션
fun MainScreen(friendViewModel: FriendViewModel = viewModel()) {
    var showCamera by remember { mutableStateOf(false) } //ShowCamera 값이 바뀌면 화면을 다시만드는 상태 객체
    // TODO: 실제 촬영 -> 2D 변환 로직이 백엔드와 함께 구현되기 전까지, 결과 화면을 미리 볼 수 있는 테스트용 상태
    var showTestResult by remember { mutableStateOf(false) }
    var showMissionRoadmap by remember { mutableStateOf(false) }
    // "밥 먹기"를 누르면 이 친구를 들고 카메라 오버레이(FeedingScreen)로 이동. null이 아니면 그 화면을 보여줌.
    var feedingFriend by remember { mutableStateOf<Friend?>(null) }
    // "밥 먹기"를 누르면 먼저 오늘의 미션 선택 팝업을 띄움. 확인을 누르면 feedingFriend로 넘어가서 카메라가 열림.
    var missionSelectionFriend by remember { mutableStateOf<Friend?>(null) }
    // TODO: 실제로는 자주 쓰는 태그를 백엔드/DB에 저장해야 함. 아직 그게 없어서 세션 동안만 유지되는 상태로 관리.
    var savedMissionTags by remember { mutableStateOf(DEFAULT_MISSION_TAGS) }
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
        // 로그인 -> 가입은 오른쪽에서 왼쪽으로, 가입 -> 로그인(뒤로가기)은 반대로 슬라이드
        AnimatedContent(
            targetState = authStep,
            transitionSpec = {
                if (targetState == AuthStep.SIGNUP) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn())
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }) + fadeIn())
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                }
            },
            label = "authStep"
        ) { step ->
            when (step) {
                AuthStep.LOGIN -> LoginScreen(
                    onLoginClick = { isEmptyTestAccount ->
                        // TODO: 백엔드 로그인 API 연동 전까지는 입력값과 무관하게 로그인 성공으로 간주
                        // test2 계정은 EmptyFriendScreen(온보딩)을 바로 보고 싶을 때 쓰는 테스트용이라
                        // 로그인과 동시에 친구 목록을 비움.
                        if (isEmptyTestAccount) {
                            friendViewModel.clearAllFriends()
                        }
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
        }
    } else if (showCamera) {
        CameraScreen(
            onClose = { showCamera = false }, //카메라 화면을 띄우지만 닫기를 누르면 showCamera를 false
            onPhotoCaptured = { imagePath ->
                // TODO(F4): 등록 완료 후 이름을 입력받는 다이얼로그가 아직 없어서, 임시로 고정 이름("곰돌이")을
                // 사용함. 이름 입력 UI가 생기면 사용자가 입력한 값으로 교체.
                friendViewModel.addFriend(name = "곰돌이", imagePath = imagePath)
                showCamera = false
            }
        )
    } else if (showTestResult) {
        // TODO: 백엔드 연동 전까지 example2 목업 캐릭터로 결과 화면 연출을 미리 보여주는 테스트 화면
        TestResultScreen(
            friends = friends,
            onAddFriendClick = ::requestCameraOrOpen,
            onMissionRoadmapClick = { showMissionRoadmap = true },
            onFeedClick = { friend -> missionSelectionFriend = friend },
            onClose = { showTestResult = false }
        )
    } else if (showMissionRoadmap) {
        MissionRoadmapScreen(onClose = { showMissionRoadmap = false })
    } else if (feedingFriend != null) {
        FeedingScreen(friend = feedingFriend, onClose = { feedingFriend = null })
    } else { //카메라를 보여줄 상황이 아니면 메인 화면을 띄움
        Scaffold(//화면의 기본 뼈대를 잡아주는 compose의 부품임
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent // 상태 바 영역까지 배경 이미지가 비치도록 흰 배경을 없앰
            // 온보딩(친구 없음)에는 하단바가 없고, 친구가 있으면 MainHomeContent가 밥먹기/놀기 버튼까지
            // 접었다 펼 수 있는 옵션 패널로 직접 그리므로 별도의 bottomBar는 쓰지 않음
        ) { _ ->
            // Scaffold의 innerPadding(상태바/네비게이션 바 여백)을 그대로 적용하면 그 틈으로 뒤에 깔린
            // bg_main_clouds(로그인 화면 배경)가 위아래로 비쳐 보임. 다른 화면들처럼 배경은 항상 화면 전체를
            // 꽉 채우고, 상태바를 피해야 하는 요소(닫기 버튼 등)만 개별적으로 statusBarsPadding()을 씀.
            Box(modifier = Modifier.fillMaxSize()) { //메인 화면의 본문 영역
                Image(
                    painter = painterResource(id = R.drawable.bg_main_clouds),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    // 친구 목록 조회 결과로 분기: 0마리면 온보딩, 1마리 이상이면 메인 홈
                    if (friends.isEmpty()) {
                        EmptyFriendContent(
                            onStartClick = ::requestCameraOrOpen,
                            onTestClick = { showTestResult = true }
                        )
                    } else {
                        MainHomeContent(
                            friends = friends,
                            onAddFriendClick = ::requestCameraOrOpen,
                            onMissionRoadmapClick = { showMissionRoadmap = true },
                            onFeedClick = { friend -> missionSelectionFriend = friend }
                        )
                    }
                }
            }
        }
    }

    // "밥 먹기"를 누르면 카메라(FeedingScreen)로 바로 가지 않고, 오늘의 미션을 먼저 고르는 팝업을 띄움.
    // 확인을 누르면 그 친구를 데리고 FeedingScreen으로 넘어감.
    val missionFriend = missionSelectionFriend
    if (missionFriend != null) {
        FeedMissionDialog(
            savedTags = savedMissionTags,
            onDeleteTag = { tag -> savedMissionTags = savedMissionTags - tag },
            onSaveTagPermanently = { tag ->
                if (tag !in savedMissionTags) savedMissionTags = savedMissionTags + tag
            },
            onDismiss = { missionSelectionFriend = null },
            onConfirm = {
                feedingFriend = missionFriend
                missionSelectionFriend = null
            }
        )
    }
}
