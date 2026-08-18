package com.example.team4uu.data

import kotlinx.coroutines.flow.Flow

class FriendRepository(private val friendDao: FriendDao) {
    val friends: Flow<List<Friend>> = friendDao.getAllFriends()

    suspend fun addFriend(name: String, imagePath: String): Long =
        friendDao.insert(Friend(name = name, imagePath = imagePath))

    suspend fun getFriend(id: Long): Friend? = friendDao.getFriendById(id)

    suspend fun updateFriend(friend: Friend) = friendDao.update(friend)

    suspend fun deleteFriend(friend: Friend) = friendDao.delete(friend)

    suspend fun deleteAllFriends() = friendDao.deleteAll()
}