package net.kdt.pojavlaunch.lifecycle

import android.app.Activity

fun interface ActivityRunnable {
    fun executeWithActivity(activity: Activity)
}
