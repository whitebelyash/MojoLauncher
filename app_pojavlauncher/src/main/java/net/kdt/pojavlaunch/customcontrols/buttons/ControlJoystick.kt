package net.kdt.pojavlaunch.customcontrols.buttons

import android.annotation.SuppressLint
import android.view.View
import git.artdeell.dnbootstrap.glfw.GLFW
import io.github.controlwear.virtual.joystick.android.JoystickView
import net.kdt.pojavlaunch.CallbackBridge
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog

@SuppressLint("ViewConstructor")
class ControlJoystick : JoystickView, ControlInterface {
    companion object {
        const val DIRECTION_FORWARD_LOCK = 8
    }

    private val mDirectionForwardLock = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL)
    private val mDirectionForward = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_W)
    private val mDirectionRight = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_D)
    private val mDirectionBackward = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_S)
    private val mDirectionLeft = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_A)
    private var mControlData: ControlJoystickData
    private var mLastDirectionInt = GamepadJoystick.DIRECTION_NONE
    private var mCurrentDirectionInt = GamepadJoystick.DIRECTION_NONE

    constructor(parent: ControlLayout, data: ControlJoystickData) : super(parent.context) {
        init(data, parent)
    }

    private fun sendInput(keys: IntArray, isDown: Boolean) {
        for (key in keys) {
            val modifiers = CallbackBridge.getCurrentMods()
            GLFW.sendKeyEvent(key, isDown, modifiers)
        }
    }

    private fun init(data: ControlJoystickData, layout: ControlLayout) {
        mControlData = data
        setProperties(preProcessProperties(data, layout))
        deadzone = 35
        setFixedCenter(data.absolute)
        setAutoReCenterButton(true)

        injectBehaviors()

        setOnMoveListener(object : OnMoveListener {
            override fun onMove(angle: Int, strength: Int) {
                mLastDirectionInt = mCurrentDirectionInt
                mCurrentDirectionInt = getDirectionInt(angle, strength)

                if (mLastDirectionInt != mCurrentDirectionInt) {
                    sendDirectionalKeycode(mLastDirectionInt, false)
                    sendDirectionalKeycode(mCurrentDirectionInt, true)
                }
            }

            override fun onForwardLock(isLocked: Boolean) {
                sendInput(mDirectionForwardLock, isLocked)
            }
        })
    }

    override fun getControlView(): View = this

    override fun getProperties(): ControlData = mControlData

    override fun setProperties(properties: ControlData, changePos: Boolean) {
        mControlData = properties as ControlJoystickData
        mControlData.isHideable = true
        ControlInterface.super.setProperties(properties, changePos)
        postDelayed({
            forwardLockDistance = if (mControlData.forwardLock) Tools.dpToPx(60f).toInt() else 0
            setFixedCenter(mControlData.absolute)
        }, 10)
    }

    override fun removeButton() {
        getControlLayoutParent()?.layout?.mJoystickDataList?.remove(getProperties())
        getControlLayoutParent()?.removeView(this)
    }

    override fun cloneButton() {
        val data = ControlJoystickData(mControlData)
        getControlLayoutParent()?.addJoystickButton(data)
    }

    override fun handlePressed() {}
    override fun handleReleased() {}

    override fun setBackground() {
        borderWidth = Tools.dpToPx(getProperties().strokeWidth * (getControlLayoutParent()!!.getLayoutScale() / 100f)).toInt()
        setBorderColor(getProperties().strokeColor)
        setBackgroundColor(getProperties().bgColor)
    }

    override fun loadEditValues(editControlPopup: EditControlSideDialog) {
        editControlPopup.loadJoystickValues(mControlData)
    }

    private fun getDirectionInt(angle: Int, intensity: Int): Int {
        if (intensity == 0) return GamepadJoystick.DIRECTION_NONE
        return (((angle + 22.5) / 45).toInt() % 8)
    }

    private fun sendDirectionalKeycode(direction: Int, isDown: Boolean) {
        when (direction) {
            GamepadJoystick.DIRECTION_NORTH -> sendInput(mDirectionForward, isDown)
            GamepadJoystick.DIRECTION_NORTH_EAST -> {
                sendInput(mDirectionForward, isDown)
                sendInput(mDirectionRight, isDown)
            }
            GamepadJoystick.DIRECTION_EAST -> sendInput(mDirectionRight, isDown)
            GamepadJoystick.DIRECTION_SOUTH_EAST -> {
                sendInput(mDirectionRight, isDown)
                sendInput(mDirectionBackward, isDown)
            }
            GamepadJoystick.DIRECTION_SOUTH -> sendInput(mDirectionBackward, isDown)
            GamepadJoystick.DIRECTION_SOUTH_WEST -> {
                sendInput(mDirectionBackward, isDown)
                sendInput(mDirectionLeft, isDown)
            }
            GamepadJoystick.DIRECTION_WEST -> sendInput(mDirectionLeft, isDown)
            GamepadJoystick.DIRECTION_NORTH_WEST -> {
                sendInput(mDirectionForward, isDown)
                sendInput(mDirectionLeft, isDown)
            }
            DIRECTION_FORWARD_LOCK -> sendInput(mDirectionForwardLock, isDown)
        }
    }
}
