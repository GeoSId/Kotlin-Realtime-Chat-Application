package com.lkps.ctApp.data.source.firebase

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.lkps.ctApp.App
import com.lkps.ctApp.data.model.Chat
import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.data.model.User
import com.lkps.ctApp.data.worker.DeleteMessagesManager.handleDeleteMessage
import com.lkps.ctApp.utils.Constant.REF_CHAT_FILES
import com.lkps.ctApp.utils.Constant.REF_CHAT_RECORDS
import com.lkps.ctApp.utils.Constant.REF_CHAT_ROOM
import com.lkps.ctApp.utils.Constant.REF_CHAT_ROOMS
import com.lkps.ctApp.utils.Constant.REF_READ_TIME_STAMP
import com.lkps.ctApp.utils.Constant.REF_TIME_STAMP
import com.lkps.ctApp.utils.Constant.REF_USERS
import com.lkps.ctApp.utils.DateUtils.Companion.getFormatTime
import com.lkps.ctApp.utils.Utility.getTimeStamp
import com.lkps.ctApp.utils.states.NetworkState
import timber.log.Timber
import java.io.File
import kotlin.properties.Delegates
import com.lkps.ctApp.utils.FileHelper

class ChatRoomLiveData : MutableLiveData<List<Message>>() {

    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }

    private val dbRefChatRooms by lazy { firebaseFirestore.collection(REF_CHAT_ROOMS) }
    private val dbRefUsers by lazy { firebaseFirestore.collection(REF_USERS) }
    private val sRefFiles by lazy { firebaseStorage.reference.child(REF_CHAT_FILES) }
    private val sRefRecords by lazy { firebaseStorage.reference.child(REF_CHAT_RECORDS) }

    private var chatRoomLr: ListenerRegistration? = null

    var user: User? = null
    var counter = 0
    private var chatRoomId: String? = null
    private var isNewSenderChatRoom: Boolean? = null
    private var isNewReceiverChatRoom: Boolean? = null

    var receiverId by Delegates.observable<String?>(null) { _, _, newValue ->
        if (newValue != null) {
            App.receiverId = newValue
            setChatRoomId(newValue)
//            onInactive()
//            onActive()

            chatRoomLr?.remove()
            chatRoomLr = null
            chatRoomLr = chatRoomId?.let {
                getChatRoomRef(it).addSnapshotListener(snapshotListener)
            }
        }
    }

    private var snapshotListener = object : EventListener<QuerySnapshot> {
        override fun onEvent(
            querySnapshot: QuerySnapshot?,
            firebaseFirestoreException: FirebaseFirestoreException?
        ) {
            querySnapshot ?: return
            firebaseFirestoreException?.let {
                return@onEvent
            }

            val msgList = mutableListOf<Message>()
            counter = 0
            val msgListSize = querySnapshot.documents.size
            if(msgListSize == 0){
                value = mutableListOf()
                return
            }
            for (documentSnapshot in querySnapshot.documents) {
                val msg = documentSnapshot.toObject(Message::class.java) ?: continue
                msg.setMessageId()
                msg.isOwner = msg.senderId.equals(user?.userId.toString())
                msgList.add(msg)
                val isSameNameWithMessage = msg.name.equals(user?.username)
                //TODO CHECK CASHE
                counter++
                if(((msg.readTimestamp == null) && (msg.isOwner == false)
                    && (querySnapshot.documents.size == counter) && !isSameNameWithMessage)){
                    updateChatRoomWithReadTime(documentSnapshot = documentSnapshot)
                    clearChat(msg)
                }
            }
            value = msgList
        }
    }

    private fun updateChatRoomWithReadTime(documentSnapshot: DocumentSnapshot){
        val map = mutableMapOf<String, Any>()
        map[REF_READ_TIME_STAMP] = getFormatTime(getTimeStamp())
        documentSnapshot.reference.update(map)
    }

    private fun getChatRoomRef(chatRoomId: String): Query {
        // TODO editable messages
        return dbRefChatRooms.document(chatRoomId).collection(REF_CHAT_ROOM)
            .orderBy(REF_TIME_STAMP, Query.Direction.ASCENDING)
    }

    private fun setChatRoomId(receiverId: String) {
        val userId = user?.userId ?: return
        val roomId = if (receiverId > userId) "${receiverId}_$userId" else "${userId}_$receiverId"
        chatRoomId = roomId
        isNewSenderChatRoom = null
        getUserChatRoom(userId, receiverId, roomId)
    }

    private fun getUserChatRoom(userId: String, receiverId: String, chatRoomId: String) {
        dbRefUsers.document(userId).collection(REF_CHAT_ROOMS).document(chatRoomId).get()
            .addOnSuccessListener { documentSnapshot ->
                isNewSenderChatRoom = !documentSnapshot.exists()
            }
            .addOnFailureListener {
            }

        dbRefUsers.document(receiverId).collection(REF_CHAT_ROOMS).document(chatRoomId).get()
            .addOnSuccessListener { documentSnapshot ->
                isNewReceiverChatRoom = !documentSnapshot.exists()
            }
            .addOnFailureListener {
            }
    }

    private fun addSenderChatRoom() {
        val chatRoomId = chatRoomId ?: return
        val chatSender = Chat(chatRoomId, user?.userId, receiverId)
        dbRefUsers.document(user?.userId!!).collection(REF_CHAT_ROOMS).document(chatRoomId)
            .set(chatSender)
            .addOnSuccessListener {
                isNewSenderChatRoom = false
            }
            .addOnFailureListener {
            }
    }

    private fun addReceiverChatRoom() {
        val chatRoomId = chatRoomId ?: return
        val chatReceiver = Chat(chatRoomId, receiverId, user?.userId)
        dbRefUsers.document(receiverId!!).collection(REF_CHAT_ROOMS).document(chatRoomId)
            .set(chatReceiver)
            .addOnSuccessListener {
                isNewReceiverChatRoom = false
            }
            .addOnFailureListener {
            }
    }

    private fun clearChat(msg: Message) {
        handleDeleteMessage(msg, chatRoomId)
    }

    fun pushMsg(
        msg: String? = null,
        fileUrl: String? = null,
        audioUrl: String? = null,
        audioFile: String? = null,
        audioDuration: Long? = null,
        fileExtension: String? = null,
        fileName: String? = null,
        callBack: ((usernameStatus: NetworkState, message: Message?) -> Unit)? = null
    ) {
        val chatRoomId = chatRoomId ?: run {
            callBack?.invoke(NetworkState.FAILED, null)
            return@pushMsg
        }
        val friendlyMessage = Message(
            senderId = user?.userId,
            receiverId = receiverId,
            name = user?.username,
            fileUrl = fileUrl,
            audioUrl = audioUrl,
            audioFile = audioFile,
            audioDuration = audioDuration,
            fileExtension = fileExtension,
            fileName = fileName,
            text = msg,
            timestamp = FieldValue.serverTimestamp()
        )
        dbRefChatRooms.document(chatRoomId).collection(REF_CHAT_ROOM).document()
            .set(friendlyMessage)
            .addOnSuccessListener {
                isNewSenderChatRoom?.let { if (it) addSenderChatRoom() }
                isNewReceiverChatRoom?.let { if (it) addReceiverChatRoom() }
                callBack?.invoke(NetworkState.LOADED, friendlyMessage)
                // Push notifications are now handled automatically by Firebase Cloud Functions
                // when a new message document is created in Firestore
                Timber.d("Message sent successfully. Cloud Function will handle push notification.")
            }
            .addOnFailureListener {
                callBack?.invoke(NetworkState.FAILED, null)
            }
    }

    fun pushAudio(
        audioPath: String,
        audioDuration: Long,
        callBack: (usernameStatus: NetworkState) -> Unit
    ) {
        callBack(NetworkState.LOADING)
        val selectedImageUri = Uri.fromFile(File(audioPath))
        val fileNameTimeStamp = getTimeStamp().toString()
        val lastPathSegment = selectedImageUri.lastPathSegment ?: ""
        sRefRecords.child(fileNameTimeStamp).putFile(selectedImageUri)
            .addOnSuccessListener { taskSnapshot ->
                val urlTask = taskSnapshot.storage.downloadUrl
                urlTask.addOnSuccessListener { uri ->
                    pushMsg(
                        audioUrl = uri.toString(),
                        audioFile = audioPath,
                        audioDuration = audioDuration,
                        fileExtension = lastPathSegment.substring(lastPathSegment.lastIndexOf(".") + 1),
                        fileName = fileNameTimeStamp
                    ) { status, message -> callBack(status) }
                }.addOnFailureListener {
                    callBack(NetworkState.FAILED)
                }
            }.addOnFailureListener { callBack(NetworkState.FAILED) }
    }

    fun pushFile(
        fileUri: Uri?,
        fileExtension: String,
        message: String? = null,
        callBack: (usernameStatus: NetworkState, sentMessage: Message?) -> Unit
    ) {
        callBack(NetworkState.LOADING, null)
        val fileNameTimeStamp = getTimeStamp().toString()

        if (fileUri != null && fileUri.toString().isNotEmpty()) {
            try {
                val uploadTask = if (fileUri.scheme == "content" && fileUri.authority == FileHelper.AUTHORITY) {
                    // This is a FileProvider URI from camera, convert to file URI for Firebase
                    val file = File(FileHelper.currentPhotoPath)
                    if (file.exists()) {
                        sRefFiles.child(fileNameTimeStamp).putFile(Uri.fromFile(file))
                    } else {
                        callBack(NetworkState.FAILED, null)
                        return
                    }
                } else {
                    // Regular URI (from gallery)
                    sRefFiles.child(fileNameTimeStamp).putFile(fileUri)
                }

                uploadTask
                    .addOnSuccessListener { taskSnapshot ->
                        val urlTask = taskSnapshot.storage.downloadUrl
                        urlTask.addOnSuccessListener { uri ->
                            // Create the message object first so we can return it in the callback
                            val fileMessage = Message(
                                senderId = user?.userId,
                                receiverId = receiverId,
                                name = user?.username,
                                fileUrl = uri.toString(),
                                fileExtension = fileExtension,
                                fileName = fileNameTimeStamp,
                                text = message,
                                timestamp = FieldValue.serverTimestamp()
                            )

                            pushMsg(
                                msg = message,
                                fileUrl = uri.toString(),
                                fileExtension = fileExtension,
                                fileName = fileNameTimeStamp
                            )
                            { status, sentMessage ->
                                if (status == NetworkState.LOADED) {
                                    callBack(NetworkState.LOADED, fileMessage)
                                } else {
                                    callBack(status, null)
                                }
                            }
                        }.addOnFailureListener {
                            callBack(NetworkState.FAILED, null)
                        }
                    }.addOnFailureListener { exception ->
                        callBack(NetworkState.FAILED, null)
                    }
            } catch (e: Exception) {
                callBack(NetworkState.FAILED, null)
            }
        } else {
            callBack(NetworkState.FAILED, null)
        }
    }

    override fun onActive() {
        Log.e("ChatRoomLiveData", "onActive")
    }

    public override fun onInactive() {
        chatRoomLr?.remove()
        chatRoomLr = null
    }
}