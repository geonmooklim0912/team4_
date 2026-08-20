package com.example.team4uu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import coil.compose.rememberAsyncImagePainter
import com.example.team4uu.data.Friend
import com.example.team4uu.data.remote.RetrofitClient

// 친구의 배경 제거 2D 캐릭터 이미지를 그려주는 Painter.
// characterAssetPath는 /doll/stylize 응답의 sprite URL(상대 경로일 수도 있음)이라 네트워크로 불러옴.
// 친구가 없거나 characterAssetPath가 없으면(등록 직후 등) 투명 Painter로 자리만 비워둔다.
@Composable
fun rememberFriendCharacterPainter(friend: Friend?): Painter {
    val characterAssetPath = friend?.characterAssetPath
    return if (characterAssetPath != null) {
        rememberAsyncImagePainter(
            model = RetrofitClient.resolveAssetUrl(characterAssetPath),
            error = ColorPainter(Color.Transparent)
        )
    } else {
        ColorPainter(Color.Transparent)
    }
}
