package net.kdt.pojavlaunch.customcontrols.mouse

import android.os.Handler
import net.kdt.pojavlaunch.CallbackBridge.sendMouseButton
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.MathUtils

class LeftClickGesture(handler: Handler) : DistanceGesture(handler) {
    companion object {
        val FINGER_STILL_THRESHOLD = Tools.dpToPx(9f).toInt()

        fun isFingerStill(startX: Float, startY: Float, endX: Float, endY: Float, threshold: Float): Boolean {
            return MathUtils.dist(endX, endY, startX, startY) <= threshold
        }
    }

    private var mMouseActivated = false

    override fun onGestureSubmitted() {}

    override fun shouldSubmitGesture(): Boolean = true

    override fun getGestureDelay(): Int = LauncherPreferences.PREF_LONGPRESS_TRIGGER

    override fun checkAndTrigger(): Boolean {
        val fingerStill = travelBelowThreshold(FINGER_STILL_THRESHOLD.toFloat())
        if (fingerStill) {
            sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, true)
            mMouseActivated = true
        }
        return true
    }

    override fun onGestureCancelled(isSwitching: Boolean) {
        if (mMouseActivated) {
            sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, false)
            mMouseActivated = false
        }
    }
}
