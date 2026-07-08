package net.kdt.pojavlaunch.customcontrols.gamepad

import android.view.InputDevice
import android.view.MotionEvent
import net.kdt.pojavlaunch.utils.MathUtils

class GamepadJoystick {
    companion object {
        const val DIRECTION_NONE = -1
        const val DIRECTION_EAST = 0
        const val DIRECTION_NORTH_EAST = 1
        const val DIRECTION_NORTH = 2
        const val DIRECTION_NORTH_WEST = 3
        const val DIRECTION_WEST = 4
        const val DIRECTION_SOUTH_WEST = 5
        const val DIRECTION_SOUTH = 6
        const val DIRECTION_SOUTH_EAST = 7

        fun isJoystickEvent(event: MotionEvent): Boolean {
            return (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
                    event.action == MotionEvent.ACTION_MOVE
        }
    }

    private val mInputDevice: InputDevice
    private val mHorizontalAxis: Int
    private val mVerticalAxis: Int
    private var mVerticalAxisValue = 0f
    private var mHorizontalAxisValue = 0f

    constructor(horizontalAxis: Int, verticalAxis: Int, device: InputDevice) {
        mHorizontalAxis = horizontalAxis
        mVerticalAxis = verticalAxis
        this.mInputDevice = device
    }

    val angleRadian: Double
        get() = -Math.atan2(mVerticalAxisValue.toDouble(), mHorizontalAxisValue.toDouble())

    val angleDegree: Double
        get() {
            var result = Math.toDegrees(angleRadian)
            if (result < 0) result += 360.0
            return result
        }

    val magnitude: Double
        get() {
            val x = kotlin.math.abs(mHorizontalAxisValue)
            val y = kotlin.math.abs(mVerticalAxisValue)
            return MathUtils.dist(0f, 0f, x, y)
        }

    val verticalAxis: Float get() = mVerticalAxisValue
    val horizontalAxis: Float get() = mHorizontalAxisValue

    val heightDirection: Int
        get() {
            if (magnitude == 0.0) return DIRECTION_NONE
            return ((angleDegree + 22.5) / 45).toInt() % 8
        }

    fun setXAxisValue(value: Float) {
        this.mHorizontalAxisValue = value
    }

    fun setYAxisValue(value: Float) {
        this.mVerticalAxisValue = value
    }
}
