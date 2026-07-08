package net.kdt.pojavlaunch.customcontrols.mouse

import android.view.MotionEvent
import net.kdt.pojavlaunch.Tools

class TapDetector(
    tapNumberToDetect: Int,
    private val mDetectionMethod: Int
) {
    companion object {
        const val DETECTION_METHOD_DOWN = 0x1
        const val DETECTION_METHOD_UP = 0x2
        const val DETECTION_METHOD_BOTH = 0x3

        private const val TAP_MIN_DELTA_MS = -1
        private const val TAP_MAX_DELTA_MS = 300
        private val TAP_SLOP_SQUARE_PX = Tools.dpToPx(2500f).toInt()
    }

    private val mTapNumberToDetect: Int
    private var mCurrentTapNumber = 0
    private var mLastEventTime: Long = 0
    private var mLastX = 9999f
    private var mLastY = 9999f

    init {
        mTapNumberToDetect = if (detectBothTouch()) 2 * tapNumberToDetect else tapNumberToDetect
    }

    fun onTouchEvent(e: MotionEvent): Boolean {
        val eventAction = e.actionMasked
        var pointerIndex = -1

        if (detectDownTouch()) {
            if (eventAction == MotionEvent.ACTION_DOWN) pointerIndex = 0
            else if (eventAction == MotionEvent.ACTION_POINTER_DOWN) pointerIndex = e.actionIndex
        }
        if (detectUpTouch()) {
            if (eventAction == MotionEvent.ACTION_UP) pointerIndex = 0
            else if (eventAction == MotionEvent.ACTION_POINTER_UP) pointerIndex = e.actionIndex
        }

        if (pointerIndex == -1) return false

        val eventX = e.getX(pointerIndex)
        val eventY = e.getY(pointerIndex)
        val eventTime = e.eventTime

        val deltaTime = eventTime - mLastEventTime
        val deltaX = (mLastX - eventX).toInt()
        val deltaY = (mLastY - eventY).toInt()

        mLastEventTime = eventTime
        mLastX = eventX
        mLastY = eventY

        if (mCurrentTapNumber > 0) {
            if ((deltaTime < TAP_MIN_DELTA_MS || deltaTime > TAP_MAX_DELTA_MS) ||
                (deltaX * deltaX + deltaY * deltaY) > TAP_SLOP_SQUARE_PX
            ) {
                if (mDetectionMethod == DETECTION_METHOD_BOTH &&
                    (eventAction == MotionEvent.ACTION_UP || eventAction == MotionEvent.ACTION_POINTER_UP)
                ) {
                    resetTapDetectionState()
                    return false
                } else {
                    mCurrentTapNumber = 0
                }
            }
        }

        mCurrentTapNumber += 1
        if (mCurrentTapNumber >= mTapNumberToDetect) {
            resetTapDetectionState()
            return true
        }

        return false
    }

    private fun resetTapDetectionState() {
        mCurrentTapNumber = 0
        mLastEventTime = 0
        mLastX = 9999f
        mLastY = 9999f
    }

    private fun detectDownTouch(): Boolean {
        return (mDetectionMethod and DETECTION_METHOD_DOWN) == DETECTION_METHOD_DOWN
    }

    private fun detectUpTouch(): Boolean {
        return (mDetectionMethod and DETECTION_METHOD_UP) == DETECTION_METHOD_UP
    }

    private fun detectBothTouch(): Boolean {
        return mDetectionMethod == DETECTION_METHOD_BOTH
    }
}
