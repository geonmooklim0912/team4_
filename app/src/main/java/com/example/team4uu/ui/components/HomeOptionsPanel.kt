package com.example.team4uu.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiFlags
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.team4uu.R
import com.example.team4uu.data.Friend
import com.example.team4uu.ui.CURRENT_MISSION_STAGE
import com.example.team4uu.ui.theme.FriendCardTan
import com.example.team4uu.ui.theme.FriendIconPink
import com.example.team4uu.ui.theme.FriendPink
import kotlinx.coroutines.delay

// 패널(카드)이 방 배경 경계선보다 살짝 위로 얹혀 보이도록 카드 맨 위에 더해주는 여백.
// Column에 정상적으로 포함되는 실제 높이라서(오프셋이 아님) 레이아웃에 빈틈이 생기지 않음.
// 새 친구 등록하기/미션로드맵/친구 모아보기/밥먹기/놀기를 담은 옵션 패널.
// 이 컴포저블은 "펼쳐진 상태"만 그림 — 접혀 있을 때는 아예 화면에서 빠지고(HomeScreen.kt 쪽에서 처리),
// 그 자리에 위로 향하는 화살표 버튼만 방 위에 떠 있음. "놀기" 버튼을 누르거나 아래로 쓸어내리면
// onCollapse가 호출되어 패널이 사라짐.
@Composable
fun HomeOptionsPanel(
    friends: List<Friend>,
    selectedFriendId: Long?,
    onCollapse: () -> Unit,
    onSelectFriend: (Friend) -> Unit,
    onAddFriendClick: () -> Unit,
    onMissionRoadmapClick: () -> Unit,
    onFeedClick: () -> Unit
) {
    // 책 아이콘을 누르면 새 친구 등록 아이콘 줄 대신 등록된 친구 전체 목록이 오른쪽에서 스와이프되어 나타남
    var showFriendsGrid by remember { mutableStateOf(false) }

    Surface(
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 10f) onCollapse()
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = showFriendsGrid,
                transitionSpec = {
                    if (targetState) {
                        // 책 아이콘 -> 친구 목록: 오른쪽에서 왼쪽으로 스와이프
                        (slideInHorizontally(initialOffsetX = { it }) + fadeIn())
                            .togetherWith(slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                    } else {
                        // 뒤로가기: 왼쪽에서 오른쪽으로 되돌아옴
                        (slideInHorizontally(initialOffsetX = { -it }) + fadeIn())
                            .togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                    }
                },
                label = "topSection"
            ) { showGrid ->
                if (showGrid) {
                    // 책 아이콘을 누른 상태: 친구 목록만 보여줌(밥먹기/놀기 포함, 아이콘 줄은 함께 옆으로 밀려서 사라짐)
                    FriendsGrid(
                        friends = friends,
                        selectedFriendId = selectedFriendId,
                        onSelectFriend = onSelectFriend,
                        onBack = { showFriendsGrid = false }
                    )
                } else {
                    Column {
                        TopIconRow(
                            onMissionRoadmapClick = onMissionRoadmapClick,
                            onAddFriendClick = onAddFriendClick,
                            onShowFriendsGridClick = { showFriendsGrid = true }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FeedButton(modifier = Modifier.weight(1f), onClick = onFeedClick)
                            PlayButton(modifier = Modifier.weight(1f), onClick = onCollapse)
                        }
                    }
                }
            }
        }
    }
}

// 평소 상태: 미션로드맵(왼쪽) / 새 친구 등록하기(가운데) / 친구 모아보기(오른쪽)
@Composable
private fun TopIconRow(
    onMissionRoadmapClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    onShowFriendsGridClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // TODO: 세부 동작은 나중에 정함. 지금은 화면 이동만 연결.
        MissonRoadMapButton(
            onClick = onMissionRoadmapClick
        )
        AddFriendButton(onClick = onAddFriendClick)
        SeeFriendsButton(
            onClick = onShowFriendsGridClick
        )
    }
}

@Composable
private fun MissonRoadMapButton(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(80.dp).height(60.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Color(0xFFE91B71))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.EmojiFlags, contentDescription = "미션 로드맵", tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "미션 로드맵",
            fontSize = 11.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp)
        )
    }
}

@Composable
private fun SeeFriendsButton(onClick: () -> Unit){
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(80.dp).height(60.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Color(0xFFE91B71))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.book_icon),
                contentDescription = "미션 로드맵",
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "친구 목록",
            fontSize = 11.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp)
        )
    }
}
@Composable
private fun AddFriendButton(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(80.dp).height(60.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Color(0xFFE91B71))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "새 친구 등록", tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "새 친구 등록하기",
            fontSize = 11.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FeedButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = FriendCardTan,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.eat_icon),
                contentDescription = "미션 로드맵",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "밥 먹기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
private fun PlayButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = FriendCardTan,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.play_icon),
                contentDescription = "미션 로드맵",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "놀기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

// 친구 칸은 총 9개 고정. 앞 2칸은 기본으로 바로 쓸 수 있고, 3번째 칸부터는 미션 스테이지를 달성해야 열림.
private const val TOTAL_FRIEND_SLOTS = 9
private const val FREE_FRIEND_SLOTS = 2

// 잠긴 말풍선이 뜬 채로 유지되는 시간
private const val LOCKED_MESSAGE_DURATION_MS = 1500L

