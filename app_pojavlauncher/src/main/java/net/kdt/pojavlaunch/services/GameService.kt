package net.kdt.pojavlaunch.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.MainActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.NotificationUtils
import java.lang.ref.WeakReference

class GameService : Service() {
    private val mLocalBinder = LocalBinder()

    override fun onCreate() {
        Tools.buildNotificationChannel(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.getBooleanExtra("kill", false)) {
            stopSelf()
            Process.killProcess(Process.myPid())
            return START_NOT_STICKY
        }
        val killIntent = Intent(applicationContext, GameService::class.java)
        killIntent.putExtra("kill", true)
        val pendingKillIntent = PendingIntent.getService(
            this, NotificationUtils.PENDINGINTENT_CODE_KILL_GAME_SERVICE,
            killIntent, if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle(getString(R.string.lazy_service_default_title))
            .setContentText(getString(R.string.notification_game_runs))
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_terminate), pendingKillIntent)
            .setSmallIcon(R.drawable.notif_icon)
            .setNotificationSilent()
        val notification = notificationBuilder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationUtils.NOTIFICATION_ID_GAME_SERVICE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID_GAME_SERVICE, notification)
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        Process.killProcess(Process.myPid())
    }

    override fun onBind(intent: Intent?): IBinder = mLocalBinder

    class LocalBinder : Binder() {
        var isActive = false
    }

    companion object {
        private val sGameService = WeakReference<Service>(null)
    }
}
