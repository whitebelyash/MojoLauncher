package net.kdt.pojavlaunch.customcontrols.mouse

import android.os.Handler
import net.kdt.pojavlaunch.CallbackBridge
import net.kdt.pojavlaunch.LwjglGlfwKeycode

class RightClickGesture(mHandler: Handler) : DistanceGesture(mHandler) {
    private var mGestureEnabled = true
    private var mGestureValid = true

    override fun onGestureSubmitted() {
        mGestureEnabled = false
        mGestureValid = true
    }

    override fun shouldSubmitGesture(): Boolean = mGestureEnabled

    override fun getGestureDelay(): Int = 150

    override fun checkAndTrigger(): Boolean {
        mGestureValid = false
        return true
    }

    override fun onGestureCancelled(isSwitching: Boolean) {
        mGestureEnabled = true
        if (!mGestureValid || isSwitching) return
        val fingerStill = travelBelowThreshold(LeftClickGesture.FINGER_STILL_THRESHOLD.toFloat())
        if (!fingerStill) return
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, true)
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, false)
    }
}
