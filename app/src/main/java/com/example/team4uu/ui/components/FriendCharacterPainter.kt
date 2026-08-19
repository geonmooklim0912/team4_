package com.example.team4uu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import com.example.team4uu.R
import com.example.team4uu.data.Friend
import com.example.team4uu.data.remote.RetrofitClient

// 친구의 배경 제거 2D 캐릭터 이미지를 그려주는 Painter.
// characterAssetPath는 /doll/stylize 응답의 sprite URL(상대 경로일 수도 있음)이라 네트워크로 불러옴.
// 친구가 없거나 characterAssetPath가 없으면 example2 목업 캐릭터를 보여줌.
@Composable
fun rememberFriendCharacterPainter(friend: Friend?): Painter {
    val characterAssetPath = friend?.characterAssetPath
    return if (characterAssetPath != null) {
        rememberAsyncImagePainter(
            model = RetrofitClient.resolveAssetUrl(characterAssetPath),
            error = painterResource(id = R.drawable.character_example2)
        )
    } else {
        painterResource(id = R.drawable.character_example2)
    }
}
