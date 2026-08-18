package com.example.team4uu.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.team4uu.ui.theme.CameraBackgroundDark
import com.example.team4uu.ui.theme.FriendCardTan
import com.example.team4uu.ui.theme.FriendPink
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CameraScreen(onClose: () -> Unit, onPhotoCaptured: (String) -> Unit) {
    val context = LocalContext.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    // 카메라 프리뷰가 상태바/내비게이션 바까지 화면 전체를 꽉 채우고, 타이틀/닫기/가이드/셔터는
    // 전부 그 위에 얹히는 오버레이. 각 요소는 필요한 만큼만 statusBarsPadding/navigationBarsPadding을 씀.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraBackgroundDark)
    ) {
        CameraPreview(imageCapture = imageCapture)


        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }

        // 가이드 영역(위쪽, 남는 공간을 채움)과 TIP/셔터 영역(아래쪽, 고정 높이)을 분리해
        // 화면 크기와 상관없이 TIP 박스가 가이드 모서리와 겹치지 않도록 함
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                CameraOverlayGuides(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(top = 96.dp, bottom = 12.dp)
                        .width(320.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CameraTipBox()
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(6.dp, FriendPink, CircleShape)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(FriendCardTan)
                        .clickable {
                            takePhoto(context, imageCapture, onPhotoCaptured)
                        }
                )
            }
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
        val strokeWidth = 8.dp.toPx()
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
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth(0.95f)
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                    fontSize = 12.sp
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "대상을 단색 배경(흰 이불, 바닥)에 놓고 정면에서 촬영해 주세요!",
                color = Color.White,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "그림자가 심하게 지지 않는 밝은 곳이 좋아요.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
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
