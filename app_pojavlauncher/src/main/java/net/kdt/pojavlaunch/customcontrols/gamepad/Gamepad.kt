package net.kdt.pojavlaunch.customcontrols.gamepad

import android.view.Choreographer
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.AXIS_HAT_X
import android.view.MotionEvent.AXIS_HAT_Y
import android.view.MotionEvent.AXIS_LTRIGGER
import android.view.MotionEvent.AXIS_RTRIGGER
import android.view.MotionEvent.AXIS_RZ
import android.view.MotionEvent.AXIS_X
import android.view.MotionEvent.AXIS_Y
import android.view.MotionEvent.AXIS_Z
import android.view.View
import fr.spse.gamepad_remapper.GamepadHandler
import fr.spse.gamepad_remapper.Settings
import git.artdeell.dnbootstrap.glfw.GLFW
import git.artdeell.dnbootstrap.glfw.GrabListener
import net.kdt.pojavlaunch.CallbackBridge
import net.kdt.pojavlaunch.CallbackBridge.sendMouseButton
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.Tools.currentDisplayMetrics
import net.kdt.pojavlaunch.prefs.LauncherPreferences

class Gamepad : GrabListener, GamepadHandler {
    private val mSensitivityFactor = 1.4 * (1080f / currentDisplayMetrics.heightPixels)

    private val mLeftJoystick: GamepadJoystick
    private var mCurrentJoystickDirection = GamepadJoystick.DIRECTION_NONE
    private val mRightJoystick: GamepadJoystick
    private var mLastHorizontalValue = 0.0f
    private var mLastVerticalValue = 0.0f

    private var mMouseMagnitude = 0.0
    private var mMouseAngle = 0.0
    private var mMouseSensitivity = 19.0

    private var mGameMap: GamepadMap? = null
    private var mMenuMap: GamepadMap? = null
    private var mCurrentMap: GamepadMap? = null
    private var isGrabbing = false

    private val mScreenChoreographer: Choreographer
    private var mLastFrameTime: Long = 0

    private val mMapProvider: GamepadDataProvider
    private val mTouchpadView: View?
    private var mRemoved = false

    constructor(inputDevice: InputDevice, mapProvider: GamepadDataProvider, touchpadView: View?) {
        Settings.setDeadlineScale(LauncherPreferences.PREF_DEADZONE_SCALE)

        mScreenChoreographer = Choreographer.getInstance()
        val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
            tick(frameTimeNanos)
            if (!mRemoved) mScreenChoreographer.postFrameCallback(this)
        }
        mScreenChoreographer.postFrameCallback(frameCallback)
        mLastFrameTime = System.nanoTime()

        mLeftJoystick = GamepadJoystick(AXIS_X, AXIS_Y, inputDevice)
        mRightJoystick = GamepadJoystick(AXIS_Z, AXIS_RZ, inputDevice)

        mMapProvider = mapProvider
        mTouchpadView = touchpadView

        GLFW.cursorX = 0.5
        GLFW.cursorY = 0.5
        GLFW.sendMousePos()

        enableTouchpadIfNecessary()

