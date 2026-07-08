package net.kdt.pojavlaunch.customcontrols.mouse

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import net.kdt.pojavlaunch.prefs.LauncherPreferences

class InGameEventProcessor : TouchEventProcessor {
    private val mGestureHandler = Handler(Looper.getMainLooper())
    private val mSensitivity: Double
    private var mEventTransitioned = true
    private val mTracker = PointerTracker()
    private val mLeftClickGesture = LeftClickGesture(mGestureHandler)
    private val mRightClickGesture = RightClickGesture(mGestureHandler)

    constructor(hostView: View, sensitivity: Double) : super(hostView) {
        mSensitivity = sensitivity
    }

    override fun processTouchEvent(motionEvent: MotionEvent): Boolean {
        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mTracker.startTracking(motionEvent)
                if (LauncherPreferences.PREF_DISABLE_GESTURES) break
                mEventTransitioned = false
                checkGestures()
            }
            MotionEvent.ACTION_MOVE -> {
                mTracker.trackEvent(motionEvent)
                val motionVector = mTracker.getMotionVector()
                val deltaX = (motionVector[0] * mSensitivity).toFloat()
                val deltaY = (motionVector[1] * mSensitivity).toFloat()
                mLeftClickGesture.setMotion(deltaX, deltaY)
                mRightClickGesture.setMotion(deltaX, deltaY)
                applyMoveVector(deltaX, deltaY)
                if (LauncherPreferences.PREF_DISABLE_GESTURES) break
                checkGestures()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mTracker.cancelTracking()
                cancelGestures(false)
            }
        }
        return true
    }

    override fun cancelPendingActions() {
        cancelGestures(true)
    }

    private fun checkGestures() {
        mLeftClickGesture.inputEvent()
        if (!mEventTransitioned) mRightClickGesture.inputEvent()
    }

    private fun cancelGestures(isSwitching: Boolean) {
        mEventTransitioned = true
        mLeftClickGesture.cancel(isSwitching)
        mRightClickGesture.cancel(isSwitching)
    }
}
