package com.example.team4uu.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.team4uu.R
import com.example.team4uu.data.Friend
import com.example.team4uu.ui.components.HomeOptionsPanel
import com.example.team4uu.ui.components.livingCharacterEffect

// 친구가 1마리 이상일 때 보이는 메인 홈. TestResultScreen과 완전히 같은 화면(FriendHomeScreen)을 씀 -
// 차이는 닫기(X) 버튼 유무뿐(메인 홈은 최상위 화면이라 닫을 필요가 없음).
@Composable
fun MainHomeContent(friends: List<Friend>, onAddFriendClick: () -> Unit) {
    FriendHomeScreen(friends = friends, onAddFriendClick = onAddFriendClick, onClose = null)
}

// 위쪽은 현재 선택된 친구가 서 있는 방, 아래쪽은 화살표/스와이프로 접었다 펼 수 있는
// 새 친구 등록하기 + 친구 목록 + 밥먹기/놀기 옵션 패널.
// TestResultScreen.kt에서도 그대로 재사용하므로 internal로 열어둠(같은 ui.screens 패키지 안 다른 파일에서 호출).
@Composable
internal fun FriendHomeScreen(
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
