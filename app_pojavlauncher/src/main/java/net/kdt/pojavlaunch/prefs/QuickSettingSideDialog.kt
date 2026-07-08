package net.kdt.pojavlaunch.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.kdt.CustomSeekbar
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_DISABLE_GESTURES
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_ENABLE_GYRO
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_GYRO_INVERT_X
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_GYRO_INVERT_Y
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_GYRO_SENSITIVITY
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_LONGPRESS_TRIGGER
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_MOUSESPEED
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_SCALE_FACTOR
import net.kdt.pojavlaunch.utils.interfaces.SimpleSeekBarListener

abstract class QuickSettingSideDialog : com.kdt.SideDialogView {

    private var mEditor: SharedPreferences.Editor? = null
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var mGyroSwitch: Switch? = null
    private var mGyroXSwitch: Switch? = null
    private var mGyroYSwitch: Switch? = null
    private var mGestureSwitch: Switch? = null
    private var mGyroSensitivityBar: CustomSeekbar? = null
    private var mMouseSpeedBar: CustomSeekbar? = null
    private var mGestureDelayBar: CustomSeekbar? = null
    private var mResolutionBar: CustomSeekbar? = null
    private var mGyroSensitivityText: TextView? = null
    private var mGyroSensitivityDisplayText: TextView? = null
    private var mMouseSpeedText: TextView? = null
    private var mGestureDelayText: TextView? = null
    private var mGestureDelayDisplayText: TextView? = null
    private var mResolutionText: TextView? = null

    private var mOriginalGyroEnabled = false
    private var mOriginalGyroXEnabled = false
    private var mOriginalGyroYEnabled = false
    private var mOriginalGestureDisabled = false
    private var mOriginalGyroSensitivity = 0f
    private var mOriginalMouseSpeed = 0f
    private var mOriginalResolution = 0f
    private var mOriginalGestureDelay = 0

    constructor(context: Context, parent: ViewGroup) : super(context, parent, R.layout.dialog_quick_setting) {
        setTitle(R.string.quick_setting_title)
        setupCancelButton()
    }

    override fun onInflate() {
        bindLayout()
        Tools.runOnUiThread {
            setupListeners()
            updateGyroCompatibility()
        }
    }

    override fun onDestroy() {
        removeListeners()
    }

    private fun bindLayout() {
        mGyroSwitch = mDialogContent!!.findViewById(R.id.checkboxGyro)
        mGyroXSwitch = mDialogContent!!.findViewById(R.id.checkboxGyroX)
        mGyroYSwitch = mDialogContent!!.findViewById(R.id.checkboxGyroY)
        mGestureSwitch = mDialogContent!!.findViewById(R.id.checkboxGesture)

        mGyroSensitivityBar = mDialogContent!!.findViewById(R.id.editGyro_seekbar)
        mMouseSpeedBar = mDialogContent!!.findViewById(R.id.editMouseSpeed_seekbar)
        mGestureDelayBar = mDialogContent!!.findViewById(R.id.editGestureDelay_seekbar)
        mResolutionBar = mDialogContent!!.findViewById(R.id.editResolution_seekbar)

        mGyroSensitivityText = mDialogContent!!.findViewById(R.id.editGyro_textView_percent)
        mGyroSensitivityDisplayText = mDialogContent!!.findViewById(R.id.editGyro_textView)
        mMouseSpeedText = mDialogContent!!.findViewById(R.id.editMouseSpeed_textView_percent)
        mGestureDelayText = mDialogContent!!.findViewById(R.id.editGestureDelay_textView_percent)
        mGestureDelayDisplayText = mDialogContent!!.findViewById(R.id.editGestureDelay_textView)
        mResolutionText = mDialogContent!!.findViewById(R.id.editResolution_textView_percent)
    }

    private fun setupListeners() {
        mEditor = LauncherPreferences.DEFAULT_PREF!!.edit()

        mOriginalGyroEnabled = PREF_ENABLE_GYRO
        mOriginalGyroXEnabled = PREF_GYRO_INVERT_X
        mOriginalGyroYEnabled = PREF_GYRO_INVERT_Y
        mOriginalGestureDisabled = PREF_DISABLE_GESTURES

        mOriginalGyroSensitivity = PREF_GYRO_SENSITIVITY
        mOriginalMouseSpeed = PREF_MOUSESPEED
        mOriginalGestureDelay = PREF_LONGPRESS_TRIGGER
        mOriginalResolution = PREF_SCALE_FACTOR

        mGyroSwitch!!.isChecked = mOriginalGyroEnabled
        mGyroXSwitch!!.isChecked = mOriginalGyroXEnabled
        mGyroYSwitch!!.isChecked = mOriginalGyroYEnabled
        mGestureSwitch!!.isChecked = mOriginalGestureDisabled

        mGyroSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            PREF_ENABLE_GYRO = isChecked
            onGyroStateChanged()
            updateGyroVisibility(isChecked)
            mEditor!!.putBoolean("enableGyro", isChecked)
        }

        mGyroXSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            PREF_GYRO_INVERT_X = isChecked
            onGyroStateChanged()
            mEditor!!.putBoolean("gyroInvertX", isChecked)
        }

        mGyroYSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            PREF_GYRO_INVERT_Y = isChecked
            onGyroStateChanged()
            mEditor!!.putBoolean("gyroInvertY", isChecked)
        }

        mGestureSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            PREF_DISABLE_GESTURES = isChecked
            updateGestureVisibility(isChecked)
            mEditor!!.putBoolean("disableGestures", isChecked)
        }

        mGyroSensitivityBar!!.setOnSeekBarChangeListener(object : SimpleSeekBarListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                PREF_GYRO_SENSITIVITY = progress / 100f
                mEditor!!.putInt("gyroSensitivity", progress)
                setSeekTextPercent(mGyroSensitivityText!!, progress)
            }
        })
        mGyroSensitivityBar!!.progress = (mOriginalGyroSensitivity * 100f).toInt()
        setSeekTextPercent(mGyroSensitivityText!!, mGyroSensitivityBar!!.progress)

        mMouseSpeedBar!!.setOnSeekBarChangeListener(object : SimpleSeekBarListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                PREF_MOUSESPEED = progress / 100f
                mEditor!!.putInt("mousespeed", progress)
                setSeekTextPercent(mMouseSpeedText!!, progress)
            }
        })
        mMouseSpeedBar!!.progress = (mOriginalMouseSpeed * 100f).toInt()
        setSeekTextPercent(mMouseSpeedText!!, mMouseSpeedBar!!.progress)

        mGestureDelayBar!!.setOnSeekBarChangeListener(object : SimpleSeekBarListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                PREF_LONGPRESS_TRIGGER = progress
                mEditor!!.putInt("timeLongPressTrigger", progress)
                setSeekTextMillisecond(mGestureDelayText!!, progress)
            }
        })
        mGestureDelayBar!!.progress = mOriginalGestureDelay
        setSeekTextMillisecond(mGestureDelayText!!, mGestureDelayBar!!.progress)

        mResolutionBar!!.setOnSeekBarChangeListener(object : SimpleSeekBarListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                PREF_SCALE_FACTOR = progress / 100f
                mEditor!!.putInt("resolutionRatio", progress)
                setSeekTextPercent(mResolutionText!!, progress)
                onResolutionChanged()
            }
        })
        mResolutionBar!!.progress = (mOriginalResolution * 100).toInt()
        setSeekTextPercent(mResolutionText!!, mResolutionBar!!.progress)

        updateGyroVisibility(mOriginalGyroEnabled)
        updateGestureVisibility(mOriginalGestureDisabled)
    }

    private fun updateGyroVisibility(isEnabled: Boolean) {
        val visibility = if (isEnabled) View.VISIBLE else View.GONE
        mGyroXSwitch!!.visibility = visibility
        mGyroYSwitch!!.visibility = visibility
        mGyroSensitivityBar!!.visibility = visibility
        mGyroSensitivityText!!.visibility = visibility
        mGyroSensitivityDisplayText!!.visibility = visibility
    }

    private fun updateGyroCompatibility() {
        val isGyroAvailable = Tools.deviceSupportsGyro(mDialogContent!!.context)
        if (!isGyroAvailable) {
            mGyroSwitch!!.visibility = View.GONE
            updateGestureVisibility(false)
        }
    }

    private fun updateGestureVisibility(isDisabled: Boolean) {
        val visibility = if (isDisabled) View.GONE else View.VISIBLE
        mGestureDelayBar!!.visibility = visibility
        mGestureDelayText!!.visibility = visibility
        mGestureDelayDisplayText!!.visibility = visibility
    }

    private fun removeListeners() {
        mGyroSwitch!!.setOnCheckedChangeListener(null)
        mGyroXSwitch!!.setOnCheckedChangeListener(null)
        mGyroYSwitch!!.setOnCheckedChangeListener(null)
        mGestureSwitch!!.setOnCheckedChangeListener(null)
        mGyroSensitivityBar!!.setOnSeekBarChangeListener(null)
        mMouseSpeedBar!!.setOnSeekBarChangeListener(null)
        mGestureDelayBar!!.setOnSeekBarChangeListener(null)
        mResolutionBar!!.setOnSeekBarChangeListener(null)
    }

    private fun setupCancelButton() {
        setStartButtonListener(android.R.string.cancel) { cancel() }
        setEndButtonListener(android.R.string.ok) {
            mEditor!!.apply()
            disappear(true)
        }
    }

    fun cancel() {
        if (isDisplaying()) {
            PREF_ENABLE_GYRO = mOriginalGyroEnabled
            PREF_GYRO_INVERT_X = mOriginalGyroXEnabled
            PREF_GYRO_INVERT_Y = mOriginalGyroYEnabled
            PREF_DISABLE_GESTURES = mOriginalGestureDisabled

            PREF_GYRO_SENSITIVITY = mOriginalGyroSensitivity
            PREF_MOUSESPEED = mOriginalMouseSpeed
            PREF_LONGPRESS_TRIGGER = mOriginalGestureDelay
            PREF_SCALE_FACTOR = mOriginalResolution

            onGyroStateChanged()
            onResolutionChanged()
        }
        disappear(true)
    }

    abstract fun onResolutionChanged()
    abstract fun onGyroStateChanged()

    companion object {
        private fun setSeekTextMillisecond(target: TextView, value: Int) {
            setSeekText(target, R.string.millisecond_format, value)
        }

        private fun setSeekTextPercent(target: TextView, value: Int) {
            setSeekText(target, R.string.percent_format, value)
        }

        private fun setSeekText(target: TextView, format: Int, value: Int) {
            target.text = target.context.getString(format, value)
        }
    }
}
