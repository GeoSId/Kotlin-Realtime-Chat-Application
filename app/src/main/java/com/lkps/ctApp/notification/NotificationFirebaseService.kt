package com.lkps.ctApp.notification

import com.lkps.ctApp.App
import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.notification.data.NewMessageNotification
import com.lkps.ctApp.utils.convertToString
import com.lkps.ctApp.utils.mapper.RemoteMessageMapper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationFirebaseService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        remoteMessage.data.isNotEmpty().let {
            val msg = RemoteMessageMapper.map(remoteMessage.data)
            if (msg.senderId == App.receiverId) return
            createNotifications(msg)
        }
    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if(it.isComplete){
                val refreshedToken = it.result.toString() ?: return@addOnCompleteListener
            }
        }
    }

    private fun createNotifications(message: Message) {
        message.senderId ?: return
        val newMessageNotification = NewMessageNotification(
                notificationId = message.senderId.hashCode(),
                userString = convertToString(message),
                senderName = message.name,
                message = getNotificationText(message)
        )
        NotificationUtils.makeStatusNotification(applicationContext, newMessageNotification)
    }

    private fun getNotificationText(message: Message): String {
        var text = ""
        message.text?.let { if (it.isNotEmpty()) text = it }
        message.audioUrl?.let {
            if (it.isNotEmpty()) text = "\uD83C\uDFA4 Record"
        }
        if(message.fileExtension.equals("jpg")){
             text = "\uD83D\uDCF7 Photo"
        }else if(message.fileExtension.equals("pdf")){
            text =  "\uD83D\uDCC1 File"
        }
        return text
    }
}