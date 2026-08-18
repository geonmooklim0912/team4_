package com.example.team4uu

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.team4uu.ui.MainScreen
import com.example.team4uu.ui.theme.Team4UUTheme

//메인 화면(앱을 실행했을 때 보이는 화면)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 기본값(SystemBarStyle.auto)은 상태바/내비게이션 바 위에 반투명 스크림을 깔아서,
        // 배경 이미지가 그 부분만 살짝 어둡게(=화면이 꽉 안 찬 것처럼) 보이게 만듦.
        // 스크림 색을 완전 투명으로 지정해서 배경이 진짜 가장자리까지 그대로 보이게 함.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        // 안드로이드 10(API 29)부터는 위 스크림 설정과 별개로 시스템이 상태바/내비게이션 바에
        // 자체적으로 최소 대비(스크림)를 또 강제로 깔 수 있음. 그것까지 꺼야 진짜로 배경이
        // 가장자리까지 아무 필터 없이 꽉 차 보임.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        setContent { //이 액티비티가 보여줄 화면(UI)를 지정하는 함수
            Team4UUTheme {
                MainScreen() //실제로 화면 내용을 그리는 Composable 함수
            }
        }
    }
}
