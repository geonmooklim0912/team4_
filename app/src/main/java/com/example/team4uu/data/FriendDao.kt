package com.example.team4uu.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY createdAt DESC")
    fun getAllFriends(): Flow<List<Friend>>

    @Query("SELECT * FROM friends WHERE id = :id")
    suspend fun getFriendById(id: Long): Friend?

    @Insert
    suspend fun insert(friend: Friend): Long

    @Update
    suspend fun update(friend: Friend)

    @Delete
    suspend fun delete(friend: Friend)

    @Query("DELETE FROM friends")
    suspend fun deleteAll()
}