package net.kdt.pojavlaunch

import net.kdt.pojavlaunch.MainActivity.Companion.touchCharInput
import net.kdt.pojavlaunch.utils.MCOptionUtils.getMcScale
import net.kdt.pojavlaunch.CallbackBridge.sendMouseButton
import net.kdt.pojavlaunch.CallbackBridge.windowHeight
import net.kdt.pojavlaunch.CallbackBridge.windowWidth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup

import androidx.annotation.RequiresApi

import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.gamepad.DefaultDataProvider
import net.kdt.pojavlaunch.customcontrols.gamepad.Gamepad
import net.kdt.pojavlaunch.customcontrols.gamepad.DirectGamepad
import net.kdt.pojavlaunch.customcontrols.mouse.AndroidPointerCapture
import net.kdt.pojavlaunch.customcontrols.mouse.InGUIEventProcessor
import net.kdt.pojavlaunch.customcontrols.mouse.InGameEventProcessor
import net.kdt.pojavlaunch.customcontrols.mouse.TouchEventProcessor
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.render.SurfaceProvider
import net.kdt.pojavlaunch.render.SurfaceViewSurfaceProvider
import net.kdt.pojavlaunch.render.TextureViewSurfaceProvider
import net.kdt.pojavlaunch.utils.JREUtils
import net.kdt.pojavlaunch.utils.MCOptionUtils

import fr.spse.gamepad_remapper.GamepadHandler
import fr.spse.gamepad_remapper.RemapperManager
import fr.spse.gamepad_remapper.RemapperView
import git.artdeell.dnbootstrap.glfw.GLFW
import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler
import git.artdeell.dnbootstrap.glfw.GrabListener

class LauncherGLSurface : View, GrabListener, GamepadEnableHandler, SurfaceProvider.SurfaceCallback {
    private var mGamepadHandler: GamepadHandler? = null
    private val mInputManager = RemapperManager(context, RemapperView.Builder(null)
        .remapA(true)
        .remapB(true)
        .remapX(true)
        .remapY(true)
        .remapLeftJoystick(true)
        .remapRightJoystick(true)
        .remapStart(true)
        .remapSelect(true)
        .remapLeftShoulder(true)
        .remapRightShoulder(true)
        .remapLeftTrigger(true)
        .remapRightTrigger(true)
        .remapDpad(true))

    private val mSensitivityFactor = 1.4 * (1080f / Tools.getDisplayMetrics(context as Activity).heightPixels)
    private val mSurfaceProvider = if (LauncherPreferences.PREF_USE_ALTERNATE_SURFACE) SurfaceViewSurfaceProvider() else TextureViewSurfaceProvider()
    private var mRefreshOnly = true
    var mSurfaceReadyListener: SurfaceReadyListener? = null
    val mSurfaceReadyListenerLock = Any()
    var mSurface: View? = null

    private val mIngameProcessor = InGameEventProcessor(this, mSensitivityFactor)
    private val mInGUIProcessor = InGUIEventProcessor(this)
    private var mCurrentTouchProcessor: TouchEventProcessor = mInGUIProcessor
    private var mPointerCapture: AndroidPointerCapture? = null
    private var mTouchpad: View? = null
    private var mLastGrabState = false

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        isFocusable = true
        GLFW.setGamepadEnableHandler(this)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setUpPointerCapture() {
        mPointerCapture?.detach()
        mPointerCapture = AndroidPointerCapture(mTouchpad!!, this)
    }

    fun start(isAlreadyRunning: Boolean, touchpad: View) {
        mTouchpad = touchpad
        if (Tools.isAndroid8OrHigher()) setUpPointerCapture()
        mInGUIProcessor.setAbstractTouchpad(touchpad)
        mRefreshOnly = isAlreadyRunning
        mSurface = mSurfaceProvider.create(context, this)
        (parent as ViewGroup).addView(mSurface)
    }

