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

// 서버 변환까지 끝나 스프라이트를 임시 폴더에 받아둔 상태. 아직 이름이 없어서 DB 에는 안 들어가 있다.
// 사용자가 이름을 입력하면 DollRegistrationRepository.save 로 넘겨 실제로 저장한다.
data class ConvertedDoll(val photoPath: String, val spriteTemp: File)

// 촬영한 사진 한 장을 "등록된 친구"로 만드는 전체 흐름.
//
//   사진 -> [서버] 스타일 변환 28초 -> 스프라이트 5장 내려받기 -> (이름 입력) -> Room 저장
//
// 이름은 변환이 끝난 뒤 사용자에게 물어보므로, 변환(convert)과 저장(save) 두 단계로 나뉜다.
// 실패하면 DB 에 아무것도 남기지 않는다. 반쯤 등록된 친구가 목록에 뜨는 것보다
// 아예 없는 편이 사용자에게 이해하기 쉽다.
class DollRegistrationRepository(
    private val friendRepository: FriendRepository,
    private val spriteStorage: SpriteStorage
) {

    // 서버 상한과 같은 값. 여기서 먼저 걸러야 28초를 기다린 뒤 400 을 받는 일이 없다.
    private val maxUploadBytes = 10L * 1024 * 1024

    // 1단계: 사진을 서버로 보내 인형으로 변환하고 스프라이트를 임시 폴더에 받아둔다.
    // 이 시점에는 아직 친구 이름을 몰라도 된다.
    suspend fun convert(photoPath: String): ConvertedDoll {
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
        Log.i(TAG, "변환 완료 ${response.elapsedMs}ms, ${response.spriteMap.size}장")

        val temp = spriteStorage.downloadToTemp(response.spriteMap)
        return ConvertedDoll(photoPath = photoPath, spriteTemp = temp)
    }

    // 2단계: 변환 성공 후 사용자가 입력한 이름으로 실제 친구 목록에 저장한다.
    suspend fun save(ownerUsername: String, name: String, converted: ConvertedDoll): Long {
        val friendId = try {
            friendRepository.addFriend(ownerUsername = ownerUsername, name = name, imagePath = converted.photoPath)
        } catch (e: Exception) {
            converted.spriteTemp.deleteRecursively()
            throw e
        }

        spriteStorage.commit(converted.spriteTemp, friendId)

        // characterAssetPath 를 채워야 화면이 목업 대신 진짜 캐릭터를 그린다.
        // (FriendCharacterPainter 가 이 값이 null 이면 character_example2 로 떨어진다)
        friendRepository.getFriend(friendId)?.let { friend ->
            friendRepository.updateFriend(
                friend.copy(characterAssetPath = spriteStorage.baseSpriteFile(friendId).absolutePath)
            )
        }

        return friendId
    }

    // 이름 입력 단계에서 사용자가 취소한 경우, 받아둔 스프라이트 임시 파일을 정리한다.
    fun discard(converted: ConvertedDoll) {
        converted.spriteTemp.deleteRecursively()
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