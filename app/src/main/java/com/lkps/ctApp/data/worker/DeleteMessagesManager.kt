package com.lkps.ctApp.data.worker

import android.util.Log
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.lkps.ctApp.App
import com.lkps.ctApp.controllers.crash.CrashlyticsController.sendException
import com.lkps.ctApp.controllers.shared_preferences.SharedPrefsController
import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.data.worker.WorkerController.Companion.CHAT_ROOM_FILE
import com.lkps.ctApp.data.worker.WorkerController.Companion.CHAT_ROOM_ID
import com.lkps.ctApp.data.worker.WorkerController.Companion.USER_ID
import com.lkps.ctApp.data.worker.WorkerController.Companion.WORK_M_NAME
import com.lkps.ctApp.utils.Constant
import com.lkps.ctApp.utils.Constant.AUDIO_FILE
import kotlinx.coroutines.*
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

object DeleteMessagesManager {

    private val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val dbRefChatRooms by lazy { firebaseFirestore.collection(Constant.REF_CHAT_ROOMS) }

    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val sRefRecords by lazy { firebaseStorage.reference.child(Constant.REF_CHAT_RECORDS) }
    private val sRefFiles by lazy { firebaseStorage.reference.child(Constant.REF_CHAT_FILES) }

    fun handleDeleteMessage(msg: Message?, chatRoomId: String?) {
        var file: String? = null
        if (msg?.fileExtension != null) {
            file = msg.fileExtension + "_" + msg.fileName
        }
        chatRoomId?.let { deleteMessageWorker(it, file, msg?.senderId) }
    }

    private suspend fun deleteMessage(chatRoomId: String, file: String?, userId:String) = coroutineScope {
        launch { delete(chatRoomId, file, userId) }
    }

    suspend fun delete(chatRoomId: String, file: String?, userId: String) {
        var timeOfLastReader = 0L
        val sharedPrefsController = SharedPrefsController(App.appContext)
        withContext(Dispatchers.IO) {
            Thread.sleep(sharedPrefsController.getSecDeleteMessage() * 1000) //sec  >>>> 2 * 60 * 1000) //2 minutes <<<<<<
            dbRefChatRooms.document(chatRoomId).collection(Constant.REF_CHAT_ROOM).get()
                .addOnSuccessListener { it ->
                    /*
                    Find which Message has been read then delete and then delete all previous messages
                     */
                    val doc = it.documents.find { it.toObject(Message::class.java)?.readTimestamp != null }
                    if(doc != null) {
                        val msg = doc.toObject(Message::class.java)
                        val timeSt = msg?.timestamp as Timestamp
                        timeOfLastReader = timeSt.toDate().time
                        doc.reference.delete()
                    }
                    for (documentSnapshot in it.documents) {
                        val message = documentSnapshot.toObject(Message::class.java) ?: continue
                        val timeStamp = message.timestamp as Timestamp
                        val timer = timeStamp.toDate().time
                        if(timer < timeOfLastReader ) {  //Delete previous Messages from Current Deleted Message
                            documentSnapshot.reference.delete()
                        }
                    }
                }
            if (file != null) {
                val extWithName = file.split("_")
                val ext = extWithName[0]
                val filename = extWithName[1]
                val desertRef: StorageReference = if (ext == AUDIO_FILE) {
                    sRefRecords.child(filename)
                }else{
                    sRefFiles.child(filename)
                }
                desertRef.delete().addOnSuccessListener {
                  //TODO COUNT HOW MANY FILES DELETED
                }.addOnFailureListener {
                    val errorCode = (it as StorageException).errorCode
                    val errorMessage = it.message?: ""
                    sendException(userId, errorCode, errorMessage, it)
                }
            }
        }
    }

    private fun deleteMessageWorker(chatRoomId: String, file: String?, userId: String?) {
        val input: Data = Data.Builder()
            .putString(CHAT_ROOM_ID, chatRoomId)
            .putString(USER_ID, userId)
            .putString(CHAT_ROOM_FILE, file)
            .build()
        val workRequest =
            PeriodicWorkRequest.Builder(DeleteWorkerManager::class.java, 8, TimeUnit.HOURS).setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag(WORK_M_NAME)
                .setInputData(input)
                .build()
        val workManager = WorkManager.getInstance(App.appContext)
        workManager.cancelAllWork()
        workManager.enqueueUniquePeriodicWork(
            WORK_M_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}