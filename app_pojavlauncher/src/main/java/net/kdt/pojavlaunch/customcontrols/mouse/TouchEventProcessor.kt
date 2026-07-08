package net.kdt.pojavlaunch.customcontrols.mouse

import android.view.MotionEvent
import android.view.View
import git.artdeell.dnbootstrap.glfw.GLFW
import net.kdt.pojavlaunch.prefs.LauncherPreferences

abstract class TouchEventProcessor(private val mHostView: View) {
    protected fun sendTouchCoordinates(x: Float, y: Float) {
        GLFW.cursorX = x / mHostView.width
        GLFW.cursorY = y / mHostView.height
        GLFW.sendMousePos()
    }

    protected fun applyMoveVector(vector: FloatArray) {
        applyMoveVector(vector[0], vector[1])
    }

    protected fun applyMoveVector(x: Float, y: Float) {
        GLFW.cursorX += x * LauncherPreferences.PREF_MOUSESPEED / mHostView.width
        GLFW.cursorY += y * LauncherPreferences.PREF_MOUSESPEED / mHostView.height
        GLFW.sendMousePos()
    }

    abstract fun processTouchEvent(motionEvent: MotionEvent): Boolean
    abstract fun cancelPendingActions()
}
