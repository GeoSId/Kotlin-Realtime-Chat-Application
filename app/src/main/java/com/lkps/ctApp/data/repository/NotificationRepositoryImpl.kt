package com.lkps.ctApp.data.repository

import com.lkps.ct.BuildConfig
import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.data.remote.FcmApiService
import com.lkps.ctApp.data.remote.model.FcmRequest
import com.lkps.ctApp.data.remote.model.FcmResponse
import com.lkps.ctApp.data.remote.model.MessagePayload
import com.lkps.ctApp.data.remote.model.NotificationPayload
import com.lkps.ctApp.utils.Constant.FCM_AUTHORIZATION_PREFIX
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation of [NotificationRepository] for FCM push notification operations.
 *
 * @param fcmApiService The Retrofit service for FCM API calls
 * @param ioDispatcher The coroutine dispatcher for IO operations
 */
class NotificationRepositoryImpl @Inject constructor(
    private val fcmApiService: FcmApiService,
    private val ioDispatcher: CoroutineDispatcher
) : NotificationRepository {

    override suspend fun sendRemoteNotification(
        regToken: String,
        message: Message
    ): Result<FcmResponse> = withContext(ioDispatcher) {
        try {
            val request = createFcmRequest(regToken, message)
            val authorization = FCM_AUTHORIZATION_PREFIX + BuildConfig.PUSH_API_K

            Timber.d("Sending FCM notification to token: ${regToken.take(10)}...")

            val response = fcmApiService.sendNotification(authorization, request)

            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null) {
                        if (body.success == 1) {
                            Timber.d("FCM notification sent successfully")
                            Result.success(body)
                        } else {
                            val errorMessage = body.results?.firstOrNull()?.error
                                ?: "Unknown FCM error"
                            Timber.e("FCM notification failed: $errorMessage")
                            Result.failure(FcmException(errorMessage))
                        }
                    } else {
                        Timber.e("FCM response body is null")
                        Result.failure(FcmException("Empty response from FCM"))
                    }
                }
                else -> {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Timber.e("FCM API error: ${response.code()} - $errorBody")
                    Result.failure(
                        FcmException("FCM API error: ${response.code()}")
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to send FCM notification")
            Result.failure(e)
        }
    }

    private fun createFcmRequest(regToken: String, message: Message): FcmRequest {
        val senderName = message.name.orEmpty()
        val notificationBody = getNotificationBody(message)

        return FcmRequest(
            to = regToken,
            notification = NotificationPayload(
                title = senderName,
                body = notificationBody
            ),
            data = MessagePayload(
                id = message.id.orEmpty(),
                senderId = message.senderId.orEmpty(),
                receiverId = message.receiverId.orEmpty(),
                isOwner = message.isOwner ?: false,
                name = senderName,
                photoUrl = message.fileUrl.orEmpty(),
                audioUrl = message.audioUrl.orEmpty(),
                audioFile = message.audioFile.orEmpty(),
                audioDuration = message.audioDuration ?: 0L,
                fileExtension = message.fileExtension.orEmpty(),
                fileName = message.fileName.orEmpty(),
                text = message.text.orEmpty(),
                timestamp = message.timestamp?.toString().orEmpty(),
                readTimestamp = message.readTimestamp?.toString().orEmpty(),
                pdf = message.pdf.orEmpty(),
                photo = message.photo.orEmpty()
            ),
            priority = PRIORITY_HIGH
        )
    }

    /**
     * Generates the notification body text based on message content type.
     */
    private fun getNotificationBody(message: Message): String {
        return when {
            !message.text.isNullOrEmpty() -> message.text!!
            !message.audioUrl.isNullOrEmpty() -> NOTIFICATION_AUDIO_MESSAGE
            message.fileExtension == FILE_EXTENSION_JPG -> NOTIFICATION_PHOTO_MESSAGE
            message.fileExtension == FILE_EXTENSION_PDF -> NOTIFICATION_FILE_MESSAGE
            else -> NOTIFICATION_NEW_MESSAGE
        }
    }

    companion object {
        private const val PRIORITY_HIGH = "high"
        private const val FILE_EXTENSION_JPG = "jpg"
        private const val FILE_EXTENSION_PDF = "pdf"

        // Notification text constants
        private const val NOTIFICATION_AUDIO_MESSAGE = "🎤 Voice message"
        private const val NOTIFICATION_PHOTO_MESSAGE = "📷 Photo"
        private const val NOTIFICATION_FILE_MESSAGE = "📁 File"
        private const val NOTIFICATION_NEW_MESSAGE = "New message"
    }
}

/**
 * Custom exception for FCM-related errors.
 */
class FcmException(message: String) : Exception(message)

