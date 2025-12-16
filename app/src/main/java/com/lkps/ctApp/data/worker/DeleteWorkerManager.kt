package com.lkps.ctApp.data.worker

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lkps.ctApp.data.worker.DeleteMessagesManager.delete
import com.lkps.ctApp.data.worker.WorkerController.Companion.CHAT_ROOM_FILE
import com.lkps.ctApp.data.worker.WorkerController.Companion.CHAT_ROOM_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteWorkerManager(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    @SuppressLint("RestrictedApi")
    override suspend fun doWork(): Result {
        val chatRoomId = inputData.getString(CHAT_ROOM_ID) ?: "-1"
        val file = inputData.getString(CHAT_ROOM_FILE)
        val userId = inputData.getString(WorkerController.USER_ID)?:"-1"
        delete(chatRoomId, file, userId)
        return withContext(Dispatchers.IO) {
            Result.success()
        }
    }
}