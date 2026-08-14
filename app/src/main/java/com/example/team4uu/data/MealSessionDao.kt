package com.example.team4uu.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MealSessionDao {
    @Query("SELECT * FROM meal_sessions WHERE friendId = :friendId ORDER BY startedAt DESC")
    fun getSessionsForFriend(friendId: Long): Flow<List<MealSession>>

    @Insert
    suspend fun insert(session: MealSession): Long

    @Update
    suspend fun update(session: MealSession)
}