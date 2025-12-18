package com.lkps.ctApp.data.source.firebase

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.lkps.ctApp.data.model.Chat
import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.data.model.User
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.lkps.ctApp.utils.Constant.REF_CHAT_ROOM
import com.lkps.ctApp.utils.Constant.REF_CHAT_ROOMS
import com.lkps.ctApp.utils.Constant.REF_TIME_STAMP
import com.lkps.ctApp.utils.Constant.REF_USERS

class ChatRoomListLiveData : MutableLiveData<List<Chat>>() {

    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val dbRefChatRooms by lazy { firebaseFirestore.collection(REF_CHAT_ROOMS) }
    private val dbRefUsers by lazy { firebaseFirestore.collection(REF_USERS) }

    private var userChatRoomsLr: ListenerRegistration? = null

    private var onChatCallBack : OnChatCallBack ?= null

    interface OnChatCallBack {
        fun onListAvailable(chatRoomList: MutableList<Chat>)
    }

    fun setChatCallBack(onChatCallBack : OnChatCallBack){
        this.onChatCallBack = onChatCallBack
    }

    private var userObserver = Observer<User?> { }
    private var newMsgObserver = Observer<Message?> {
        sortChatRooms()
    }

    private var user: User? = null

    private val snapshotListener = object : EventListener<QuerySnapshot> {
        override fun onEvent(querySnapshot: QuerySnapshot?, firebaseFirestoreException: FirebaseFirestoreException?) {
            firebaseFirestoreException?.let { return@onEvent }
            val chatRoomList = querySnapshot?.toObjects(Chat::class.java)
            loadChatRooms(chatRoomList as MutableList<Chat>)
            chatRoomList.let { value = it }
        }
    }

    fun setNewDocRef(user: User) {
        if (this.user?.userId == user.userId) return
        onInactive()
        this.user = user
        onActive()
    }

    private fun getChatRoomListRef(userId: String): CollectionReference {
        return dbRefUsers.document(userId).collection(REF_CHAT_ROOMS)
    }

    fun loadChatRooms(chatRoomList: MutableList<Chat>) {
        onChatCallBack?.onListAvailable(chatRoomList)
        for (chat in chatRoomList) {
            val isReceiver = chat.receiverId != user?.userId
            val receiverId = if (isReceiver) chat.receiverId else chat.senderId
            receiverId?.let { chat.user = ReceiverLiveData(getReceiver(receiverId)) }
            chat.chatId?.let { chat.message = MessageLiveData(getSingleMsgListener(it)) }
            chat.user?.observeForever(userObserver)
            chat.message?.observeForever(newMsgObserver)
        }
    }

    private fun getReceiver(receiverId: String): DocumentReference {
        return dbRefUsers.document(receiverId)
    }

    private fun getSingleMsgListener(chatId: String): Query {
        return dbRefChatRooms.document(chatId).collection(REF_CHAT_ROOM)
            .orderBy(REF_TIME_STAMP, Query.Direction.DESCENDING)
            .limit(1)
    }

    private fun sortChatRooms() { //todo may sort by timeStamp
//        val sortedList = value?.sortedByDescending {
//            val timestamp = it.message?.value?.timestamp
//            if (timestamp is Timestamp) timestamp.seconds else 0
//        }?.toMutableList()
        value.let { value = it }
    }

    override fun onActive() {
        val userId = user?.userId ?: return
        if(userChatRoomsLr != null){
            onInactive()
        }
        userChatRoomsLr = getChatRoomListRef(userId).addSnapshotListener(snapshotListener)
    }

    override fun onInactive() {
        userChatRoomsLr?.remove()
        userChatRoomsLr = null
        value?.let {
            for (chat in it) {
                chat.user?.removeObserver(userObserver)
                chat.message?.removeObserver(newMsgObserver)
            }
        }
    }
}