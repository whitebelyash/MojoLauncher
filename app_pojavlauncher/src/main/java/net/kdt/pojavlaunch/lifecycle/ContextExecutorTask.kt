package net.kdt.pojavlaunch.lifecycle

import android.content.Context

interface ContextExecutorTask : ActivityRunnable {
    fun executeWithApplication(context: Context)
}
