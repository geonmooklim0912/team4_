package com.example.team4uu.data

import androidx.room.TypeConverter

// Room은 List<String> 같은 컬렉션 타입을 그대로 저장할 수 없어서,
// 문자열 하나로 합쳤다가(fromMessageList) 다시 리스트로 쪼개는(toMessageList) 변환기가 필요함.
class Converters {
    private val separator = ""

    @TypeConverter
    fun fromMessageList(messages: List<String>): String = messages.joinToString(separator)

    @TypeConverter
    fun toMessageList(raw: String): List<String> =
        if (raw.isEmpty()) emptyList() else raw.split(separator)
}