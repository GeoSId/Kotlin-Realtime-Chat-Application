package com.lkps.ctApp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.lkps.ctApp.data.model.User
import com.lkps.ctApp.data.source.firebase.ChatRoomListLiveData
import com.lkps.ctApp.data.source.firebase.ChatRoomLiveData
import com.lkps.ctApp.data.source.firebase.FirebaseDaoImpl
import com.lkps.ctApp.utils.states.AuthenticationState
import com.lkps.ctApp.utils.states.NetworkState
import javax.inject.Inject

class RepositoryImpl @Inject constructor(
    private val firebaseDaoImpl: FirebaseDaoImpl
) : Repository {

    override fun getChatRoomListLiveData(): ChatRoomListLiveData {
        return firebaseDaoImpl.chatRoomListLiveData
    }

    override fun getChatRoomLiveData(): ChatRoomLiveData {
        return firebaseDaoImpl.chatRoomLiveData
    }

    override fun getUserLiveData(): LiveData<Pair<User?, AuthenticationState>> {
        return firebaseDaoImpl.userLiveData
    }

    override suspend fun isUsernameAvailable(
        username: String,
        callBack: (networkState: NetworkState) -> Unit
    ) {
        firebaseDaoImpl.isUsernameAvailable(username, callBack)
    }

    override suspend fun searchForUser(
        userId: String,
        username: String,
        callBack: (networkState: NetworkState, userList: MutableList<User>) -> Unit
    ) {
        firebaseDaoImpl.searchForUser(userId, username, callBack)
    }

    override fun addUsername(
        username: String,
        userId: String,
        callBack: (usernameStatus: NetworkState) -> Unit
    ) {
        firebaseDaoImpl.addUsername(userId, username, callBack)
    }

    override suspend fun getUsersIds(callBack: (networkState: NetworkState, userList: MutableList<String>) -> Unit) {
        firebaseDaoImpl.getUsersIds(callBack)
    }

    override suspend fun getUser(
        userId: String,
        callBack: (networkState: NetworkState, user: User) -> Unit
    ) {
        firebaseDaoImpl.getUsers(userId, callBack)
    }


    override fun fetchConfigMsgLength(context: Context?, callBack: (msgLenght: Int) -> Unit) {
        firebaseDaoImpl.fetchConfigMsgLength(context, callBack)
    }

    override suspend fun deleteUsernames(
        callBack: (networkState: NetworkState) -> Unit
    ) {
        firebaseDaoImpl.deleteUserNamesDocument(callBack)
    }

    override suspend fun deleteChatRooms(
        chatId: String,
        callBack: (networkState: NetworkState) -> Unit
    ) {
        firebaseDaoImpl.deleteChatRoomDocument(chatId, callBack)
    }

    override suspend fun deleteUsers(
        userId: String,
        callBack: (networkState: NetworkState) -> Unit
    ) {
        firebaseDaoImpl.deleteUserDocument(userId, callBack)
    }

}