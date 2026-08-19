package com.example.team4uu.data

import android.content.Context

// 아이 프로필(이름·나이·관심사). 회원가입에서 이름·나이를 받는다.
//
// 이 값은 AI 서버의 대화 엔드포인트로 넘어가서 **인형이 아이 이름을 부르는 데** 쓰인다.
//   WS /doll/talk?child=지우&age=4&interests=공룡,딸기
// 안 보내면 서버가 기본값("초록이", 4살)으로 도므로, 비어 있어도 대화 자체는 된다.
//
// 📌 지금은 SharedPreferences 에 둔다. 백엔드에 회원 저장소가 생기면 **이 파일만**
//    갈아끼우면 된다 — 호출부(SignUpScreen, MainScreen)는 save/load 만 알고 있다.
//    Room 으로 옮길 거라면 AppDatabase 의 version 을 올리고 Migration 을 같이 써야
//    한다(현재 version = 1 이고 마이그레이션 설정이 없어서, 엔티티만 늘리면
//    기존 설치에서 앱이 죽는다).
class ChildProfileStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(profile: ChildProfile) {
        prefs.edit()
            .putString(KEY_NAME, profile.name)
            .putInt(KEY_AGE, profile.age)
            .putString(KEY_INTERESTS, profile.interests.joinToString(","))
            .apply()
    }

    // 저장된 적이 없으면 null. 호출부는 이 경우 프로필 없이 대화를 시작하면 된다.
    fun load(): ChildProfile? {
        val name = prefs.getString(KEY_NAME, null) ?: return null
        return ChildProfile(
            name = name,
            age = prefs.getInt(KEY_AGE, DEFAULT_AGE),
            interests = prefs.getString(KEY_INTERESTS, "")
                .orEmpty()
                .split(",")
                .filter { it.isNotBlank() }
        )
    }

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val PREFS_NAME = "child_profile"
        private const val KEY_NAME = "child_name"
        private const val KEY_AGE = "child_age"
        private const val KEY_INTERESTS = "child_interests"

        const val DEFAULT_AGE = 4
    }
}

data class ChildProfile(
    val name: String,
    val age: Int,
    // 회원가입에서는 안 받는다. 관심사 입력 화면이 생기면 여기 채운다.
    val interests: List<String> = emptyList()
)

// 입력 검증. 서버(server/profile.py)와 같은 기준이어야 한다 —
// 앱에서 통과시킨 값을 서버가 조용히 잘라내면 사용자는 왜 이름이 달라졌는지 모른다.
object ChildProfileRules {
    const val MAX_NAME_LEN = 12
    const val MIN_AGE = 1
    const val MAX_AGE = 12

    // 한글·영문·공백만. 서버가 이름 자리에서 나머지를 털어내므로 여기서 미리 막는다.
    private val NAME_PATTERN = Regex("^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z ]+$")

    fun nameError(raw: String): String? {
        val name = raw.trim()
        return when {
            name.isEmpty() -> "아이 이름을 입력해주세요."
            name.length > MAX_NAME_LEN -> "이름은 ${MAX_NAME_LEN}자까지 입력할 수 있어요."
            !NAME_PATTERN.matches(name) -> "이름은 한글 또는 영문으로 입력해주세요."
            else -> null
        }
    }

    fun ageError(raw: String): String? {
        val age = raw.trim().toIntOrNull()
        return when {
            raw.isBlank() -> "아이 나이를 입력해주세요."
            age == null -> "나이는 숫자로 입력해주세요."
            age !in MIN_AGE..MAX_AGE -> "나이는 ${MIN_AGE}~${MAX_AGE}살 사이로 입력해주세요."
            else -> null
        }
    }
}
