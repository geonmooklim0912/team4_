package com.example.team4uu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.team4uu.ui.MainScreen
import com.example.team4uu.ui.theme.Team4UUTheme

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
