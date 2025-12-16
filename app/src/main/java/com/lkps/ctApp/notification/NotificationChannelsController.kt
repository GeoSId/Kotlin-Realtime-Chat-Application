package  com.lkps.ctApp.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.lkps.ct.R

@RequiresApi(Build.VERSION_CODES.O)
class NotificationChannelsController
constructor(base: Context) : ContextWrapper(base) {

    var primaryChannel: String = getString(R.string.app_name)
    var CHANNEL_ID: String = getString(R.string.app_name)+"/"

    private var manager: NotificationManager? = null

    init {
        primaryChannel = getString(R.string.app_name)
        CHANNEL_ID = getString(R.string.app_name)+"/"

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

         val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val soundUri: Uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE+"://" + packageName + "/raw/" + R.raw.sound)
        val channelPrimary = NotificationChannel(CHANNEL_ID, primaryChannel, NotificationManager.IMPORTANCE_DEFAULT)

        channelPrimary.enableLights(true)
        channelPrimary.lightColor = ContextCompat.getColor(this, R.color.colorGreen)

        channelPrimary.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        channelPrimary.enableVibration(true)
        channelPrimary.vibrationPattern = longArrayOf(
            100,
            200,
            300,
            400,
            500,
            400
        )
        channelPrimary.setShowBadge(true)
        try {
            channelPrimary.setSound(soundUri, audioAttributes)
        }catch (e:Exception){
            e.printStackTrace()
        }
        getManager().createNotificationChannel(channelPrimary)
    }

    private fun getManager(): NotificationManager {
        if (manager == null) {
            manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return manager as NotificationManager
    }
}

