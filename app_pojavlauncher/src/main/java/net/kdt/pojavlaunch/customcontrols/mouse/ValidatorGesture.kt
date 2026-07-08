package net.kdt.pojavlaunch.customcontrols.mouse

import android.os.Handler

abstract class ValidatorGesture(private val mHandler: Handler) : Runnable {
    private var mGestureActive = false

    fun submit(): Boolean {
        if (mGestureActive) return false
        mHandler.postDelayed(this, getGestureDelay().toLong())
        mGestureActive = true
        return true
    }

    fun cancel(isSwitching: Boolean) {
        if (!mGestureActive) return
        mHandler.removeCallbacks(this)
        onGestureCancelled(isSwitching)
        mGestureActive = false
    }

    override fun run() {
        if (checkAndTrigger()) return
        mGestureActive = false
        onGestureCancelled(false)
    }

    protected abstract fun getGestureDelay(): Int
    abstract fun checkAndTrigger(): Boolean
    abstract fun onGestureCancelled(isSwitching: Boolean)
}
