package com.example.team4uu.data

import android.util.Log
import com.example.team4uu.data.remote.NetworkModule
import com.example.team4uu.data.remote.StylizeException
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException

// 촬영한 사진 한 장을 "등록된 친구"로 만드는 전체 흐름.
//
//   사진 -> [서버] 스타일 변환 28초 -> 스프라이트 5장 내려받기 -> Room 저장
//
// 실패하면 DB 에 아무것도 남기지 않는다. 반쯤 등록된 친구가 목록에 뜨는 것보다
// 아예 없는 편이 사용자에게 이해하기 쉽다.
class DollRegistrationRepository(
    private val friendRepository: FriendRepository,
    private val spriteStorage: SpriteStorage
) {

    // 서버 상한과 같은 값. 여기서 먼저 걸러야 28초를 기다린 뒤 400 을 받는 일이 없다.
    private val maxUploadBytes = 10L * 1024 * 1024

    // ownerUsername: 계정별로 친구 목록을 분리하면서 필수가 됐다(FriendRepository 참조).
    suspend fun register(ownerUsername: String, name: String, photoPath: String): Long {
        val photo = File(photoPath)
        if (!photo.exists() || photo.length() == 0L) {
            throw StylizeException.of("INVALID_IMAGE", "사진 파일이 없음: $photoPath")
        }
        if (photo.length() > maxUploadBytes) {
            throw StylizeException.of("INVALID_IMAGE", "사진이 상한 초과: ${photo.length()}")
        }

        val response = try {
            val part = MultipartBody.Part.createFormData(
                // ⚠️ "image" 여야 한다. 서버가 이 이름으로만 파일을 찾는다.
                name = "image",
                filename = photo.name,
                body = photo.asRequestBody(mediaTypeOf(photo))
            )
            NetworkModule.stylizeApi.stylize(part)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw NetworkModule.parseError(e)
        } catch (e: IOException) {
            // 연결 끊김·타임아웃. 서버가 준 code 가 없으므로 네트워크로 분류한다.
            throw StylizeException.of(StylizeException.NETWORK, cause = e)
        }

        if (response.failed.isNotEmpty()) {
            // 등록 자체는 성공이다. 빠진 건 립싱크용 스프라이트뿐이라
            // 지금 범위(정지 이미지)에서는 화면에 아무 차이가 없다.
            Log.w(TAG, "스프라이트 일부 실패: ${response.failed.joinToString()}")
        }
        Log.i(TAG, "변환 완료 ${response.elapsed_ms}ms, ${response.sprite_map.size}장")

        // 다운로드를 먼저 끝내고 나서 DB 에 넣는다. 순서를 바꾸면 다운로드가 실패했을 때
        // 이미 등록된 친구를 되돌려 지워야 한다.
        val temp = spriteStorage.downloadToTemp(response.sprite_map)

        val friendId = try {
            friendRepository.addFriend(
                ownerUsername = ownerUsername,
                name = name,
                imagePath = photoPath
            )
        } catch (e: Exception) {
            temp.deleteRecursively()
            throw e
        }

        spriteStorage.commit(temp, friendId)

        // characterAssetPath 를 채워야 화면이 빈 자리 대신 진짜 캐릭터를 그린다.
        friendRepository.getFriend(friendId)?.let { friend ->
            friendRepository.updateFriend(
                friend.copy(characterAssetPath = spriteStorage.baseSpriteFile(friendId).absolutePath)
            )
        }

        return friendId
    }

    private fun mediaTypeOf(file: File) =
        when (file.extension.lowercase()) {
            "png" -> "image/png".toMediaType()
            else -> "image/jpeg".toMediaType()
        }

    private companion object {
        const val TAG = "DollRegistration"
    }
}