package com.lkps.ctApp.notification

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lkps.ct.R
import com.lkps.ctApp.notification.data.NewMessageNotification
import com.lkps.ctApp.utils.Constant.NOTIFICATION_INTENT
import com.lkps.ctApp.view.MainActivity
import timber.log.Timber

/**
 * Utility class for creating and displaying local notifications.
 *
 * This class handles only local notification display. Remote push notifications
 * are handled automatically by Firebase Cloud Functions.
 */
object NotificationUtils {

    /**
     * Displays a local notification for a new message.
     *
     * @param context The application context
     * @param newNotification The notification data to display
     * @return true if the notification was shown, false if permission was denied
     */
    fun makeStatusNotification(
        context: Context,
        newNotification: NewMessageNotification
    ): Boolean {
        if (!hasNotificationPermission(context)) {
            Timber.w("Notification permission not granted, skipping notification")
            return false
        }

        val notification = buildNotification(context, newNotification)
        try {
            NotificationManagerCompat.from(context)
                .notify(newNotification.notificationId, notification)
            return true
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException when showing notification")
            return false
        }
    }

    /**
     * Cancels a notification by its ID.
     *
     * @param context The application context
     * @param notificationId The ID of the notification to cancel
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    /**
     * Cancels all notifications for this app.
     *
     * @param context The application context
     */
    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    /**
     * Checks if the app has notification permission.
     *
     * @param context The application context
     * @return true if permission is granted or not required (Android < 13)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun buildNotification(
        context: Context,
        notification: NewMessageNotification
    ): Notification {
        val builder = createNotificationBuilder(context)

        return builder
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notification.message)
                    .setBigContentTitle(notification.senderName)
            )
            .setSmallIcon(R.drawable.ic_launcher)
            .setLargeIcon(
                BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher)
            )
            .setTicker(notification.senderName)
            .setContentTitle(notification.senderName)
            .setContentText(notification.message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(createPendingIntent(context, notification.userString))
            .build()
    }

    private fun createNotificationBuilder(context: Context): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelsController = NotificationChannelsController(context)
            NotificationCompat.Builder(context, channelsController.CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(context)
        }
    }

    private fun createPendingIntent(context: Context, userString: String): PendingIntent {
        val resultIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(NOTIFICATION_INTENT, userString)
        }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getActivity(
            context,
            userString.hashCode(), // Use unique request code per user
            resultIntent,
            flags
        )
    }
}
