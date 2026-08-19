package com.example.team4uu.ui.components

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import com.example.team4uu.data.Friend
import com.example.team4uu.data.SpriteStorage
import com.example.team4uu.data.audio.MouthShape
import java.io.File

// 대화 중인 캐릭터. 인형이 내는 소리에 맞춰 입 모양 3장을 갈아끼운다.
//
// 🔴 **3장을 미리 디코드해 둔다.** 입은 초당 최대 30번 바뀌는데 그때마다
//    BitmapFactory.decodeFile 을 부르면 디스크를 읽느라 못 따라가고 화면이 끊긴다.
//    기존 rememberFriendCharacterPainter 는 한 장짜리라 이 구조가 아니라서 따로 뒀다
//    (그쪽은 대화하지 않는 화면에서 그대로 쓴다).
//
// 스프라이트는 인형을 등록할 때 이미 내려받아 저장돼 있다:
//   filesDir/sprites/<friendId>/mouth_closed.png / mouth_half.png / mouth_open.png
@Composable
fun rememberTalkingCharacterPainter(friend: Friend?, mouth: MouthShape): Painter {
    val context = LocalContext.current
    val storage = remember(context) { SpriteStorage(context) }

    // friend 가 바뀔 때만 다시 읽는다. mouth 가 바뀔 때 다시 읽으면 미리 디코드해
    // 두는 의미가 없다.
    val painters = remember(friend?.id) {
        val dir = friend?.let { storage.spriteDir(it.id) }
        MouthShape.entries.associateWith { shape ->
            dir?.let { File(it, "${shape.spriteName}.png") }
                ?.takeIf { it.exists() }
                ?.let { BitmapFactory.decodeFile(it.absolutePath) }
                ?.asImageBitmap()
                ?.let { BitmapPainter(it) }
        }
    }

    // 서버가 변형 스프라이트를 못 만든 경우(응답의 failed) half/open 이 없을 수 있다.
    // 그때는 입을 다문 그림으로 버틴다 — 립싱크만 안 될 뿐 대화는 그대로 된다.
    painters[mouth]?.let { return it }
    painters[MouthShape.CLOSED]?.let { return it }

    // 스프라이트가 통째로 없으면(등록 전 목업 친구 등) 기존 경로로 떨어진다.
    return rememberFriendCharacterPainter(friend)
}
