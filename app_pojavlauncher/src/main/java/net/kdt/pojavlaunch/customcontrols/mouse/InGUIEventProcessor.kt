package net.kdt.pojavlaunch.customcontrols.mouse

import android.view.MotionEvent
import android.view.View
import net.kdt.pojavlaunch.CallbackBridge
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences

class InGUIEventProcessor : TouchEventProcessor {
    companion object {
        val FINGER_SCROLL_THRESHOLD = Tools.dpToPx(6f)
        val FINGER_STILL_THRESHOLD = Tools.dpToPx(5f)
    }

    private val mTracker = PointerTracker()
    private val mSingleTapDetector: TapDetector
    private var mTouchpad: View? = null
    private var mIsMouseDown = false
    private var mStartX = 0f
    private var mStartY = 0f
    private val mScroller = Scroller(FINGER_SCROLL_THRESHOLD)

    constructor(hostView: View) : super(hostView) {
        mSingleTapDetector = TapDetector(1, TapDetector.DETECTION_METHOD_BOTH)
    }

    override fun processTouchEvent(motionEvent: MotionEvent): Boolean {
        val singleTap = mSingleTapDetector.onTouchEvent(motionEvent)

        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mTracker.startTracking(motionEvent)
                if (!touchpadDisplayed()) {
                    sendTouchCoordinates(motionEvent.x, motionEvent.y)

                    if (LauncherPreferences.PREF_DISABLE_GESTURES) enableMouse()
                    else setGestureStart(motionEvent)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerCount = motionEvent.pointerCount
                val pointerIndex = mTracker.trackEvent(motionEvent)
                if (pointerCount == 1 || LauncherPreferences.PREF_DISABLE_GESTURES) {
                    if (touchpadDisplayed()) {
                        applyMoveVector(mTracker.getMotionVector())
                    } else {
                        val mainPointerX = motionEvent.getX(pointerIndex)
                        val mainPointerY = motionEvent.getY(pointerIndex)
                        sendTouchCoordinates(mainPointerX, mainPointerY)

                        if (!mIsMouseDown) {
                            if (!hasGestureStarted()) setGestureStart(motionEvent)
                            if (!LeftClickGesture.isFingerStill(
                                    mStartX, mStartY, mainPointerX, mainPointerY, FINGER_STILL_THRESHOLD
                                )
                            ) enableMouse()
                        }
                    }
                } else mScroller.performScroll(mTracker.getMotionVector())
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mScroller.resetScrollOvershoot()
                mTracker.cancelTracking()

                if ((!LauncherPreferences.PREF_DISABLE_GESTURES || touchpadDisplayed()) && !mIsMouseDown && singleTap) {
                    CallbackBridge.performClick(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT)
                }

                if (mIsMouseDown) disableMouse()
                resetGesture()
            }
        }

        return true
    }

    private fun touchpadDisplayed(): Boolean {
        return mTouchpad != null && mTouchpad!!.visibility == View.VISIBLE
    }

    fun setAbstractTouchpad(touchpad: View) {
        mTouchpad = touchpad
    }

    private fun enableMouse() {
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, true)
        mIsMouseDown = true
    }

    private fun disableMouse() {
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, false)
        mIsMouseDown = false
    }

    private fun setGestureStart(event: MotionEvent) {
        mStartX = event.x
        mStartY = event.y
    }

    private fun resetGesture() {
        mStartX = -1f
        mStartY = -1f
    }

    private fun hasGestureStarted(): Boolean {
        return mStartX != -1f || mStartY != -1f
    }

    override fun cancelPendingActions() {
        mScroller.resetScrollOvershoot()
        if (mIsMouseDown) disableMouse()
    }
}
