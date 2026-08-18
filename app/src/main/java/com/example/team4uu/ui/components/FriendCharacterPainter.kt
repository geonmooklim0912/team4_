package com.example.team4uu.ui.components

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.example.team4uu.R
import com.example.team4uu.data.Friend

// 친구의 배경 제거 2D 캐릭터 이미지를 그려주는 Painter.
// TODO: 백엔드가 친구별 배경 제거 2D 이미지(characterAssetPath)를 내려주면 그걸로 교체.
// 지금은 백엔드 연동 전이라, 친구가 없거나 characterAssetPath가 없으면 example2 목업 캐릭터를 보여줌.
@Composable
fun rememberFriendCharacterPainter(friend: Friend?): Painter {
    val characterAssetPath = friend?.characterAssetPath
    return if (characterAssetPath != null) {
        val bitmap = remember(characterAssetPath) {
            BitmapFactory.decodeFile(characterAssetPath)?.asImageBitmap()
        }
        bitmap?.let { BitmapPainter(it) } ?: painterResource(id = R.drawable.character_example2)
    } else {
        painterResource(id = R.drawable.character_example2)
    }
}
