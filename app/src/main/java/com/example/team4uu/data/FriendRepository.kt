package com.example.team4uu.data

import kotlinx.coroutines.flow.Flow

class FriendRepository(private val friendDao: FriendDao) {
    fun friendsForOwner(ownerUsername: String): Flow<List<Friend>> =
        friendDao.getFriendsForOwner(ownerUsername)

    suspend fun addFriend(
        ownerUsername: String,
        name: String,
        imagePath: String,
        characterAssetPath: String? = null
    ): Long = friendDao.insert(
        Friend(
            ownerUsername = ownerUsername,
            name = name,
            imagePath = imagePath,
            characterAssetPath = characterAssetPath
        )
    )

    suspend fun getFriend(id: Long): Friend? = friendDao.getFriendById(id)

    suspend fun updateFriend(friend: Friend) = friendDao.update(friend)

    suspend fun deleteFriend(friend: Friend) = friendDao.delete(friend)
}