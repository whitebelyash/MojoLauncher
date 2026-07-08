package net.kdt.pojavlaunch.lifecycle

import android.app.Activity
import android.app.Application
import net.kdt.pojavlaunch.Tools
import java.lang.ref.WeakReference

object ContextExecutor {
    private var sApplication: WeakReference<Application>? = null
    private var sActivity: WeakReference<Activity>? = null

    fun execute(contextExecutorTask: ContextExecutorTask) {
        Tools.runOnUiThread { executeOnUiThread(contextExecutorTask) }
    }

    fun executeActivity(activityRunnable: ActivityRunnable) {
        Tools.runOnUiThread {
            val activity = Tools.getWeakReference(sActivity)
            if (activity != null) activityRunnable.executeWithActivity(activity)
        }
    }

    private fun executeOnUiThread(contextExecutorTask: ContextExecutorTask) {
        val activity = Tools.getWeakReference(sActivity)
        if (activity != null) {
            contextExecutorTask.executeWithActivity(activity)
            return
        }
        val application = Tools.getWeakReference(sApplication)
        if (application != null) {
            contextExecutorTask.executeWithApplication(application)
        } else {
            throw RuntimeException("ContextExecutor.execute() called before Application.onCreate!")
        }
    }

    fun setActivity(activity: Activity) {
        sActivity = WeakReference(activity)
    }

    fun clearActivity() {
        sActivity?.clear()
    }

    fun setApplication(application: Application) {
        sApplication = WeakReference(application)
    }

    fun clearApplication() {
        sApplication?.clear()
    }
}
