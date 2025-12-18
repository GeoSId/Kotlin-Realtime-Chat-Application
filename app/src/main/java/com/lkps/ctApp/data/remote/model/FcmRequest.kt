package com.lkps.ctApp.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * FCM push notification request body.
 *
 * @param to The recipient device FCM token
 * @param notification Notification payload for background display
 * @param data Custom data payload to include in the notification
 * @param priority Message priority (high ensures immediate delivery)
 * @param android Android-specific notification options
 */
data class FcmRequest(
    @SerializedName("to")
    val to: String,
    @SerializedName("notification")
    val notification: NotificationPayload,
    @SerializedName("data")
    val data: MessagePayload,
    @SerializedName("priority")
    val priority: String = "high",
    @SerializedName("android")
    val android: AndroidConfig = AndroidConfig()
)

/**
 * Notification payload for FCM.
 * This is automatically displayed by the system when app is in background.
 */
data class NotificationPayload(
    @SerializedName("title")
    val title: String,
    @SerializedName("body")
    val body: String,
    @SerializedName("sound")
    val sound: String = "default",
    @SerializedName("click_action")
    val clickAction: String = "OPEN_CHAT_ACTIVITY"
)

/**
 * Android-specific FCM configuration.
 */
data class AndroidConfig(
    @SerializedName("priority")
    val priority: String = "high",
    @SerializedName("notification")
    val notification: AndroidNotification = AndroidNotification()
)

/**
 * Android-specific notification options.
 */
data class AndroidNotification(
    @SerializedName("sound")
    val sound: String = "default",
    @SerializedName("default_vibrate_timings")
    val defaultVibrateTimings: Boolean = true,
    @SerializedName("default_light_settings")
    val defaultLightSettings: Boolean = true
)

/**
 * Message payload for FCM data notification.
 */
data class MessagePayload(
    @SerializedName("id")
    val id: String,
    @SerializedName("senderId")
    val senderId: String,
    @SerializedName("receiverId")
    val receiverId: String,
    @SerializedName("isOwner")
    val isOwner: Boolean,
    @SerializedName("name")
    val name: String,
    @SerializedName("photoUrl")
    val photoUrl: String,
    @SerializedName("audioUrl")
    val audioUrl: String,
    @SerializedName("audioFile")
    val audioFile: String,
    @SerializedName("audioDuration")
    val audioDuration: Long,
    @SerializedName("fileExtension")
    val fileExtension: String,
    @SerializedName("fileName")
    val fileName: String,
    @SerializedName("text")
    val text: String,
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("readTimestamp")
    val readTimestamp: String,
    @SerializedName("pdf")
    val pdf: String,
    @SerializedName("photo")
    val photo: String
)

