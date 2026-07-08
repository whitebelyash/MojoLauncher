package net.kdt.pojavlaunch.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener
import net.kdt.pojavlaunch.utils.NotificationUtils

class ProgressService : Service(), TaskCountListener {
    private lateinit var notificationManagerCompat: NotificationManagerCompat
    private var mNotificationBuilder: NotificationCompat.Builder? = null

    override fun onCreate() {
        Tools.buildNotificationChannel(applicationContext)
        notificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        val killIntent = Intent(applicationContext, ProgressService::class.java)
        killIntent.putExtra("kill", true)
        val pendingKillIntent = PendingIntent.getService(
            this, NotificationUtils.PENDINGINTENT_CODE_KILL_PROGRESS_SERVICE,
            killIntent, if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        mNotificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle(getString(R.string.lazy_service_default_title))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_terminate), pendingKillIntent)
            .setSmallIcon(R.drawable.notif_icon)
            .setNotificationSilent()
    }

    @SuppressLint("StringFormatInvalid")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.getBooleanExtra("kill", false)) {
            stopSelf()
            Process.killProcess(Process.myPid())
            return START_NOT_STICKY
        }
        Log.d("ProgressService", "Started!")
        mNotificationBuilder!!.setContentText(getString(R.string.progresslayout_tasks_in_progress, ProgressKeeper.getTaskCount()))
        val notification = mNotificationBuilder!!.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationUtils.NOTIFICATION_ID_PROGRESS_SERVICE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID_PROGRESS_SERVICE, notification)
        }
        if (ProgressKeeper.getTaskCount() < 1) stopSelf()
        else ProgressKeeper.addTaskCountListener(this, false)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        ProgressKeeper.removeTaskCountListener(this)
    }

    override fun onUpdateTaskCount(taskCount: Int): Boolean {
        Tools.MAIN_HANDLER.post {
            if (taskCount > 0) {
                mNotificationBuilder!!.setContentText(getString(R.string.progresslayout_tasks_in_progress, taskCount))
                notificationManagerCompat.notify(1, mNotificationBuilder!!.build())
            } else {
                stopSelf()
            }
        }
        return false
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        super.onTimeout(startId, fgsType)
        stopForeground(true)
        stopSelf()
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, ProgressService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
