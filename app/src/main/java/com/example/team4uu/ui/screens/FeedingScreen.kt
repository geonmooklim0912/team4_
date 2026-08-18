package com.example.team4uu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.team4uu.data.Friend
import com.example.team4uu.ui.components.livingCharacterEffect
import com.example.team4uu.ui.components.rememberFriendCharacterPainter

// "밥 먹기" 버튼을 누르면 오는 화면. 진짜 AR(ARCore 바닥 인식 등)은 아직 아니고,
// 후면 카메라 실시간 화면 위에 지금 선택된 친구 캐릭터를 얹어서 보여주는 간단한 오버레이 방식.
// TODO: 나중에 ARCore/Sceneform으로 업그레이드하면 실제 공간에 캐릭터를 고정할 수 있음.
@Composable
fun FeedingScreen(friend: Friend?, onClose: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // CameraPreview는 CameraScreen.kt에서도 쓰는 CameraX 후면 카메라 미리보기.
            // 여기서는 사진을 찍을 게 아니라서 imageCapture는 그냥 바인딩용으로만 생성.
            val imageCapture = remember { ImageCapture.Builder().build() }
            CameraPreview(imageCapture = imageCapture)

            if (friend != null) {
                val characterPainter = rememberFriendCharacterPainter(friend)
                Image(
                    painter = characterPainter,
                    contentDescription = friend.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.39f)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 70.dp)
                        .livingCharacterEffect()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "밥 먹기를 하려면 카메라 권한이 필요해요",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
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
            Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
        }
    }
}
