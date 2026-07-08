package net.kdt.pojavlaunch.customcontrols.mouse

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import git.artdeell.dnbootstrap.glfw.GLFW
import git.artdeell.dnbootstrap.glfw.GrabListener
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import java.util.Arrays

class GyroControl : SensorEventListener, GrabListener {
    companion object {
        private const val SINGLE_AXIS_LOW_PASS_THRESHOLD = 0.00113f
        private const val MULTI_AXIS_LOW_PASS_THRESHOLD = 0.0013f
        private const val ROTATION_VECTOR_WARMUP_PERIOD = 2
    }

    private val mWindowManager: WindowManager
    private var mSurfaceRotation = 0
    private val mSensorManager: SensorManager
    private val mSensor: Sensor?
    private val mCorrectionListener: OrientationCorrectionListener
    private var mShouldHandleEvents = false
    private var mWarmup = 0
    private var xFactor = 1f
    private var yFactor = 1f
    private var mSwapXY = false

    private val mPreviousRotation = FloatArray(16)
    private val mCurrentRotation = FloatArray(16)
    private val mAngleDifference = FloatArray(3)

    private val mAngleBuffer = Array(
        if (LauncherPreferences.PREF_GYRO_SMOOTHING) 2 else 1
    ) { FloatArray(3) }
    private var xTotal = 0f
    private var yTotal = 0f
    private var xAverage = 0f
    private var yAverage = 0f
    private var mHistoryIndex = -1
    private var mStoredX = 0f
    private var mStoredY = 0f

    constructor(activity: Activity) {
        mWindowManager = activity.windowManager
        mSurfaceRotation = -10
        mSensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        mCorrectionListener = OrientationCorrectionListener(activity)
        updateOrientation()
    }

    fun enable() {
        if (mSensor == null) return
        mWarmup = ROTATION_VECTOR_WARMUP_PERIOD
        mSensorManager.registerListener(this, mSensor, (1000 * LauncherPreferences.PREF_GYRO_SAMPLE_RATE).toInt())
        mCorrectionListener.enable()
        mShouldHandleEvents = GLFW.isGrabbing()
        GLFW.addGrabListener(this)
    }

    fun disable() {
        if (mSensor == null) return
        mSensorManager.unregisterListener(this)
        mCorrectionListener.disable()
        mStoredX = 0f
        mStoredY = 0f
        resetDamper()
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        if (!mShouldHandleEvents) return
        System.arraycopy(mCurrentRotation, 0, mPreviousRotation, 0, 16)
        SensorManager.getRotationMatrixFromVector(mCurrentRotation, sensorEvent.values)

        if (mWarmup > 0) {
            mWarmup--
            return
        }
        SensorManager.getAngleChange(mAngleDifference, mCurrentRotation, mPreviousRotation)
        damperValue(mAngleDifference)
        mStoredX += xAverage * LauncherPreferences.PREF_GYRO_SENSITIVITY
        mStoredY += yAverage * LauncherPreferences.PREF_GYRO_SENSITIVITY

        var updatePosition = false
        val absX = kotlin.math.abs(mStoredX)
        val absY = kotlin.math.abs(mStoredY)

        if (absX + absY > MULTI_AXIS_LOW_PASS_THRESHOLD) {
            GLFW.cursorX -= (if (mSwapXY) mStoredY else mStoredX) * xFactor
            GLFW.cursorY += (if (mSwapXY) mStoredX else mStoredY) * yFactor
            mStoredX = 0f
            mStoredY = 0f
            updatePosition = true
        } else {
            if (kotlin.math.abs(mStoredX) > SINGLE_AXIS_LOW_PASS_THRESHOLD) {
                GLFW.cursorX -= (if (mSwapXY) mStoredY else mStoredX) * xFactor
                mStoredX = 0f
                updatePosition = true
            }
            if (kotlin.math.abs(mStoredY) > SINGLE_AXIS_LOW_PASS_THRESHOLD) {
                GLFW.cursorY += (if (mSwapXY) mStoredX else mStoredY) * yFactor
                mStoredY = 0f
                updatePosition = true
            }
        }

        if (updatePosition) {
            GLFW.sendMousePos()
        }
    }

    fun updateOrientation() {
        val rotation = mWindowManager.defaultDisplay.rotation
        mSurfaceRotation = rotation
        when (rotation) {
            Surface.ROTATION_0 -> {
                mSwapXY = true
                xFactor = 1f
                yFactor = 1f
            }
            Surface.ROTATION_90 -> {
                mSwapXY = false
                xFactor = -1f
                yFactor = 1f
            }
            Surface.ROTATION_180 -> {
                mSwapXY = true
                xFactor = -1f
                yFactor = -1f
            }
            Surface.ROTATION_270 -> {
                mSwapXY = false
                xFactor = 1f
                yFactor = -1f
            }
        }

        if (LauncherPreferences.PREF_GYRO_INVERT_X) xFactor *= -1f
        if (LauncherPreferences.PREF_GYRO_INVERT_Y) yFactor *= -1f
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {}

    override fun onGrabState(isGrabbing: Boolean) {
        mWarmup = ROTATION_VECTOR_WARMUP_PERIOD
        mShouldHandleEvents = isGrabbing
    }

    private fun damperValue(newAngleDifference: FloatArray) {
        mHistoryIndex++
        if (mHistoryIndex >= mAngleBuffer.size) mHistoryIndex = 0

        xTotal -= mAngleBuffer[mHistoryIndex][1]
        yTotal -= mAngleBuffer[mHistoryIndex][2]

        System.arraycopy(newAngleDifference, 0, mAngleBuffer[mHistoryIndex], 0, 3)

        xTotal += mAngleBuffer[mHistoryIndex][1]
        yTotal += mAngleBuffer[mHistoryIndex][2]

        xAverage = xTotal / mAngleBuffer.size
        yAverage = yTotal / mAngleBuffer.size
    }

    private fun resetDamper() {
        mHistoryIndex = -1
        xTotal = 0f
        yTotal = 0f
        xAverage = 0f
        yAverage = 0f
        for (oldAngle in mAngleBuffer) {
            Arrays.fill(oldAngle, 0f)
        }
    }

    inner class OrientationCorrectionListener(context: Context) : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
        override fun onOrientationChanged(i: Int) {
            if (!mShouldHandleEvents) return
            if (i == ORIENTATION_UNKNOWN) return

            when (mSurfaceRotation) {
                Surface.ROTATION_90, Surface.ROTATION_270 -> {
                    mSwapXY = false
                    if (225 < i && i < 315) {
                        xFactor = -1f
                        yFactor = 1f
                    } else if (45 < i && i < 135) {
                        xFactor = 1f
                        yFactor = -1f
                    }
                }
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    mSwapXY = true
                    if ((315 < i && i <= 360) || (i < 45)) {
                        xFactor = 1f
                        yFactor = 1f
                    } else if (135 < i && i < 225) {
                        xFactor = -1f
                        yFactor = -1f
                    }
                }
            }

            if (LauncherPreferences.PREF_GYRO_INVERT_X) xFactor *= -1f
            if (LauncherPreferences.PREF_GYRO_INVERT_Y) yFactor *= -1f
        }
    }
}
