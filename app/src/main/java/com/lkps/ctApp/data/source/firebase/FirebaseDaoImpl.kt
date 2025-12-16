package com.lkps.ctApp.data.source.firebase

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.lkps.ctApp.controllers.shared_preferences.SharedPrefsController
import com.lkps.ctApp.data.model.User
import com.lkps.ctApp.utils.Constant.REF_CHAT_RECORDS
import com.lkps.ctApp.utils.Constant.REF_CHAT_ROOM
import com.lkps.ctApp.utils.Constant.REF_CHAT_ROOMS
import com.lkps.ctApp.utils.Constant.REF_USERS
import com.lkps.ctApp.utils.Constant.REF_USERS_NAMES
import com.lkps.ctApp.utils.Constant.REF_USER_NAME_LIST
import com.lkps.ctApp.utils.states.AuthenticationState
import com.lkps.ctApp.utils.states.NetworkState
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseDaoImpl {

    companion object {
        const val RC_SIGN_IN = 1
        const val RC_PHOTO_PICKER = 2
        const val CONFIG_DEFAULT_MSG_LENGTH_LIMIT = 1000
        const val CONFIG_MSG_LENGTH_KEY = "friendly_msg_length"
        const val CONFIG_DEFAULT_DELETE = 30  // sec
        const val CONFIG_DELETE_SEC_KEY = "delete_chat_room_sec"
        const val CONFIG_DELETE_MESSAGE_IGNORE_READER = false
        const val CONFIG_DELETE_MESSAGE_IGNORE_READER_KEY = "delete_chat_room_ignore_reader"
    }

    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val firebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    private val dbRefUsernames by lazy { firebaseFirestore.collection(REF_USERS_NAMES) }
    private val dbRefUsers by lazy { firebaseFirestore.collection(REF_USERS) }
    private val dbRefChatRooms by lazy { firebaseFirestore.collection(REF_CHAT_ROOMS) }
    private val dbRefChatRoom by lazy { firebaseFirestore.collection(REF_CHAT_ROOM) }
    private val dbRefChatRecords by lazy { firebaseFirestore.collection(REF_CHAT_RECORDS) }

    val chatRoomListLiveData = ChatRoomListLiveData()
    val chatRoomLiveData = ChatRoomLiveData()
    val user = UserLiveData()

    val userLiveData: LiveData<Pair<User?, AuthenticationState>> = user.map { userWithState ->
        userWithState.first?.let { user ->
            chatRoomListLiveData.setNewDocRef(user)
            chatRoomLiveData.user = user
        }
        userWithState
    }
//    val userLiveData = Transformations.map(user) { userWithState ->
//        userWithState.first?.let { user ->
//            chatRoomListLiveData.setNewDocRef(user)
//            chatRoomLiveData.user = user
//        }
//        userWithState
//    }

    init {
        fetchConfig()
    }

    private fun fetchConfig() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(60)
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        val defaultConfigMap = HashMap<String, Any>()
        defaultConfigMap[CONFIG_MSG_LENGTH_KEY] = CONFIG_DEFAULT_MSG_LENGTH_LIMIT
        defaultConfigMap[CONFIG_DELETE_SEC_KEY] = CONFIG_DEFAULT_DELETE
        defaultConfigMap[CONFIG_DELETE_MESSAGE_IGNORE_READER_KEY] = CONFIG_DELETE_MESSAGE_IGNORE_READER
        firebaseRemoteConfig.setDefaultsAsync(defaultConfigMap)
    }

    fun fetchConfigMsgLength(context: Context?, callBack: (msgLenght: Int) -> Unit) {
        firebaseRemoteConfig.fetchAndActivate()
            .addOnSuccessListener {
                val sharedPrefsController = SharedPrefsController(context)
                var messageLength = firebaseRemoteConfig.getLong(CONFIG_MSG_LENGTH_KEY)
                val deleteChatRoomSeconds = firebaseRemoteConfig.getLong(CONFIG_DELETE_SEC_KEY)
                val ignoreRecipientIFReadAndDelete = firebaseRemoteConfig.getBoolean(CONFIG_DELETE_MESSAGE_IGNORE_READER_KEY)
                sharedPrefsController.setDeleteMessageSec(deleteChatRoomSeconds)
                sharedPrefsController.setRecipientIgnore(ignoreRecipientIFReadAndDelete)
                callBack(firebaseRemoteConfig.getLong(CONFIG_MSG_LENGTH_KEY).toInt())
            }

            .addOnFailureListener {
                val sharedPrefsController = SharedPrefsController(context)
                var defaultMessageLength = firebaseRemoteConfig.getLong(CONFIG_MSG_LENGTH_KEY)
                val deleteChatRoomSeconds = firebaseRemoteConfig.getLong(CONFIG_DELETE_SEC_KEY)
                val ignoreRecipientIFReadAndDelete = firebaseRemoteConfig.getBoolean(CONFIG_DELETE_MESSAGE_IGNORE_READER_KEY)
                sharedPrefsController.setRecipientIgnore(ignoreRecipientIFReadAndDelete)
                sharedPrefsController.setDeleteMessageSec(deleteChatRoomSeconds)
                callBack(firebaseRemoteConfig.getLong(CONFIG_MSG_LENGTH_KEY).toInt())
            }
    }

    private fun addUser(user: User, callBack: (usernameStatus: NetworkState) -> Unit) {
        val userId = userLiveData.value?.first?.userId ?: run {
            callBack(NetworkState.FAILED)
            return
        }
        dbRefUsers.document(userId).set(user.apply { this.userId = userId })
            .addOnSuccessListener {
                this.user.setNewUser(user)
                callBack(NetworkState.LOADED)
            }
            .addOnFailureListener {
                callBack(NetworkState.FAILED)
            }
    }

    fun addUsername(
        userId: String,
        username: String,
        callBack: (usernameStatus: NetworkState) -> Unit
    ) {
        val user = User()
        callBack(NetworkState.LOADING)
        val usernameLowerCase = username.lowercase(Locale.ROOT)
        user.username = usernameLowerCase
        user.userId = userId
        user.usernameList = User.nameToArray(usernameLowerCase)
        user.fcmTokenList = arrayListOf()
        dbRefUsernames.document(usernameLowerCase).set(user)
            .addOnSuccessListener {
                addUser(user) { callBack(it) }
            }
            .addOnFailureListener {
                callBack(NetworkState.FAILED)
            }
    }

    suspend fun getUsersIds(
        callBack: (networkState: NetworkState, userList: MutableList<String>) -> Unit
    ) {
        try {
            val listOfUserIds: MutableList<String> = mutableListOf()
            callBack(NetworkState.LOADING, mutableListOf())

            dbRefUsers.get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        listOfUserIds.add(document.id)
                    }
                    val networkState =
                        if (listOfUserIds.isNotEmpty()) NetworkState.LOADED else NetworkState.FAILED
                    callBack(networkState, listOfUserIds)
                }
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED, mutableListOf())
        }
    }

    suspend fun getUsers(
        userId: String,
        callBack: (networkState: NetworkState, user: User) -> Unit
    ) {
        try {
            callBack(NetworkState.LOADING, User())
//            val querySnapshot = dbRefUsers.whereArrayContains("userId", userId).get().await()
//            val check  = dbRefUsers.document(userId).get().await()
//            val list : MutableList<User> = querySnapshot.toObjects(User::class.java)
            dbRefUsers.get().await().documents.mapNotNull { snapShot ->
                val user: User? = snapShot.toObject(User::class.java)
                if (user != null) {
                    callBack(NetworkState.LOADED, user)
                } else {
                    callBack(NetworkState.FAILED, User())
                }
            }
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED, User())
        }
    }

    suspend fun searchForUser(
        userId: String,
        username: String,
        callBack: (networkState: NetworkState, userList: MutableList<User>) -> Unit
    ) {
        try {
            callBack(NetworkState.LOADING, mutableListOf())
            val querySnapshot = dbRefUsers.whereArrayContains(REF_USER_NAME_LIST, username).get().await()
            val list = querySnapshot.toObjects(User::class.java)
            val currentUser = list.find { it.userId == userId }
            list.remove(currentUser)
            val networkState = if (list.isNotEmpty()) NetworkState.LOADED else NetworkState.FAILED
            callBack(networkState, list)
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED, mutableListOf())
        }
    }

    suspend fun isUsernameAvailable(
        username: String,
        callBack: (networkState: NetworkState) -> Unit
    ) {
        try {

            callBack(NetworkState.LOADING)
            val document = dbRefUsernames.document(username.lowercase(Locale.ROOT)).get().await()
            callBack(if (document.exists()) NetworkState.FAILED else NetworkState.LOADED)
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED)
        }
    }

    fun deleteUserNamesDocument(
        callBack: (networkState: NetworkState) -> Unit
    ) {
        try {
            callBack(NetworkState.LOADING)
            deleteCollection(dbRefUsernames, 5)
            callBack(NetworkState.LOADED)
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED)
        }
    }

    suspend fun deleteUserDocument(
        userId: String,
        callBack: (networkState: NetworkState) -> Unit
    ) {
        try {
            callBack(NetworkState.LOADING)
            deleteFiles(callBack)
            dbRefUsers.document(userId).delete()
//            val user1 = documentSnapshot.toObject(User::class.java)
//                user1?.userId.let {
//                    it?.let { it1 -> dbRefUsers.document(it1).update(mapOf("userId" to FieldValue.delete())) }
//                }

            callBack(NetworkState.LOADED)
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED)
        }
    }

    suspend fun searchInChatRooms(
        userId: String,
        callBack: (networkState: NetworkState, userList: MutableList<User>) -> Unit
    ) {
        try {
            callBack(NetworkState.LOADING, mutableListOf())
            val querySnapshot =
                dbRefChatRooms.whereArrayContains(REF_USER_NAME_LIST, userId).get().await()
            val list = querySnapshot.toObjects(User::class.java)
            list[0]?.userId.let {
                    it?.let { it1 -> dbRefChatRooms.document(it1).update(mapOf("senderId" to FieldValue.delete())) }
                }
            val networkState = if (list.isNotEmpty()) NetworkState.LOADED else NetworkState.FAILED
            callBack(networkState, list)
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED, mutableListOf())
        }
    }

    fun deleteFiles(callBack: (networkState: NetworkState) -> Unit) {
        callBack(NetworkState.LOADING)
        dbRefChatRecords.document().delete()
            .addOnSuccessListener {
                callBack(NetworkState.LOADED)
            }.addOnFailureListener {
                callBack(NetworkState.LOADED)
            }
    }

    suspend fun deleteChatRoomDocument(
        chatId: String,
        callBack: (networkState: NetworkState) -> Unit
    ) {
        try {
            callBack(NetworkState.LOADING)
            val doc1 = dbRefChatRooms.document(chatId).get().await()
            val chatoomColl = dbRefChatRoom.document(chatId).collection(REF_CHAT_ROOM)
//            dbRefChatRooms.document(chatId).delete().await()
            chatoomColl.document().update(mapOf(REF_CHAT_ROOM to FieldValue.delete()))
            dbRefChatRooms.document().update(mapOf(REF_CHAT_ROOM to FieldValue.delete()))
            callBack(NetworkState.LOADED)
        } catch (e: FirebaseFirestoreException) {
            callBack(NetworkState.FAILED)
        }
    }

    private fun deleteCollection(collection: CollectionReference, batchSize: Int) {
        try {
            // Retrieve a small batch of documents to avoid out-of-memory errors/
            var deleted = 0
            collection
                .limit(batchSize.toLong())
                .get()
                .addOnCompleteListener {
                    for (document in it.result.documents) {
                        document.reference.delete()
                        ++deleted
                    }
                    if (deleted >= batchSize) {
                        // retrieve and delete another batch
                        deleteCollection(collection, batchSize)
                    }
                }
        } catch (e: Exception) {
            System.err.println("Error deleting collection : " + e.message)
        }
    }
}