// slotIndex(0부터 시작)가 3번째 칸(index=2)부터 몇 스테이지에 열리는지 계산. 3번째=1, 4번째=2, 5번째=3 ...
private fun requiredStageForSlot(slotIndex: Int): Int = slotIndex - FREE_FRIEND_SLOTS + 1

// 책 아이콘을 누르면 나타나는, 친구 칸 9개 고정 그리드. 한 줄에 3칸씩, 넘치면 세로 스크롤.
@Composable
private fun FriendsGrid(
    friends: List<Friend>,
    selectedFriendId: Long?,
    onSelectFriend: (Friend) -> Unit,
    onBack: () -> Unit
) {
    // 잠긴 칸을 탭하면 그 칸 번호를 기억해서 말풍선을 띄우고, 일정 시간 후 자동으로 다시 null로 돌려 닫음
    var activeLockedSlot by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(activeLockedSlot) {
        if (activeLockedSlot != null) {
            delay(LOCKED_MESSAGE_DURATION_MS)
            activeLockedSlot = null
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.DarkGray
                )
            }
            Text(text = "등록한 친구 (${friends.size}/$TOTAL_FRIEND_SLOTS)", fontSize = 13.sp, color = Color.DarkGray)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp) // 여러 줄이면 이 높이를 넘는 부분은 세로 스크롤로 처리
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            (0 until TOTAL_FRIEND_SLOTS).chunked(3).forEach { rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowSlots.forEach { slotIndex ->
                        FriendGridSlot(
                            friend = friends.getOrNull(slotIndex),
                            selectedFriendId = selectedFriendId,
                            requiredStage = requiredStageForSlot(slotIndex),
                            isLocked = slotIndex >= FREE_FRIEND_SLOTS &&
                                requiredStageForSlot(slotIndex) > CURRENT_MISSION_STAGE,
                            showLockedMessage = activeLockedSlot == slotIndex,
                            onSelectFriend = onSelectFriend,
                            onLockedClick = { activeLockedSlot = slotIndex }
                        )
                    }
                }
            }
        }
    }
}

// 그리드 한 칸(잠김/빈 칸/등록된 친구 + 잠긴 칸 탭 시 뜨는 말풍선까지).
// AnimatedVisibility가 Row 안 Box에 바로 있으면 리시버가 모호해져서 별도 컴포저블로 분리함.
@Composable
private fun FriendGridSlot(
    friend: Friend?,
    selectedFriendId: Long?,
    requiredStage: Int,
    isLocked: Boolean,
    showLockedMessage: Boolean,
    onSelectFriend: (Friend) -> Unit,
    onLockedClick: () -> Unit
) {
    Box {
        if (isLocked) {
            LockedFriendSlot(onClick = onLockedClick)
        } else if (friend != null) {
            FriendThumbnail(
                friend = friend,
                selected = friend.id == selectedFriendId,
                onClick = { onSelectFriend(friend) }
            )
        } else {
            EmptyFriendSlot()
        }

        if (showLockedMessage) {
            // Popup은 별도 레이어에 그려져서 옆/위 칸의 Box나 부모의 스크롤 영역(clip)에 가리지 않고
            // 항상 다른 모든 요소들 위로 뜸. alignment는 이 Box(=지금 탭한 칸) 기준 위치.
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.TopCenter,
                offset = with(density) { IntOffset(x = 0, y = (-44).dp.roundToPx()) }
            ) {
                LockedFriendMessageBubble(requiredStage = requiredStage)
            }
        }
    }
}

// 아직 잠금 해제되지 않은 칸: 회색 원 + 자물쇠 이미지. 이름/사진은 안 보여줌.
@Composable
private fun LockedFriendSlot(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20))
                .background(Color(0xFFBDBDBD))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_lock),
                contentDescription = "잠김",
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        // 다른 칸들의 이름 텍스트와 세로 높이를 맞추기 위한 빈 자리
        Spacer(modifier = Modifier.height(14.dp))
    }
}

// 잠금 해제됐지만 아직 그 자리에 친구를 등록하지 않은 빈 칸
@Composable
private fun EmptyFriendSlot() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(FriendCardTan)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Spacer(modifier = Modifier.height(14.dp))
    }
}

// 잠긴 칸을 탭했을 때 뜨는 말풍선: "미션 로드맵 n스테이지 도달 시 잠금 해제"
@Composable
private fun LockedFriendMessageBubble(requiredStage: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.width(170.dp), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.bg_lock_message_bubble),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
        Text(
            text = "미션 로드맵 ${requiredStage}스테이지 도달 시 잠금 해제",
            fontSize = 10.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
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
                .clip(RoundedCornerShape(20))
                .background(FriendCardTan)
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

// 미리보기 확인용 임의의 친구 9명(사진 없이 이름만) — 실제 데이터는 카메라 촬영 후 Room DB에서 옴
private fun sampleFriends(count: Int): List<Friend> =
    (1..count).map { i -> Friend(id = i.toLong(), ownerUsername = "preview", name = "친구$i", imagePath = "") }

@Preview(showBackground = true)
@Composable
private fun FriendsGridPreview() {
    FriendsGrid(
        friends = sampleFriends(TOTAL_FRIEND_SLOTS),
        selectedFriendId = 1L,
        onSelectFriend = {},
        onBack = {}
    )
}
