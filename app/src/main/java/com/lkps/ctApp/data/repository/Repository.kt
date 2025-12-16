package com.lkps.ctApp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.lkps.ctApp.data.model.User
import com.lkps.ctApp.data.source.firebase.ChatRoomListLiveData
import com.lkps.ctApp.data.source.firebase.ChatRoomLiveData
import com.lkps.ctApp.utils.states.AuthenticationState
import com.lkps.ctApp.utils.states.NetworkState

interface Repository {

    fun getChatRoomListLiveData(): ChatRoomListLiveData

    fun getChatRoomLiveData(): ChatRoomLiveData

    fun getUserLiveData(): LiveData<Pair<User?, AuthenticationState>>

    suspend fun isUsernameAvailable(username: String, callBack: (networkState: NetworkState) -> Unit)

    suspend fun deleteUsernames(callBack: (networkState: NetworkState) -> Unit)

    suspend fun deleteChatRooms(chatId: String, callBack: (networkState: NetworkState) -> Unit)

    suspend fun deleteUsers(userId: String, callBack: (networkState: NetworkState) -> Unit)

    suspend fun searchForUser(
        userId: String,
        username: String,
        callBack: (networkState: NetworkState, userList: MutableList<User>) -> Unit
    )

    fun addUsername(
        username: String,
        userId: String,
        callBack: (usernameStatus: NetworkState) -> Unit
    )

   suspend fun getUsersIds(
        callBack: (networkState: NetworkState, userList: MutableList<String>) -> Unit
    )

   suspend fun getUser(
       userId: String,
       callBack: (networkState: NetworkState, user: User) -> Unit
   )

   fun fetchConfigMsgLength(context: Context?, callBack: (msgLengh: Int) -> Unit)
}