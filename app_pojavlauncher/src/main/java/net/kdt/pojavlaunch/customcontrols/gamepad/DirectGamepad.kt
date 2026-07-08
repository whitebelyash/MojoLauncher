package net.kdt.pojavlaunch.customcontrols.gamepad

import android.view.KeyEvent
import android.view.MotionEvent
import fr.spse.gamepad_remapper.GamepadHandler
import git.artdeell.dnbootstrap.glfw.GLFW
import git.artdeell.dnbootstrap.glfw.GamepadKeycodes

class DirectGamepad : GamepadHandler {
    override fun handleGamepadInput(keycode: Int, value: Float) {
        var gKeycode: Short = -1
        var gAxis: Short = -1
        var normalize = false
        when (keycode) {
            KeyEvent.KEYCODE_BUTTON_A -> gKeycode = GamepadKeycodes.BUTTON_A
            KeyEvent.KEYCODE_BUTTON_B -> gKeycode = GamepadKeycodes.BUTTON_B
            KeyEvent.KEYCODE_BUTTON_X -> gKeycode = GamepadKeycodes.BUTTON_X
            KeyEvent.KEYCODE_BUTTON_Y -> gKeycode = GamepadKeycodes.BUTTON_Y
            KeyEvent.KEYCODE_BUTTON_L1 -> gKeycode = GamepadKeycodes.BUTTON_LEFT_BUMPER
            KeyEvent.KEYCODE_BUTTON_R1 -> gKeycode = GamepadKeycodes.BUTTON_RIGHT_BUMPER
            KeyEvent.KEYCODE_BUTTON_L2, MotionEvent.AXIS_LTRIGGER -> {
                gAxis = GamepadKeycodes.AXIS_LEFT_TRIGGER
                normalize = true
            }
            KeyEvent.KEYCODE_BUTTON_R2, MotionEvent.AXIS_RTRIGGER -> {
                gAxis = GamepadKeycodes.AXIS_RIGHT_TRIGGER
                normalize = true
            }
            KeyEvent.KEYCODE_BUTTON_THUMBL -> gKeycode = GamepadKeycodes.BUTTON_LEFT_THUMB
            KeyEvent.KEYCODE_BUTTON_THUMBR -> gKeycode = GamepadKeycodes.BUTTON_RIGHT_THUMB
            KeyEvent.KEYCODE_BUTTON_START -> gKeycode = GamepadKeycodes.BUTTON_START
            KeyEvent.KEYCODE_BUTTON_SELECT -> gKeycode = GamepadKeycodes.BUTTON_BACK
            KeyEvent.KEYCODE_DPAD_UP -> gKeycode = GamepadKeycodes.BUTTON_DPAD_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> gKeycode = GamepadKeycodes.BUTTON_DPAD_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> gKeycode = GamepadKeycodes.BUTTON_DPAD_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> gKeycode = GamepadKeycodes.BUTTON_DPAD_RIGHT
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                GLFW.gamepadButtonBuffer!!.put(GamepadKeycodes.BUTTON_DPAD_UP.toInt(), GamepadKeycodes.GLFW_RELEASE)
                GLFW.gamepadButtonBuffer!!.put(GamepadKeycodes.BUTTON_DPAD_DOWN.toInt(), GamepadKeycodes.GLFW_RELEASE)
                GLFW.gamepadButtonBuffer!!.put(GamepadKeycodes.BUTTON_DPAD_LEFT.toInt(), GamepadKeycodes.GLFW_RELEASE)
                GLFW.gamepadButtonBuffer!!.put(GamepadKeycodes.BUTTON_DPAD_RIGHT.toInt(), GamepadKeycodes.GLFW_RELEASE)
                return
            }
            MotionEvent.AXIS_X -> gAxis = GamepadKeycodes.AXIS_LEFT_X
            MotionEvent.AXIS_Y -> gAxis = GamepadKeycodes.AXIS_LEFT_Y
            MotionEvent.AXIS_Z -> gAxis = GamepadKeycodes.AXIS_RIGHT_X
            MotionEvent.AXIS_RZ -> gAxis = GamepadKeycodes.AXIS_RIGHT_Y
            MotionEvent.AXIS_HAT_X -> {
                GLFW.gamepadButtonBuffer!!.put(
                    GamepadKeycodes.BUTTON_DPAD_LEFT.toInt(),
                    if (value < -0.85) GamepadKeycodes.GLFW_PRESS else GamepadKeycodes.GLFW_RELEASE
                )
                GLFW.gamepadButtonBuffer!!.put(
                    GamepadKeycodes.BUTTON_DPAD_RIGHT.toInt(),
                    if (value > 0.85) GamepadKeycodes.GLFW_PRESS else GamepadKeycodes.GLFW_RELEASE
                )
                return
            }
            MotionEvent.AXIS_HAT_Y -> {
                GLFW.gamepadButtonBuffer!!.put(
                    GamepadKeycodes.BUTTON_DPAD_UP.toInt(),
                    if (value < -0.85) GamepadKeycodes.GLFW_PRESS else GamepadKeycodes.GLFW_RELEASE
                )
                GLFW.gamepadButtonBuffer!!.put(
                    GamepadKeycodes.BUTTON_DPAD_DOWN.toInt(),
                    if (value > 0.85) GamepadKeycodes.GLFW_PRESS else GamepadKeycodes.GLFW_RELEASE
                )
                return
            }
        }
        if (gKeycode.toInt() != -1) {
            GLFW.gamepadButtonBuffer!!.put(gKeycode.toInt(), if (value > 0.85) GamepadKeycodes.GLFW_PRESS else GamepadKeycodes.GLFW_RELEASE)
        }
        if (gAxis.toInt() != -1) {
            if (normalize) {
                GLFW.gamepadAxisBuffer!!.put(gAxis.toInt(), value * 2 - 1)
            } else {
                GLFW.gamepadAxisBuffer!!.put(gAxis.toInt(), value)
            }
        }
    }
}
