package com.example.team4uu.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Friend::class,
            parentColumns = ["id"],
            childColumns = ["friendId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("friendId")]
)
data class MealSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val friendId: Long,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationSec: Int = 0,
    val messages: List<String> = emptyList()
)