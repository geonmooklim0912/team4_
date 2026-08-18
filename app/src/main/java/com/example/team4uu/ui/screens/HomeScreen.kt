package com.example.team4uu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team4uu.R
import com.example.team4uu.data.Friend
import com.example.team4uu.ui.CURRENT_MISSION_STAGE
import com.example.team4uu.ui.components.HomeOptionsPanel
import com.example.team4uu.ui.components.livingCharacterEffect
import com.example.team4uu.ui.components.rememberFriendCharacterPainter
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

// 친구가 1마리 이상일 때 보이는 메인 홈. TestResultScreen과 완전히 같은 화면(FriendHomeScreen)을 씀 -
// 차이는 닫기(X) 버튼 유무뿐(메인 홈은 최상위 화면이라 닫을 필요가 없음).
@Composable
fun MainHomeContent(
    friends: List<Friend>,
    onAddFriendClick: () -> Unit,
    onMissionRoadmapClick: () -> Unit,
    onFeedClick: (Friend) -> Unit
) {
    FriendHomeScreen(
        friends = friends,
        onAddFriendClick = onAddFriendClick,
        onMissionRoadmapClick = onMissionRoadmapClick,
        onFeedClick = onFeedClick,
        onClose = null
    )
}

// 위쪽은 현재 선택된 친구가 서 있는 방, 아래쪽은 화살표/스와이프로 접었다 펼 수 있는
// 새 친구 등록하기 + 친구 목록 + 밥먹기/놀기 옵션 패널.
// TestResultScreen.kt에서도 그대로 재사용하므로 internal로 열어둠(같은 ui.screens 패키지 안 다른 파일에서 호출).
@Composable
internal fun FriendHomeScreen(
    friends: List<Friend>,
    onAddFriendClick: () -> Unit,
    onMissionRoadmapClick: () -> Unit,
    onFeedClick: (Friend) -> Unit,
    onClose: (() -> Unit)?
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

    var selectorExpanded by remember { mutableStateOf(false) }
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

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

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

            val characterPainter = rememberFriendCharacterPainter(selectedFriend)

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

            // 옵션 패널이 접혀 있을 때: 패널은 화면에서 완전히 빠지고, 방 위에 펼치기 화살표만 떠 있음
            if (!selectorExpanded) {
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
