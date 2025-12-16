package com.lkps.ctApp.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.lkps.ctApp.controllers.device.DeviceController
import com.lkps.ctApp.controllers.shared_preferences.SharedPrefsController
import com.lkps.ctApp.data.model.Chat
import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.data.model.User
import com.lkps.ctApp.data.repository.Repository
import com.lkps.ctApp.data.source.firebase.ChatRoomListLiveData
import com.lkps.ctApp.data.source.firebase.ChatRoomLiveData
import com.lkps.ctApp.utils.IntentManager.getIntentUri
import com.lkps.ctApp.utils.extension.clear
import com.lkps.ctApp.utils.states.AuthenticationState
import com.lkps.ctApp.utils.states.FragmentState
import com.lkps.ctApp.utils.states.NetworkState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class FirebaseViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private var isUsernameAvailableJob: Job? = null
    private var searchForUserJob: Job? = null
    private var usersJob: Job? = null

    private val _userList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>> = _userList

    private val _userSearchStatus = MutableLiveData<NetworkState>()
    val userSearchStatus: LiveData<NetworkState> = _userSearchStatus

    private val usersIdsList: MutableList<String> = mutableListOf()

    private val _userIdsStatus = MutableLiveData<NetworkState>()
    val userIdsStatus: LiveData<NetworkState> = _userIdsStatus

    private val _pushFileStatus = MutableLiveData<NetworkState>()
    val pushFileStatus: LiveData<NetworkState> = _pushFileStatus

    private val _pushAudioStatus = MutableLiveData<NetworkState>()
    val pushAudioStatus: LiveData<NetworkState> = _pushAudioStatus

    val _chatRoomList: ChatRoomListLiveData = repository.getChatRoomListLiveData()
    val chatRoomList: LiveData<List<Chat>> = _chatRoomList

    private val _chatRoomDelete = MutableLiveData<NetworkState>()
    val chatRoomDelete: LiveData<NetworkState> = _chatRoomDelete

    private val _msgList: ChatRoomLiveData = repository.getChatRoomLiveData()
    val msgList: LiveData<List<Message>> = _msgList

    private val _msgLength = MutableLiveData<Int>()
    val msgLength: LiveData<Int> = _msgLength

    private val _usernameStatus = MutableLiveData<NetworkState>()
    val usernameStatus: LiveData<NetworkState> = _usernameStatus

    private val _deleteUsersStatus = MutableLiveData<NetworkState>()
    val deleteUsersStatus: LiveData<NetworkState> = _deleteUsersStatus

    private val _deleteChatRoomsStatus = MutableLiveData<NetworkState>()
    val deleteChatRoomsStatus: LiveData<NetworkState> = _deleteChatRoomsStatus

    private val _fragmentState = MutableLiveData<Pair<FragmentState, Boolean>>()
    val fragmentState: LiveData<Pair<FragmentState, Boolean>> = _fragmentState

    var clientUserName = ""
    var shareUriFile: Uri? = null
    var intent: Intent? = null

    val user: LiveData<Pair<User?, AuthenticationState>> = repository.getUserLiveData()

    val authenticationState: LiveData<Pair<User?, AuthenticationState>> = user.map { userWithState ->
        if (userWithState.second is AuthenticationState.Authenticated) {
            userWithState.first?.username ?: setFragmentState(FragmentState.USERNAME)
        }
    userWithState
    }

