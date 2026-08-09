package com.example.team4uu

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.team4uu.data.Friend
import com.example.team4uu.viewmodel.FriendViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.team4uu.ui.theme.Team4UUTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


//메인 화면(앱을 실행했을 때 보이는 화면)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() //화면 전체(상태바까지)
        setContent { //이 액티비티가 보여줄 화면(UI)를 지정하는 함수
            Team4UUTheme {
                MainScreen() //실제로 화면 내용을 그리는 Composable 함수
            }
        }
    }
}

@Composable //화면을 그리는 함수라는 의미의 어노테이션
fun MainScreen(friendViewModel: FriendViewModel = viewModel()) {
    var showCamera by remember { mutableStateOf(false) } //ShowCamera 값이 바뀌면 화면을 다시만드는 상태 객체
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

    if (showCamera) {
        CameraScreen(
            onClose = { showCamera = false }, //카메라 화면을 띄우지만 닫기를 누르면 showCamera를 false
            onPhotoCaptured = { imagePath ->
                // TODO(F4): 지금은 임시 이름으로 저장. 이름 입력 다이얼로그가 만들어지면 그 값으로 대체.
                friendViewModel.addFriend(name = "친구 ${friends.size + 1}", imagePath = imagePath)
                showCamera = false
            }
        )
    } else { //카메라를 보여줄 상황이 아니면 메인 화면을 띄움
        Scaffold(//화면의 기본 뼈대를 잡아주는 compose의 부품임
            modifier = Modifier.fillMaxSize(),
            bottomBar = { BottomNavigationBar() }
        ) { innerPadding ->
            Box( //메인 화면의 본문 영역
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFD1D1D1))
            ) {
                // 친구 목록 조회 결과로 분기: 0마리면 온보딩, 1마리 이상이면 메인 홈
                if (friends.isEmpty()) {
                    EmptyFriendContent(onMakeFriendClick = ::requestCameraOrOpen)
                } else {
                    MainHomeContent(friends = friends, onAddFriendClick = ::requestCameraOrOpen)
                }
            }
        }
    }
}

@Composable
fun EmptyFriendContent(onMakeFriendClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "아직 친구가 없습니다.\n좋아하는 장남감을 지금 바로 등록해보세요.",
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.Black,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onMakeFriendClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A7A7A)),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .width(200.dp)
                .height(56.dp)
        ) {
            Text(text = "친구 만들기", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// F6(메인 홈 연출)이 만들어지기 전까지 쓰는 임시 화면.
// 데이터 계층(F5)이 실제로 동작하는지 확인하는 용도로, 저장된 친구 이름만 나열함.
@Composable
fun MainHomeContent(friends: List<Friend>, onAddFriendClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "등록된 친구 ${friends.size}마리",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(16.dp))
        friends.forEach { friend ->
            Text(text = "• ${friend.name}", fontSize = 16.sp, color = Color.Black)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddFriendClick) {
            Text(text = "+ 친구 추가")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "(메인 홈 화면은 F6에서 구현 예정)",
            fontSize = 12.sp,
            color = Color.DarkGray
        )
    }
}

@Composable
fun BottomNavigationBar() {
    Surface(
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BottomNavItem(
                modifier = Modifier.weight(1f),
                text = "밥 먹기",
                icon = Icons.Default.Info
            )
            BottomNavItem(
                modifier = Modifier.weight(1f),
                text = "놀기",
                icon = Icons.Default.Face
            )
        }
    }
}

@Composable
fun BottomNavItem(modifier: Modifier = Modifier, text: String, icon: ImageVector) {
    Surface(
        color = Color(0xFFE5E5E5),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CameraScreen(onClose: () -> Unit, onPhotoCaptured: (String) -> Unit) {
    val context = LocalContext.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFBDBDBD))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 상단 카메라 영역
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black)
        ) {
            CameraPreview(imageCapture = imageCapture)

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            CameraOverlayGuides(
                modifier = Modifier
                    .size(width = 250.dp, height = 350.dp)
                    .align(Alignment.Center)
            )

            CameraTipBox(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }

        // 하단 셔터 버튼 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(6.dp, Color(0xFFE0E0E0), CircleShape)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD1D1D1))
                    .clickable {
                        takePhoto(context, imageCapture, onPhotoCaptured)
                    }
            )
        }
    }
}

private fun takePhoto(context: Context, imageCapture: ImageCapture, onPhotoCaptured: (String) -> Unit) {
    val outputDirectory = context.cacheDir
    val photoFile = File(
        outputDirectory,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
                Toast.makeText(context, "사진 촬영 실패", Toast.LENGTH_SHORT).show()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d("CameraScreen", "Photo capture succeeded: ${photoFile.absolutePath}")
                Toast.makeText(context, "사진이 저장되었습니다!", Toast.LENGTH_SHORT).show()
                onPhotoCaptured(photoFile.absolutePath)
            }
        }
    )
}

@Composable
fun CameraOverlayGuides(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 6.dp.toPx()
        val cornerSize = 30.dp.toPx()
        val color = Color.White.copy(alpha = 0.8f)

        drawPath(
            path = Path().apply {
                moveTo(0f, cornerSize)
                lineTo(0f, 0f)
                lineTo(cornerSize, 0f)
            },
            color = color,
            style = Stroke(width = strokeWidth)
        )

        drawPath(
            path = Path().apply {
                moveTo(size.width - cornerSize, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, cornerSize)
            },
            color = color,
            style = Stroke(width = strokeWidth)
        )

        drawPath(
            path = Path().apply {
                moveTo(0f, size.height - cornerSize)
                lineTo(0f, size.height)
                lineTo(cornerSize, size.height)
            },
            color = color,
            style = Stroke(width = strokeWidth)
        )

        drawPath(
            path = Path().apply {
                moveTo(size.width - cornerSize, size.height)
                lineTo(size.width, size.height)
                lineTo(size.width, size.height - cornerSize)
            },
            color = color,
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun CameraTipBox(modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth(0.9f)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TIP",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "대상을 단색 배경(흰 이불, 바닥)에 놓고 정면에서 촬영해 주세요!",
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "그림자가 심하게 지지 않는 밝은 곳이 좋아요.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CameraPreview(imageCapture: ImageCapture) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Binding failed", e)
                }
            }, executor)
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