    @Suppress("accessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if ((parent as ControlLayout).getModifiable()) return false

        for (i in 0 until e.pointerCount) {
            val toolType = e.getToolType(i)
            if (toolType == MotionEvent.TOOL_TYPE_MOUSE) {
                if (Tools.isAndroid8OrHigher() && mPointerCapture != null) {
                    mPointerCapture!!.handleAutomaticCapture()
                    return true
                }
            } else if (toolType != MotionEvent.TOOL_TYPE_STYLUS) continue

            if (GLFW.isGrabbing()) return false
            GLFW.cursorX = e.getX(i) / width.toDouble()
            GLFW.cursorY = e.getY(i) / height.toDouble()
            GLFW.sendMousePos()
            return true
        }
        if (mIngameProcessor == null || mInGUIProcessor == null) return true
        val ret = mCurrentTouchProcessor.processTouchEvent(e)
        if (LauncherPreferences.PREF_KEYBOARD_AUTOPANNING && MainActivity.mImeHeight > 0) {
            val translationY = Tools.getTranslationFromCursorY(
                (GLFW.cursorY * mSurface!!.height + 100).toInt(),
                mSurface!!.height,
                MainActivity.mImeHeight,
                0
            )
            if (MainActivity.mForceFullPanning) {
                mSurface!!.animate().setDuration(100).translationY(-translationY.toFloat()).start()
                mTouchpad!!.animate().setDuration(100).translationY(-translationY.toFloat()).start()
                MainActivity.mForceFullPanning = false
            } else {
                mSurface!!.translationY = -translationY.toFloat()
                mTouchpad!!.translationY = -translationY.toFloat()
            }
        }
        return ret
    }

    private fun createGamepad(inputDevice: InputDevice) {
        mGamepadHandler = if (GLFW.gamepadButtonBuffer != null) {
            GLFW.nativeNotifyGamepadConnected()
            DirectGamepad()
        } else {
            Gamepad(inputDevice, DefaultDataProvider.INSTANCE, mTouchpad)
        }
    }

    @SuppressLint("NewApi")
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        var mouseCursorIndex = -1

        if (Gamepad.isGamepadEvent(event)) {
            if (mGamepadHandler == null) createGamepad(event.device!!)
            mInputManager.handleMotionEventInput(context, event, mGamepadHandler)
            return true
        }

        for (i in 0 until event.pointerCount) {
            if (event.getToolType(i) != MotionEvent.TOOL_TYPE_MOUSE && event.getToolType(i) != MotionEvent.TOOL_TYPE_STYLUS) continue
            mouseCursorIndex = i
            break
        }
        if (mouseCursorIndex == -1) return false

