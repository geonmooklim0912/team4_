package com.example.team4uu.data

import android.content.Context
import android.util.Log
import com.example.team4uu.data.remote.NetworkModule
import com.example.team4uu.data.remote.StylizeException
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

// 서버가 준 스프라이트 URL 을 기기에 내려받아 보관한다.
//
// 왜 URL 을 그대로 DB 에 넣지 않는가:
//   기존 화면 코드(FriendCharacterPainter)가 BitmapFactory.decodeFile 로 **로컬 경로**를
//   읽도록 이미 만들어져 있다. 파일로 내려두면 그 코드를 한 줄도 고칠 필요가 없다.
//   네트워크가 없어도 등록된 친구가 계속 보이는 것도 덤이다.
//
// 저장 구조 — Room 스키마를 건드리지 않으려고 파일 경로 규칙으로 푼다:
//   filesDir/sprites/<friendId>/mouth_closed.png   <- Friend.characterAssetPath 가 가리킨다
//                              /mouth_half.png
//                              /mouth_open.png     <- 나중에 립싱크할 때 이름으로 찾는다
//                              /happy.png
//                              /sleepy.png
class SpriteStorage(private val context: Context) {

    // 앱과 서버가 공유하는 이름. sprites[0] 이 항상 이것이다.
    companion object {
        const val BASE_SPRITE = "mouth_closed"
        private const val ROOT = "sprites"
        private const val TAG = "SpriteStorage"
    }

    fun spriteDir(friendId: Long): File = File(File(context.filesDir, ROOT), friendId.toString())

    fun baseSpriteFile(friendId: Long): File = File(spriteDir(friendId), "$BASE_SPRITE.png")

    // friendId 를 아직 모르는 상태에서 먼저 받아둔다.
    //
    // 순서가 이렇게 된 이유: id 는 Room 에 넣어야 나오는데, 다운로드가 실패했을 때
    // 이미 넣어둔 친구를 지우는 것보다 **DB 에 아무것도 안 남기는 쪽**이 깔끔하다.
    // 그래서 임시 폴더에 먼저 받고, 성공하면 그때 등록하고 폴더 이름을 바꾼다.
    suspend fun downloadToTemp(spriteMap: Map<String, String>): File = withContext(Dispatchers.IO) {
        val temp = File(File(context.filesDir, ROOT), "tmp-${UUID.randomUUID()}")
        if (!temp.mkdirs() && !temp.isDirectory) {
            throw StylizeException.of(StylizeException.NETWORK, "임시 폴더를 만들 수 없음")
        }

        try {
            for ((name, url) in spriteMap) {
                download(url, File(temp, "$name.png"))
            }
        } catch (e: Exception) {
            temp.deleteRecursively()
            throw e
        }

        // base 가 없으면 화면에 띄울 그림이 없는 것이므로 실패로 본다.
        // 나머지 4장은 없어도 정지 이미지로 동작한다.
        if (!File(temp, "$BASE_SPRITE.png").exists()) {
            temp.deleteRecursively()
            throw StylizeException.of("GEMINI_EMPTY", "$BASE_SPRITE 이 응답에 없음")
        }
        temp
    }

    // 등록이 끝나 id 가 정해지면 임시 폴더를 제자리로 옮긴다.
    suspend fun commit(temp: File, friendId: Long): File = withContext(Dispatchers.IO) {
        val dest = spriteDir(friendId)
        dest.deleteRecursively()
        dest.parentFile?.mkdirs()

        if (!temp.renameTo(dest)) {
            // 같은 파일시스템 안이라 보통은 성공한다. 실패하면 복사로 물러선다.
            temp.copyRecursively(dest, overwrite = true)
            temp.deleteRecursively()
        }
        dest
    }

    private fun download(url: String, dest: File) {
        val request = Request.Builder().url(url).build()
        NetworkModule.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("스프라이트 내려받기 실패 ${response.code}: $url")
            }
            val body = response.body ?: throw IOException("본문 없음: $url")
            dest.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
        Log.d(TAG, "저장 ${dest.name} (${dest.length()} bytes)")
    }
}