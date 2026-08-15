package com.example.team4uu.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team4uu.data.Friend
import com.example.team4uu.ui.theme.FriendCardTan
import com.example.team4uu.ui.theme.FriendPink

// 새 친구 등록하기/친구 목록/밥먹기/놀기를 담은 옵션 패널.
// 화살표 버튼(또는 위아래 스와이프)으로 접었다 펼 수 있고, 접으면 그만큼 위 방 화면이 자연스럽게 넓어짐.
@Composable
fun HomeOptionsPanel(
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