        reloadGamepadMaps()
        mMapProvider.attachGrabListener(this)
    }

    fun reloadGamepadMaps() {
        mGameMap?.resetPressedState()
        mMenuMap?.resetPressedState()
        GamepadMapStore.load()
        mGameMap = mMapProvider.getGameMap()
        mMenuMap = mMapProvider.getMenuMap()
        mCurrentMap = mGameMap
        val currentGrab = GLFW.isGrabbing()
        isGrabbing = !currentGrab
        onGrabState(currentGrab)
    }

    fun updateJoysticks() {
        updateDirectionalJoystick()
        updateMouseJoystick()
    }

    private fun enableTouchpadIfNecessary() {
        if (mTouchpadView == null) return
        if (mTouchpadView.visibility != View.VISIBLE) mTouchpadView.visibility = View.VISIBLE
    }

    companion object {
        private const val MOUSE_MAX_ACCELERATION = 2.0

        fun sendInput(keycodes: ShortArray, isDown: Boolean) {
            for (keycode in keycodes) {
                when (keycode.toInt()) {
                    GamepadMap.MOUSE_SCROLL_DOWN.toInt() -> if (isDown) CallbackBridge.sendScroll(0, -1)
                    GamepadMap.MOUSE_SCROLL_UP.toInt() -> if (isDown) CallbackBridge.sendScroll(0, 1)
                    GamepadMap.MOUSE_LEFT.toInt() -> sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, isDown)
                    GamepadMap.MOUSE_MIDDLE.toInt() -> sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE, isDown)
                    GamepadMap.MOUSE_RIGHT.toInt() -> sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, isDown)
                    GamepadMap.UNSPECIFIED.toInt() -> {}
                    else -> {
                        CallbackBridge.setModifiers(keycode.toInt(), isDown)
                        GLFW.sendKeyEvent(keycode.toInt(), isDown, CallbackBridge.getCurrentMods())
                    }
                }
            }
        }

        fun isGamepadEvent(event: MotionEvent): Boolean = GamepadJoystick.isJoystickEvent(event)

        fun isGamepadEvent(event: KeyEvent): Boolean {
            val isGamepad = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (event.device != null && (event.device!!.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
            return isGamepad && GamepadDpad.isDpadEvent(event)
        }
    }

    private fun tick(frameTimeNanos: Long) {
        var newFrameTime = System.nanoTime()
        if (mLastHorizontalValue != 0f || mLastVerticalValue != 0f) {
            var acceleration = Math.pow(mMouseMagnitude, MOUSE_MAX_ACCELERATION)
            if (acceleration > 1) acceleration = 1.0

            var deltaX = (Math.cos(mMouseAngle) * acceleration * mMouseSensitivity).toFloat()
            var deltaY = (Math.sin(mMouseAngle) * acceleration * mMouseSensitivity).toFloat()
            newFrameTime = System.nanoTime()
            val deltaTimeScale = (newFrameTime - mLastFrameTime) / 16666666f
            deltaX *= deltaTimeScale
            deltaY *= deltaTimeScale

            GLFW.cursorX += deltaX / 1000
            GLFW.cursorY -= deltaY / 1000

            GLFW.sendMousePos()
        }
        mLastFrameTime = newFrameTime
    }

    private fun updateMouseJoystick() {
        val currentJoystick = if (isGrabbing) mRightJoystick else mLeftJoystick
        val horizontalValue = currentJoystick.horizontalAxis
        val verticalValue = currentJoystick.verticalAxis
        if (horizontalValue != mLastHorizontalValue || verticalValue != mLastVerticalValue) {
            mLastHorizontalValue = horizontalValue
            mLastVerticalValue = verticalValue
            mMouseMagnitude = currentJoystick.magnitude
            mMouseAngle = currentJoystick.angleRadian
            tick(System.nanoTime())
            return
        }
        mLastHorizontalValue = horizontalValue
        mLastVerticalValue = verticalValue
        mMouseMagnitude = currentJoystick.magnitude
        mMouseAngle = currentJoystick.angleRadian
    }

    private fun updateDirectionalJoystick() {
        val currentJoystick = if (isGrabbing) mLeftJoystick else mRightJoystick
        val lastJoystickDirection = mCurrentJoystickDirection
        mCurrentJoystickDirection = currentJoystick.heightDirection
        if (mCurrentJoystickDirection == lastJoystickDirection) return
        sendDirectionalKeycode(lastJoystickDirection, false, getCurrentMap())
        sendDirectionalKeycode(mCurrentJoystickDirection, true, getCurrentMap())
    }

    private fun getCurrentMap(): GamepadMap = mCurrentMap!!

    private fun sendDirectionalKeycode(direction: Int, isDown: Boolean, map: GamepadMap) {
        when (direction) {
            GamepadJoystick.DIRECTION_NORTH -> map.DIRECTION_FORWARD.update(isDown)
            GamepadJoystick.DIRECTION_NORTH_EAST -> {
                map.DIRECTION_FORWARD.update(isDown)
                map.DIRECTION_RIGHT.update(isDown)
            }
            GamepadJoystick.DIRECTION_EAST -> map.DIRECTION_RIGHT.update(isDown)
            GamepadJoystick.DIRECTION_SOUTH_EAST -> {
                map.DIRECTION_RIGHT.update(isDown)
                map.DIRECTION_BACKWARD.update(isDown)
            }
            GamepadJoystick.DIRECTION_SOUTH -> map.DIRECTION_BACKWARD.update(isDown)
            GamepadJoystick.DIRECTION_SOUTH_WEST -> {
                map.DIRECTION_BACKWARD.update(isDown)
                map.DIRECTION_LEFT.update(isDown)
            }
            GamepadJoystick.DIRECTION_WEST -> map.DIRECTION_LEFT.update(isDown)
            GamepadJoystick.DIRECTION_NORTH_WEST -> {
                map.DIRECTION_FORWARD.update(isDown)
                map.DIRECTION_LEFT.update(isDown)
            }
        }
    }

    override fun onGrabState(isGrabbing: Boolean) {
        val lastGrabbingValue = this.isGrabbing
        this.isGrabbing = isGrabbing
        if (lastGrabbingValue == isGrabbing) return

        mCurrentMap!!.resetPressedState()
        if (isGrabbing) {
            mCurrentMap = mGameMap
            mMouseSensitivity = 18.0
            return
        }

        mCurrentMap = mMenuMap
        sendDirectionalKeycode(mCurrentJoystickDirection, false, mGameMap!!)
        mMouseSensitivity = 19.0 * LauncherPreferences.PREF_SCALE_FACTOR / mSensitivityFactor
    }

    override fun handleGamepadInput(keycode: Int, value: Float) {
        enableTouchpadIfNecessary()
        val isKeyEventDown = value == 1f
        when (keycode) {
            KeyEvent.KEYCODE_BUTTON_A -> getCurrentMap().BUTTON_A.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_B -> getCurrentMap().BUTTON_B.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_X -> getCurrentMap().BUTTON_X.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_Y -> getCurrentMap().BUTTON_Y.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_L1 -> getCurrentMap().SHOULDER_LEFT.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_R1 -> getCurrentMap().SHOULDER_RIGHT.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_L2 -> getCurrentMap().TRIGGER_LEFT.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_R2 -> getCurrentMap().TRIGGER_RIGHT.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_THUMBL -> getCurrentMap().THUMBSTICK_LEFT.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_THUMBR -> getCurrentMap().THUMBSTICK_RIGHT.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_UP -> getCurrentMap().DPAD_UP.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_DOWN -> getCurrentMap().DPAD_DOWN.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_LEFT -> getCurrentMap().DPAD_LEFT.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_RIGHT -> getCurrentMap().DPAD_RIGHT.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                getCurrentMap().DPAD_RIGHT.update(false)
                getCurrentMap().DPAD_LEFT.update(false)
                getCurrentMap().DPAD_UP.update(false)
                getCurrentMap().DPAD_DOWN.update(false)
            }
            KeyEvent.KEYCODE_BUTTON_START -> getCurrentMap().BUTTON_START.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_SELECT -> getCurrentMap().BUTTON_SELECT.update(isKeyEventDown)
            AXIS_HAT_X -> {
                getCurrentMap().DPAD_RIGHT.update(value > 0.85)
                getCurrentMap().DPAD_LEFT.update(value < -0.85)
            }
            AXIS_HAT_Y -> {
                getCurrentMap().DPAD_DOWN.update(value > 0.85)
                getCurrentMap().DPAD_UP.update(value < -0.85)
            }
            AXIS_X -> {
                mLeftJoystick.setXAxisValue(value)
                updateJoysticks()
            }
            AXIS_Y -> {
                mLeftJoystick.setYAxisValue(value)
                updateJoysticks()
            }
            AXIS_Z -> {
                mRightJoystick.setXAxisValue(value)
                updateJoysticks()
            }
            AXIS_RZ -> {
                mRightJoystick.setYAxisValue(value)
                updateJoysticks()
            }
            AXIS_RTRIGGER -> getCurrentMap().TRIGGER_RIGHT.update(value > 0.5)
            AXIS_LTRIGGER -> getCurrentMap().TRIGGER_LEFT.update(value > 0.5)
            else -> {
                val modifiers = CallbackBridge.getCurrentMods()
                GLFW.sendKeyEvent(LwjglGlfwKeycode.GLFW_KEY_SPACE, isKeyEventDown, modifiers)
            }
        }
    }

    fun removeSelf() {
        mRemoved = true
    }
}
