package com.lkps.ctApp.notification.data

data class NewMessageNotification(
    val notificationId: Int,
    val userString: String,
    val senderName: String?,
    val senderPhotoUrl: String? = "",
    val message: String?
)