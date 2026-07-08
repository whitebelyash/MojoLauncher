package net.kdt.pojavlaunch.customcontrols.mouse

import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import git.artdeell.dnbootstrap.glfw.GLFW
import net.kdt.pojavlaunch.CallbackBridge
import net.kdt.pojavlaunch.LauncherGLSurface
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences

@RequiresApi(api = Build.VERSION_CODES.O)
class AndroidPointerCapture : ViewTreeObserver.OnWindowFocusChangeListener, View.OnCapturedPointerListener {
    companion object {
        private const val TOUCHPAD_SCROLL_THRESHOLD = 1f
    }

    private val mTouchpadView: View
    private val mHostView: View
    private val mMousePrescale = Tools.dpToPx(1f)
    private val mPointerTracker = PointerTracker()
    private val mScroller = Scroller(TOUCHPAD_SCROLL_THRESHOLD)
    private val mVector = mPointerTracker.getMotionVector()

    private var mInputDeviceIdentifier = 0
    private var mDeviceSupportsRelativeAxis = false

    constructor(touchpad: View, hostView: View) {
        this.mTouchpadView = touchpad
        this.mHostView = hostView
        hostView.setOnCapturedPointerListener(this)
        hostView.viewTreeObserver.addOnWindowFocusChangeListener(this)
    }

    private fun enableTouchpadIfNecessary() {
        if (mTouchpadView.visibility != View.VISIBLE) mTouchpadView.visibility = View.VISIBLE
    }

    fun handleAutomaticCapture() {
        if (!mHostView.hasWindowFocus()) {
            mHostView.requestFocus()
        } else {
            mHostView.requestPointerCapture()
        }
    }

    private fun accumulateHistoricalValues(motionEvent: MotionEvent, axisX: Int, axisY: Int) {
        var relX = motionEvent.getAxisValue(axisX)
        var relY = motionEvent.getAxisValue(axisY)

        if (motionEvent.historySize > 1) {
            for (i in 0 until motionEvent.historySize) {
                relX += motionEvent.getHistoricalAxisValue(axisX, i)
                relY += motionEvent.getHistoricalAxisValue(axisY, i)
            }
        }

        mVector[0] = relX
        mVector[1] = relY
    }

    override fun onCapturedPointer(view: View, event: MotionEvent): Boolean {
        checkSameDevice(event.device)

        if ((event.source and InputDevice.SOURCE_CLASS_TRACKBALL) != 0) {
            if (mDeviceSupportsRelativeAxis) {
                accumulateHistoricalValues(event, MotionEvent.AXIS_RELATIVE_X, MotionEvent.AXIS_RELATIVE_Y)
            } else {
                accumulateHistoricalValues(event, MotionEvent.AXIS_X, MotionEvent.AXIS_Y)
            }
        } else {
            mPointerTracker.trackEvent(event)
        }

        if (!GLFW.isGrabbing()) {
            enableTouchpadIfNecessary()
            mVector[0] *= mMousePrescale
            mVector[1] *= mMousePrescale
            if (event.pointerCount < 2) {
                applyMotionVector(view, LauncherPreferences.PREF_MOUSESPEED)
                mScroller.resetScrollOvershoot()
            } else {
                mScroller.performScroll(mVector)
            }
        } else {
            applyMotionVector(view, 1f)
        }

        return when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> true
            MotionEvent.ACTION_BUTTON_PRESS -> LauncherGLSurface.sendMouseButtonUnconverted(event.actionButton, true)
            MotionEvent.ACTION_BUTTON_RELEASE -> LauncherGLSurface.sendMouseButtonUnconverted(event.actionButton, false)
            MotionEvent.ACTION_SCROLL -> {
                CallbackBridge.sendScroll(
                    event.getAxisValue(MotionEvent.AXIS_HSCROLL),
                    event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                )
                true
            }
            MotionEvent.ACTION_UP -> {
                mPointerTracker.cancelTracking()
                true
            }
            else -> false
        }
    }

    private fun applyMotionVector(view: View, speed: Float) {
        GLFW.cursorX += mVector[0] * speed / view.width
        GLFW.cursorY += mVector[1] * speed / view.height
        GLFW.sendMousePos()
    }

    private fun checkSameDevice(inputDevice: InputDevice?) {
        val newIdentifier = inputDevice?.id ?: Int.MAX_VALUE
        if (mInputDeviceIdentifier != newIdentifier) {
            reinitializeDeviceSpecificProperties(inputDevice)
            mInputDeviceIdentifier = newIdentifier
        }
    }

    private fun reinitializeDeviceSpecificProperties(inputDevice: InputDevice?) {
        mPointerTracker.cancelTracking()
        if (inputDevice == null) {
            mDeviceSupportsRelativeAxis = false
            return
        }
        val relativeXSupported = inputDevice.getMotionRange(MotionEvent.AXIS_RELATIVE_X) != null
        val relativeYSupported = inputDevice.getMotionRange(MotionEvent.AXIS_RELATIVE_Y) != null
        mDeviceSupportsRelativeAxis = relativeXSupported && relativeYSupported
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus && Tools.isAndroid8OrHigher()) mHostView.requestPointerCapture()
    }

    fun detach() {
        mHostView.setOnCapturedPointerListener(null)
        mHostView.viewTreeObserver.removeOnWindowFocusChangeListener(this)
    }
}