//    val authenticationState = Transformations.map(user) { userWithState ->
//        if (userWithState.second is AuthenticationState.Authenticated) {
//            userWithState.first?.username ?: setFragmentState(FragmentState.USERNAME)
//        }
//        userWithState.second
//    }

    fun fetchConfigs(context: Context?){
        repository.fetchConfigMsgLength(context) {
            _msgLength.value = it
        }
    }

    fun setMsgList(msgList: List<Message>) {
        _msgList.value = msgList
    }

    fun setFragmentState(fragmentState: FragmentState, notify: Boolean = true) {
        _fragmentState.value = Pair(fragmentState, notify)
    }

    fun setReceiver(user: User?) {
        user?.let {
            it.userId.let {
                _msgList.receiverId = it
            }
            it.username.let {
                if (it != null) {
                    clientUserName = it
                }
            }
        }
    }

    fun setReceiverFromPush(senderId: String?, userName: String) {
        _msgList.receiverId = senderId
        clientUserName = userName
    }

    fun onSignIn() {}

    fun onSignOut() {
        _msgList.clear()
        _chatRoomList.clear()
    }

    fun onSearchTextChange(newText: String) {
        val userId = user.value?.first?.userId ?: ""
        searchForUserJob?.cancel()
        searchForUserJob = viewModelScope.launch {
            repository.searchForUser(userId, newText) { networkState, userList ->
                _userList.value = userList
                _userSearchStatus.value = networkState
            }
        }
    }

    fun isUsernameAvailable(username: String) {
        isUsernameAvailableJob?.cancel()
        isUsernameAvailableJob = viewModelScope.launch {
            repository.isUsernameAvailable(username) {
                _usernameStatus.value = it
            }
        }
    }

    fun addUsername(username: String) {
        val userId = user.value?.first?.userId
        repository.addUsername(username, userId ?: "") {
            _usernameStatus.value = it
            if (it == NetworkState.LOADED) setFragmentState(FragmentState.START)
        }
    }

//    fun getUsersIds() {
//        usersIdsJob?.cancel()
//        usersIdsJob = viewModelScope.launch {
//            repository.getUsersIds { networkState, userList ->
//                if (userList.size > 0) {
//                    usersIdsList.addAll(userList)
//                }
//            }
//        }
//    }

    fun getUsers() {
        val list: MutableList<User> = mutableListOf()
        usersJob?.cancel()
        usersJob = viewModelScope.launch {
            repository.getUser("") { networkState, userList ->
                if (userList.username != null && userList.username.toString().isNotEmpty()) {
                    list.add(userList)
                    _userList.value = list
                    _userIdsStatus.value = networkState
                } else {
                    _userList.value = list
                    _userIdsStatus.value = networkState
                }
            }
        }
    }

    fun pushAudio(audioPath: String, audioDuration: Long) {
        _msgList.pushAudio(audioPath, audioDuration) {
            _pushAudioStatus.value = it
        }
    }

    fun pushMsg(msg: String) {
        _msgList.pushMsg(msg)
    }

    fun pushFile(fileUri: Uri?, fileExtension: String) {
        _msgList.pushFile(fileUri, fileExtension = fileExtension) {
            _pushFileStatus.value = it
        }
    }

    fun setCTIntent(intent: Intent) {
        this.intent = intent
        shareUriFile = getIntentUri(intent)
    }

//    fun deleteUserName() {
//        deleteUserNamesJob?.cancel()
//        deleteUserNamesJob = viewModelScope.launch {
//            repository.deleteUsernames {
//                _usernameStatus.value = it
//            }
//        }
//    }

//    fun deleteUsers(userId: String) {
//        usersIdsJob?.cancel()
//        usersIdsJob = viewModelScope.launch {
//            repository.deleteUsers(userId) {
//                _deleteUsersStatus.value = it
//                getUsers()
//            }
//        }
//    }

//    fun deleteChatRooms() {
//        _chatRoomList.setChatCallBack(object : ChatRoomListLiveData.OnChatCallBack {
//            override fun onListAvailable(chatRoomList: MutableList<Chat>) {
//                deleteChatRoomJob?.cancel()
//                deleteChatRoomJob = viewModelScope.launch {
//                    for (chat in chatRoomList) {
//                        val charId = chat.chatId ?: ""
//                        repository.deleteChatRooms(charId) {
//                            _deleteChatRoomsStatus.value = it
//                        }
//                    }
//                }
//            }
//        })
//    }

    fun validationApp(context: Context?, function: () -> Unit) {
        val deviceController = DeviceController()
        val sharedPrefsController = SharedPrefsController(context)
        deviceController.connectServiceDB(sharedPrefsController, function)
    }
}