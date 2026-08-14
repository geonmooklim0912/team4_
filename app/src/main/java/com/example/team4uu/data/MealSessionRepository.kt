package com.example.team4uu.data

import kotlinx.coroutines.flow.Flow

class MealSessionRepository(private val mealSessionDao: MealSessionDao) {
    fun sessionsForFriend(friendId: Long): Flow<List<MealSession>> =
        mealSessionDao.getSessionsForFriend(friendId)

    suspend fun addSession(session: MealSession): Long = mealSessionDao.insert(session)

    suspend fun updateSession(session: MealSession) = mealSessionDao.update(session)
}