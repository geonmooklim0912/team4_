package com.example.team4uu.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.team4uu.data.Friend
import com.example.team4uu.ui.theme.FriendCardTan
import com.example.team4uu.ui.theme.FriendPink

// 설정 > "친구 목록 관리" 팝업. 등록된 친구를 썸네일과 함께 나열하고, 한 명씩
// 이름 변경/삭제를 할 수 있게 해준다. 친구는 서버에 등록되지 않고 로컬(Room)에만
// 있어서(FriendViewModel 참고) 여기서도 네트워크 요청 없이 로컬 CRUD만 한다.
@Composable
fun FriendManageDialog(
    friends: List<Friend>,
    onDismiss: () -> Unit,
    onRenameFriend: (Friend, String) -> Unit,
    onDeleteFriend: (Friend) -> Unit
) {
    var renamingFriend by remember { mutableStateOf<Friend?>(null) }
    var deletingFriend by remember { mutableStateOf<Friend?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .width(280.dp)
                    .heightIn(max = 420.dp)
            ) {
                Text(text = "친구 목록 관리", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))

                if (friends.isEmpty()) {
                    Text(text = "등록된 친구가 없어요.", fontSize = 13.sp, color = Color.DarkGray)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(friends, key = { it.id }) { friend ->
                            FriendManageRow(
                                friend = friend,
                                onRenameClick = { renamingFriend = friend },
                                onDeleteClick = { deletingFriend = friend }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(text = "닫기", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    renamingFriend?.let { friend ->
        FriendRenameDialog(
            initialName = friend.name,
            onDismiss = { renamingFriend = null },
            onConfirm = { newName ->
                onRenameFriend(friend, newName)
                renamingFriend = null
            }
        )
    }

    deletingFriend?.let { friend ->
        FriendDeleteConfirmDialog(
            friendName = friend.name,
            onDismiss = { deletingFriend = null },
            onConfirm = {
                onDeleteFriend(friend)
                deletingFriend = null
            }
        )
    }
}

@Composable
private fun FriendManageRow(friend: Friend, onRenameClick: () -> Unit, onDeleteClick: () -> Unit) {
    val characterPainter = rememberFriendCharacterPainter(friend)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(FriendCardTan),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = characterPainter,
                contentDescription = friend.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = friend.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRenameClick, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "이름 변경", tint = Color.DarkGray)
        }
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = FriendPink)
        }
    }
}

// 이름 변경 입력창. ChildNameEditDialog/DollNameDialog와 같은 톤·검증 규칙(한글·영문·숫자,
// 10자 이하 — DollNameDialog가 처음 이름 지을 때 쓰는 것과 동일한 기준)을 그대로 따른다.
@Composable
private fun FriendRenameDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    val trimmed = name.trim()
    val error = when {
        trimmed.isEmpty() -> null
        trimmed.length > FRIEND_NAME_MAX_LEN -> "이름은 ${FRIEND_NAME_MAX_LEN}자까지 지을 수 있어요."
        !FRIEND_NAME_PATTERN.matches(trimmed) -> "한글, 영문, 숫자로 지어주세요."
        else -> null
    }
    val canConfirm = trimmed.isNotEmpty() && trimmed != initialName && error == null

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "친구 이름 변경", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    isError = trimmed.isNotEmpty() && error != null,
                    placeholder = { Text("이름 입력", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black,
                        cursorColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (trimmed.isNotEmpty() && error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = error, color = FriendPink, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "취소", color = Color.DarkGray)
                    }
                    Button(
                        onClick = { onConfirm(trimmed) },
                        enabled = canConfirm,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FriendPink)
                    ) {
                        Text(text = "저장", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 삭제 확인 팝업. 삭제는 되돌릴 수 없어서(로컬 DB에서 바로 지워짐) 한 번 더 확인받는다.
@Composable
private fun FriendDeleteConfirmDialog(friendName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "\"$friendName\"을(를) 삭제할까요?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "삭제하면 되돌릴 수 없어요.", fontSize = 13.sp, color = Color.DarkGray)

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "취소", color = Color.DarkGray)
                    }
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FriendPink)
                    ) {
                        Text(text = "삭제", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 서버(server/profile.py)와 같은 기준. DollNameDialog에서 처음 이름 지을 때 쓰는 규칙과 동일하게 맞춘다.
private const val FRIEND_NAME_MAX_LEN = 10
private val FRIEND_NAME_PATTERN = Regex("^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ]+$")
