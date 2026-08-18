package com.example.team4uu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.team4uu.ui.theme.FriendCardTan
import com.example.team4uu.ui.theme.FriendPink
import com.example.team4uu.ui.theme.FriendYellow

// TODO: 실제로는 "우리 아이 맞춤" 추천이 사용 이력/백엔드 기반으로 내려와야 함.
// 아직 그런 데이터가 없어서, 그럴듯한 기본 태그 몇 개로 시작함.
val DEFAULT_MISSION_TAGS = listOf("골고루 먹기", "밥 한 공기", "당근 먹기", "20분 식사", "브로콜리 먹기")

private val CATEGORY_MISSION_TAGS = listOf("식사 예절", "물 마시기", "편식 정복")

// "밥 먹기"를 누르면 뜨는 오늘의 미션 선택 팝업.
// savedTags(자주 쓰는 태그 목록)는 팝업을 닫아도 유지되고(호출부에서 상태를 들고 있음),
// "저장" 버튼으로 추가한 임시 태그는 이 팝업 안에서만 유지되다가 팝업이 닫히면 사라짐.
@Composable
fun FeedMissionDialog(
    savedTags: List<String>,
    onDeleteTag: (String) -> Unit,
    onSaveTagPermanently: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (selectedMission: String?) -> Unit
) {
    var tempTags by remember { mutableStateOf(listOf<String>()) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🔥 우리 아이 맞춤 / 최근 자주 쓴 목표",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    (savedTags + tempTags).forEach { tag ->
                        MissionTagChip(
                            text = tag,
                            selected = tag == selectedTag,
                            onClick = { selectedTag = tag },
                            onDelete = {
                                if (tag in savedTags) onDeleteTag(tag) else tempTags = tempTags - tag
                                if (selectedTag == tag) selectedTag = null
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "📋 카테고리별 추천 태그",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CATEGORY_MISSION_TAGS.forEach { tag ->
                        CategoryTagChip(
                            text = tag,
                            selected = tag == selectedTag,
                            onClick = { selectedTag = tag }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddDialog = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(FriendYellow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "오늘의 미션 직접 추가", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "오늘의 미션 직접 추가", fontSize = 12.sp, color = Color.DarkGray)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onConfirm(selectedTag) },
                    colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(text = "확인", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddDialog) {
        AddMissionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { text, saveToFrequent ->
                if (saveToFrequent) {
                    onSaveTagPermanently(text)
                } else {
                    tempTags = tempTags + text
                }
                selectedTag = text
                showAddDialog = false
            }
        )
    }
}

// 오늘의 미션을 직접 입력하는 팝업. "자주 쓰는 태그 목록에도 저장하기"는 영구 저장,
// "저장"은 이번 팝업 세션에서만 쓰이는 임시 저장(FeedMissionDialog의 tempTags로 들어감).
@Composable
private fun AddMissionDialog(onDismiss: () -> Unit, onSave: (text: String, saveToFrequent: Boolean) -> Unit) {
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = FriendYellow
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(text = "오늘의 미션을 입력해주세요") },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = FriendCardTan,
                        focusedContainerColor = FriendCardTan,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { if (text.isNotBlank()) onSave(text, true) },
                    colors = ButtonDefaults.buttonColors(containerColor = FriendPink),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "자주 쓰는 태그 목록에도 저장하기", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
                // 자주 쓰는 태그로 저장하는 게 아니라, 오늘 이 팝업에서만 쓰고 사라지는 일회성 저장
                OutlinedButton(
                    onClick = { if (text.isNotBlank()) onSave(text, false) },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(text = "저장", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MissionTagChip(text: String, selected: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Box {
        Surface(
            onClick = onClick,
            color = if (selected) FriendPink else FriendCardTan,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = text,
                color = if (selected) Color.White else Color.DarkGray,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
        // 태그 삭제용 X. 태그 칩 자체의 클릭(선택)과 겹치지 않게 모서리에 작은 별도 터치 영역으로 둠.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFDDDDDD), CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "태그 삭제",
                modifier = Modifier.size(12.dp),
                tint = Color.DarkGray
            )
        }
    }
}

@Composable
private fun CategoryTagChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) FriendPink else Color(0xFFE0E0E0),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.DarkGray,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
