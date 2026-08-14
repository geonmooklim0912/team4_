package com.example.team4uu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val imagePath: String,
    val characterAssetPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastFedAt: Long? = null
)