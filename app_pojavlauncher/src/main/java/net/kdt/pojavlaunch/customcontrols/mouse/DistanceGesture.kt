package net.kdt.pojavlaunch.customcontrols.mouse

import android.os.Handler
import net.kdt.pojavlaunch.utils.MathUtils

abstract class DistanceGesture(mHandler: Handler) : ValidatorGesture(mHandler) {
    protected var mGestureTravelX = 0f
    protected var mGestureTravelY = 0f

    fun inputEvent() {
        if (!shouldSubmitGesture()) return
        if (submit()) {
            mGestureTravelX = 0f
            mGestureTravelY = 0f
            onGestureSubmitted()
        }
    }

    fun setMotion(deltaX: Float, deltaY: Float) {
        mGestureTravelX += deltaX
        mGestureTravelY += deltaY
    }

    protected fun travelBelowThreshold(th: Float): Boolean {
        return MathUtils.dist(mGestureTravelX, mGestureTravelY) <= th
    }

    abstract fun onGestureSubmitted()
    abstract fun shouldSubmitGesture(): Boolean
}
