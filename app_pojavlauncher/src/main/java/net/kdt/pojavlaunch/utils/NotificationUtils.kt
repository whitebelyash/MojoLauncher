package net.kdt.pojavlaunch.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import git.artdeell.mojo.R

object NotificationUtils {
    const val NOTIFICATION_ID_PROGRESS_SERVICE = 1
    const val NOTIFICATION_ID_GAME_SERVICE = 2
    const val NOTIFICATION_ID_DOWNLOAD_LISTENER = 3
    const val NOTIFICATION_ID_SHOW_ERROR = 4
    const val NOTIFICATION_ID_GAME_START = 5
    const val PENDINGINTENT_CODE_KILL_PROGRESS_SERVICE = 1
    const val PENDINGINTENT_CODE_KILL_GAME_SERVICE = 2
    const val PENDINGINTENT_CODE_DOWNLOAD_SERVICE = 3
    const val PENDINGINTENT_CODE_SHOW_ERROR = 4
    const val PENDINGINTENT_CODE_GAME_START = 5

    fun sendBasicNotification(context: Context, contentTitle: Int, contentText: Int, actionIntent: Intent?,
                              pendingIntentCode: Int, notificationId: Int) {
        val pendingIntent = PendingIntent.getActivity(context, pendingIntentCode, actionIntent,
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(context, context.getString(R.string.notif_channel_id))
        if (contentTitle != -1) notificationBuilder.setContentTitle(context.getString(contentTitle))
        if (contentText != -1) notificationBuilder.setContentText(context.getString(contentText))
        if (actionIntent != null) notificationBuilder.setContentIntent(pendingIntent)
        notificationBuilder.setSmallIcon(R.drawable.notif_icon)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
