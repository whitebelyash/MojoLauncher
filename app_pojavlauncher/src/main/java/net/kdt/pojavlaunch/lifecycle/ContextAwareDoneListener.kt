package net.kdt.pojavlaunch.lifecycle

import net.kdt.pojavlaunch.MainActivity.INTENT_LAUNCH_CLASSPATH
import net.kdt.pojavlaunch.MainActivity.INTENT_LAUNCH_VERSION
import android.app.Activity
import android.content.Context
import android.content.Intent
import net.kdt.pojavlaunch.MainActivity
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.tasks.MoJsonExtras
import net.kdt.pojavlaunch.utils.NotificationUtils
import java.io.File

class ContextAwareDoneListener(
    baseContext: Context,
    private val mNormalizedVersionid: String
) : MoJsonExtras.DoneListener, ContextExecutorTask {

    private val mErrorString: String = baseContext.getString(R.string.mc_download_failed)
    private var classpath: Array<File>? = null

    private fun createGameStartIntent(context: Context): Intent {
        val mainIntent = Intent(context, MainActivity::class.java)
        mainIntent.putExtra(INTENT_LAUNCH_VERSION, mNormalizedVersionid)
        mainIntent.putExtra(INTENT_LAUNCH_CLASSPATH, classpath)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return mainIntent
    }

    override fun onDownloadDone(classpath: Array<File>) {
        this.classpath = classpath
        ProgressKeeper.waitUntilDone { ContextExecutor.execute(this) }
    }

    override fun onDownloadFailed(throwable: Throwable) {
        Tools.showErrorRemote(mErrorString, throwable)
    }

    override fun executeWithActivity(activity: Activity) {
        try {
            val gameStartIntent = createGameStartIntent(activity)
            activity.startActivity(gameStartIntent)
            activity.finish()
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (e: Throwable) {
            Tools.showError(activity.baseContext, e)
        }
    }

    override fun executeWithApplication(context: Context) {
        val gameStartIntent = createGameStartIntent(context)
        NotificationUtils.sendBasicNotification(
            context,
            R.string.notif_download_finished,
            R.string.notif_download_finished_desc,
            gameStartIntent,
            NotificationUtils.PENDINGINTENT_CODE_GAME_START,
            NotificationUtils.NOTIFICATION_ID_GAME_START
        )
    }
}
