package com.example.team4uu.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.team4uu.data.Friend
import com.example.team4uu.ui.theme.FriendCardTan
import com.example.team4uu.ui.theme.FriendPink

// 친구 관리 버튼을 누르면 뜨는 팝업: 등록한 친구를 리스트로 보여주고, 각 칸에서
// 이름 바꾸기/삭제를 할 수 있음. 삭제는 확인 팝업을 한 번 더 거친 뒤에만 실제로 지워짐.
@Composable
fun FriendManageDialog(
    friends: List<Friend>,
    onRename: (Friend, String) -> Unit,
    onDelete: (Friend) -> Unit,
    onDismiss: () -> Unit
) {
    var renamingFriend by remember { mutableStateOf<Friend?>(null) }
    var deletingFriend by remember { mutableStateOf<Friend?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = FriendCardTan,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "친구 관리", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (friends.isEmpty()) {
                    Text(
                        text = "등록한 친구가 없어요.",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        friends.forEach { friend ->
                            FriendManageRow(
                                friend = friend,
                                onRenameClick = { renamingFriend = friend },
                                onDeleteClick = { deletingFriend = friend }
                            )
                        }
                    }
                }
            }
        }
    }

    renamingFriend?.let { friend ->
        RenameFriendDialog(
            friend = friend,
            onConfirm = { newName ->
                onRename(friend, newName)
                renamingFriend = null
            },
            onDismiss = { renamingFriend = null }
        )
    }

    deletingFriend?.let { friend ->
        DeleteFriendConfirmDialog(
            friend = friend,
            onConfirm = {
                onDelete(friend)
                deletingFriend = null
            },
            onDismiss = { deletingFriend = null }
        )
    }
}

@Composable
private fun FriendManageRow(friend: Friend, onRenameClick: () -> Unit, onDeleteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberFriendCharacterPainter(friend),
            contentDescription = friend.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = friend.name,
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRenameClick, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "이름 바꾸기", tint = FriendPink)
        }
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFE53935))
        }
    }
}

@Composable
private fun RenameFriendDialog(friend: Friend, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(friend.name) }
    val trimmedName = name.trim()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = FriendCardTan,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "이름 바꾸기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                AuthField(
                    label = "친구 이름",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "이름을 입력해 주세요",
                    modifier = Modifier.padding(top = 20.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(text = "취소", fontSize = 14.sp, color = Color.DarkGray)
                    }
                    Button(
                        onClick = { onConfirm(trimmedName) },
                        enabled = trimmedName.isNotEmpty() && trimmedName != friend.name,
                        colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(text = "확인", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteFriendConfirmDialog(friend: Friend, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = FriendCardTan,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "친구를 삭제할까요?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = friend.name,
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FriendPink,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "삭제하면 되돌릴 수 없어요.",
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(text = "취소", fontSize = 14.sp, color = Color.DarkGray)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(text = "삭제", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