        updateGrabState(GLFW.isGrabbing())

        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_MOVE -> {
                GLFW.cursorX = event.getX(mouseCursorIndex) / width.toDouble()
                GLFW.cursorY = event.getY(mouseCursorIndex) / height.toDouble()
                GLFW.sendMousePos()
                return true
            }
            MotionEvent.ACTION_SCROLL -> {
                CallbackBridge.sendScroll(event.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(), event.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble())
                return true
            }
            MotionEvent.ACTION_BUTTON_PRESS -> return sendMouseButtonUnconverted(event.actionButton, true)
            MotionEvent.ACTION_BUTTON_RELEASE -> return sendMouseButtonUnconverted(event.actionButton, false)
            else -> return false
        }
    }

    fun processKeyEvent(event: KeyEvent): Boolean {
        val eventKeycode = event.keyCode
        if (eventKeycode == KeyEvent.KEYCODE_UNKNOWN) return true
        if (eventKeycode == KeyEvent.KEYCODE_VOLUME_DOWN) return false
        if (eventKeycode == KeyEvent.KEYCODE_VOLUME_UP) return false
        if (event.repeatCount != 0) return true
        val action = event.action
        if (action == KeyEvent.ACTION_MULTIPLE) return true
        if (action == KeyEvent.ACTION_UP &&
            (event.flags and KeyEvent.FLAG_CANCELED) != 0) return true

        if ((event.flags and KeyEvent.FLAG_SOFT_KEYBOARD) == KeyEvent.FLAG_SOFT_KEYBOARD) {
            if (eventKeycode == KeyEvent.KEYCODE_ENTER) return true
            touchCharInput!!.dispatchKeyEvent(event)
            return true
        }

        if (event.device != null
            && (((event.source and InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE
                    || (event.source and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE))) {
            if (eventKeycode == KeyEvent.KEYCODE_BACK) {
                sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, event.action == KeyEvent.ACTION_DOWN)
                return true
            }
        }

        if (Gamepad.isGamepadEvent(event)) {
            if (mGamepadHandler == null) createGamepad(event.device!!)
            mInputManager.handleKeyEventInput(context, event, mGamepadHandler)
            return true
        }

        CallbackBridge.setModifiers(event)
        val codepoint = if (action == KeyEvent.ACTION_DOWN) event.getUnicodeChar(event.metaState).toChar() else 0.toChar()
        GLFW.sendRawKeyEvent(eventKeycode, if (action == KeyEvent.ACTION_DOWN) 1 else 0, CallbackBridge.currentMods, codepoint)

        return (event.flags and KeyEvent.FLAG_FALLBACK) == KeyEvent.FLAG_FALLBACK
    }

    companion object {
        fun sendMouseButtonUnconverted(button: Int, status: Boolean): Boolean {
            val glfwButton = when (button) {
                MotionEvent.BUTTON_PRIMARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT
                MotionEvent.BUTTON_TERTIARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE
                MotionEvent.BUTTON_SECONDARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT
                else -> -256
            }
            if (glfwButton == -256) return false
            sendMouseButton(glfwButton, status)
            return true
        }
    }

    fun refreshSize() {
        refreshSize(false)
    }

    fun refreshSize(immediate: Boolean) {
        if (isInLayout && !immediate) {
            post { refreshSize() }
            return
        }
        val newWidth = Tools.getDisplayFriendlyRes(width, LauncherPreferences.PREF_SCALE_FACTOR)
        val newHeight = Tools.getDisplayFriendlyRes(height, LauncherPreferences.PREF_SCALE_FACTOR)
        if (newHeight < 1 || newWidth < 1) {
            Log.e("MGLSurface", String.format("Impossible resolution : %dx%d", newWidth, newHeight))
            return
        }
        windowWidth = newWidth
        windowHeight = newHeight
        if (mSurface == null) {
            Log.w("MGLSurface", "Attempt to refresh size on null surface")
            return
        }
        JREUtils.configureRenderspecDisplay(windowWidth, windowHeight, mSurface!!.display.refreshRate.toInt())
        mSurfaceProvider.updateSize()
    }

    private fun realStart() {
        refreshSize(true)
        MCOptionUtils.set("fullscreen", "off")
        MCOptionUtils.set("overrideWidth", windowWidth.toString())
        MCOptionUtils.set("overrideHeight", windowHeight.toString())
        MCOptionUtils.save()
        getMcScale()

        Thread {
            try {
                synchronized(mSurfaceReadyListenerLock) {
                    if (mSurfaceReadyListener == null) mSurfaceReadyListenerLock.wait()
                }
                mSurfaceReadyListener!!.isReady()
            } catch (e: Throwable) {
                Tools.showError(context, e, true)
            }
        }.start()
    }

    override fun onGrabState(isGrabbing: Boolean) {
        post { updateGrabState(isGrabbing) }
    }

    private fun pickEventProcessor(isGrabbing: Boolean): TouchEventProcessor {
        return if (isGrabbing) mIngameProcessor else mInGUIProcessor
    }

    private fun updateGrabState(isGrabbing: Boolean) {
        if (mLastGrabState != isGrabbing) {
            mCurrentTouchProcessor.cancelPendingActions()
            mCurrentTouchProcessor = pickEventProcessor(isGrabbing)
            mLastGrabState = isGrabbing
        }
    }

    override fun onSurfaceAvailable(surface: Surface) {
        GLFW.nativeSurfaceCreated(surface)
        if (mRefreshOnly) return
        realStart()
        mRefreshOnly = true
    }

    override fun onSurfaceResized() {
        GLFW.nativeSurfaceUpdated()
    }

    override fun onSurfaceDestroyed() {
        GLFW.nativeSurfaceDestroyed()
    }

    override fun onEnableGamepad() {
        post {
            if (mGamepadHandler != null && mGamepadHandler is Gamepad) {
                (mGamepadHandler as Gamepad).removeSelf()
            }
            mGamepadHandler = null
        }
    }

    interface SurfaceReadyListener {
        fun isReady()
    }

    fun setSurfaceReadyListener(listener: SurfaceReadyListener) {
        synchronized(mSurfaceReadyListenerLock) {
            mSurfaceReadyListener = listener
            mSurfaceReadyListenerLock.notifyAll()
        }
    }
}
