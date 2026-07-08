package net.kdt.pojavlaunch.customcontrols.mouse

import android.view.MotionEvent

class PointerTracker {
    private var mColdStart = true
    private var mTrackedPointerId = 0
    private var mPointerCount = 0
    private var mLastX = 0f
    private var mLastY = 0f
    private val mMotionVector = FloatArray(2)

    fun startTracking(motionEvent: MotionEvent) {
        mColdStart = false
        mTrackedPointerId = motionEvent.getPointerId(0)
        mPointerCount = motionEvent.pointerCount
        mLastX = motionEvent.x
        mLastY = motionEvent.y
    }

    fun cancelTracking() {
        mColdStart = true
    }

    fun trackEvent(motionEvent: MotionEvent): Int {
        var trackedPointerIndex = motionEvent.findPointerIndex(mTrackedPointerId)
        val pointerCount = motionEvent.pointerCount
        if (trackedPointerIndex == -1 || mPointerCount != pointerCount || mColdStart) {
            startTracking(motionEvent)
            trackedPointerIndex = 0
        }
        val trackedX = motionEvent.getX(trackedPointerIndex)
        val trackedY = motionEvent.getY(trackedPointerIndex)
        mMotionVector[0] = trackedX - mLastX
        mMotionVector[1] = trackedY - mLastY
        mLastX = trackedX
        mLastY = trackedY
        return trackedPointerIndex
    }

    fun getMotionVector(): FloatArray = mMotionVector
}
