package net.kdt.pojavlaunch.prefs.screens

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceCategory
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference
import net.kdt.pojavlaunch.prefs.LauncherPreferences

class LauncherPreferenceControlFragment : LauncherPreferenceFragment() {
    private var mGyroAvailable = false

    override fun onCreatePreferences(b: Bundle?, str: String?) {
        val longPressTrigger = LauncherPreferences.PREF_LONGPRESS_TRIGGER
        val prefButtonSize = LauncherPreferences.PREF_BUTTONSIZE.toInt()
        val mouseScale = (LauncherPreferences.PREF_MOUSESCALE * 100).toInt()
        val gyroSampleRate = LauncherPreferences.PREF_GYRO_SAMPLE_RATE
        val mouseSpeed = LauncherPreferences.PREF_MOUSESPEED
        val gyroSpeed = LauncherPreferences.PREF_GYRO_SENSITIVITY
        val joystickDeadzone = LauncherPreferences.PREF_DEADZONE_SCALE

        addPreferencesFromResource(R.xml.pref_control)

        val seek2 = requirePreference("timeLongPressTrigger", CustomSeekBarPreference::class.java)
        seek2.value = longPressTrigger
        seek2.suffix = " ms"

        val seek3 = requirePreference("buttonscale", CustomSeekBarPreference::class.java)
        seek3.value = prefButtonSize
        seek3.suffix = " %"

        val seek4 = requirePreference("mousescale", CustomSeekBarPreference::class.java)
        seek4.value = mouseScale
        seek4.suffix = " %"

        val seek6 = requirePreference("mousespeed", CustomSeekBarPreference::class.java)
        seek6.value = (mouseSpeed * 100f).toInt()
        seek6.suffix = " %"

        val deadzoneSeek = requirePreference("gamepad_deadzone_scale", CustomSeekBarPreference::class.java)
        deadzoneSeek.value = (joystickDeadzone * 100f).toInt()
        deadzoneSeek.suffix = " %"

        val context = context
        if (context != null) {
            mGyroAvailable = Tools.deviceSupportsGyro(context)
        }
        val gyroCategory = requirePreference("gyroCategory", PreferenceCategory::class.java)
        gyroCategory.isVisible = mGyroAvailable

        val gyroSensitivitySeek = requirePreference("gyroSensitivity", CustomSeekBarPreference::class.java)
        gyroSensitivitySeek.value = (gyroSpeed * 100f).toInt()
        gyroSensitivitySeek.suffix = " %"

        val gyroSampleRateSeek = requirePreference("gyroSampleRate", CustomSeekBarPreference::class.java)
        gyroSampleRateSeek.value = gyroSampleRate
        gyroSampleRateSeek.suffix = " ms"

        computeVisibility()
    }

    override fun onSharedPreferenceChanged(p: SharedPreferences, s: String) {
        super.onSharedPreferenceChanged(p, s)
        computeVisibility()
    }

    private fun computeVisibility() {
        requirePreference("timeLongPressTrigger").isVisible = !LauncherPreferences.PREF_DISABLE_GESTURES
        requirePreference("gyroSensitivity").isVisible = LauncherPreferences.PREF_ENABLE_GYRO
        requirePreference("gyroSampleRate").isVisible = LauncherPreferences.PREF_ENABLE_GYRO
        requirePreference("gyroInvertX").isVisible = LauncherPreferences.PREF_ENABLE_GYRO
        requirePreference("gyroInvertY").isVisible = LauncherPreferences.PREF_ENABLE_GYRO
        requirePreference("gyroSmoothing").isVisible = LauncherPreferences.PREF_ENABLE_GYRO
    }
}
