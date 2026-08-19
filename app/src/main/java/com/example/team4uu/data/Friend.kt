package com.example.team4uu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // 이 친구를 등록한 계정의 아이디(로그인 아이디). 계정마다 친구 목록을 따로 보여주기 위한 구분 키.
    val ownerUsername: String,
    val name: String,
    val imagePath: String,
    val characterAssetPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastFedAt: Long? = null
)