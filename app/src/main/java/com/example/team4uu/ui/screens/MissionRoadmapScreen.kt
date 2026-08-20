package com.example.team4uu.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team4uu.R
import com.example.team4uu.ui.MissionLevelProgress
import com.example.team4uu.ui.STARS_TO_UNLOCK_LEVEL
import com.example.team4uu.ui.theme.FriendPink
import kotlin.math.cos
import kotlin.math.sin

private val NODE_CIRCLE_SIZE = 56.dp // 레벨 숫자/자물쇠가 들어가는 원
private val NODE_SIZE = 92.dp // 원 + 둘레를 감싸는 별까지 포함한 전체 노드 크기(부모에서 중심 좌표 계산에 씀)
private val STAR_ORBIT_RADIUS = 38.dp // 원 중심에서 별까지 거리
private val STAR_ICON_SIZE = 16.dp
private val STAR_BADGE_SIZE = 22.dp // 별 뒤에 까는 흰 원형 뱃지 크기(배경 색과 무관하게 항상 도드라지도록)
// 별 3개를 원 위쪽 반원에 걸쳐 배치하는 각도(도). 0°=오른쪽, -90°=바로 위, 화면 좌표라 y는 위로 갈수록 음수.
private val STAR_ANGLES_DEGREES = listOf(-120.0, -90.0, -60.0)

// 배경 이미지(bg_mission_roadmap) 속 구불구불한 길을 따라 8단계를 눈대중으로 배치한 좌표(화면 비율 0~1).
// 1단계가 맨 아래, 8단계가 맨 위. 실제 기기에서 보고 길과 어긋나면 이 좌표만 조정하면 됨.


// HomeOptionsPanel의 미션로드맵 버튼(왼쪽 아이콘)을 누르면 오는 화면.
// 게임처럼 길을 따라 단계가 있고, 각 단계에서 별 3개를 모으면 다음 단계 잠금이 풀리는 구조.
@Composable
fun MissionRoadmapScreen(levels: List<MissionLevelProgress>, onClose: () -> Unit) {
     val levelPositions = listOf(
        0.44f to 0.84f, // 1
        0.80f to 0.79f, // 2
        0.45f to 0.68f, // 3
        0.67f to 0.56f, // 4
        0.50f to 0.44f, // 5
        0.40f to 0.30f, // 6
        0.70f to 0.23f, // 7
        0.62f to 0.065f // 8
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_mission_roadmap),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 1단계(fy=0.84, 화면 맨 아래 쪽)의 별이 하단 내비게이션 바에 가려 안 보이던 문제 —
        // 배경 이미지는 그대로 화면 끝까지 채우되(edge-to-edge), 노드 위치 계산은 상태바·내비게이션
        // 바를 뺀 안전 영역 크기를 기준으로 해서 어떤 단계도 시스템 바 밑에 깔리지 않게 함.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val safeWidth = maxWidth
            val safeHeight = maxHeight

            levels.forEachIndexed { index, missionLevel ->
                val unlocked = index == 0 || levels[index - 1].starsEarned >= STARS_TO_UNLOCK_LEVEL
                val (fx, fy) = levelPositions[index]

                MissionLevelNode(
                    level = missionLevel,
                    unlocked = unlocked,
                    modifier = Modifier.offset(
                        x = safeWidth * fx - NODE_SIZE / 2,
                        y = safeHeight * fy - NODE_SIZE / 2
                    )
                )
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "닫기", tint = Color.Black)
        }
    }
}

// 단계 하나: 원형 노드(잠금 해제면 단계 숫자, 아니면 자물쇠) 둘레를 별 3개가 감싸는 형태.
@Composable
private fun MissionLevelNode(level: MissionLevelProgress, unlocked: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(NODE_SIZE),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(NODE_CIRCLE_SIZE)
                .clip(CircleShape)
                .background(if (unlocked) FriendPink else Color(0xFFBDBDBD))
                .border(width = 3.dp, color = Color.White, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (unlocked) {
                Text(
                    text = level.level.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            } else {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "잠김", tint = Color.White)
            }
        }

        // 별 3개를 원 중심 기준 각도(STAR_ANGLES_DEGREES)로 배치해서 원 둘레를 감싸는 형태로 만듦.
        // 1단계 자리(fy=0.84)의 길 배경이 유독 옅은 크림색이라, 별에 흰 바탕(뱃지)을 깔지 않으면
        // 노란색 달성 별이 배경과 거의 같은 색으로 보여 안 보이는 것처럼 됐다 — 그래서 배경 색이
        // 무엇이든 항상 도드라지도록 별마다 작고 흰 원형 뱃지를 깐다.
        STAR_ANGLES_DEGREES.forEachIndexed { starIndex, angleDegrees ->
            val angleRadians = Math.toRadians(angleDegrees)
            Box(
                modifier = Modifier
                    .size(STAR_BADGE_SIZE)
                    .offset(
                        x = STAR_ORBIT_RADIUS * cos(angleRadians).toFloat(),
                        y = STAR_ORBIT_RADIUS * sin(angleRadians).toFloat()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(STAR_ICON_SIZE + 3.dp)
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (starIndex < level.starsEarned) Color(0xFFFFC107) else Color(0xFF7A3B2E),
                    modifier = Modifier.size(STAR_ICON_SIZE)
                )
            }
        }
    }
}
