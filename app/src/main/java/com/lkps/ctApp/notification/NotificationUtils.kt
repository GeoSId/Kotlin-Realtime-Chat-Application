package com.lkps.ctApp.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lkps.ct.BuildConfig
import com.lkps.ct.R
import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.notification.data.NewMessageNotification
import com.lkps.ctApp.utils.Constant.NOTIFICATION_INTENT
import com.lkps.ctApp.utils.mapper.RemoteMessageMapper
import com.lkps.ctApp.view.MainActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object NotificationUtils {

    fun makeStatusNotification(
            context: Context,
            newNotification: NewMessageNotification
    ) {
        val notification = getNotification(context, newNotification)
        NotificationManagerCompat.from(context).notify(newNotification.notificationId, notification)
    }

    private fun getNotification(context: Context, notification: NewMessageNotification
    ): Notification {
        val mBuilder: NotificationCompat.Builder = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                val notificationChannelsController = NotificationChannelsController(context)
                NotificationCompat.Builder(context, notificationChannelsController.CHANNEL_ID)
            }
            else -> NotificationCompat.Builder(context)
        }
        return mBuilder
                .setStyle(NotificationCompat.BigTextStyle()
                .bigText(notification.message)
                .setBigContentTitle(notification.senderName))
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher))
                .setTicker(notification.senderName)
//                .setSound(defaultSoundUri)
//                .setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
                .setContentTitle(notification.senderName)
                .setContentText(notification.message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(getPendingIntent(context, notification.userString)).build()
    }

    private fun getPendingIntent(context: Context, userString: String): PendingIntent? {
        val resultIntent = Intent(context, MainActivity::class.java)
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        resultIntent.putExtra(NOTIFICATION_INTENT, userString)
        return PendingIntent.getActivity(context, 0, resultIntent,  PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT)
    }

    fun sendRemoteNotification(regToken: String?, message: Message?) {
        GlobalScope.launch {
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            try {
                val messageJson = RemoteMessageMapper.messageToJson(message)
                val client = OkHttpClient()
                val notification = JSONObject()
                notification.put("data", messageJson)
                notification.put("to", regToken)
                val body = notification.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .header(
                        "Authorization",
                        "key=" + BuildConfig.PUSH_API_K
                    )
                    .url("https://fcm.googleapis.com/fcm/send")
                    .post(body)
                    .build()
                val response = client.newCall(request).execute()
                response.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
