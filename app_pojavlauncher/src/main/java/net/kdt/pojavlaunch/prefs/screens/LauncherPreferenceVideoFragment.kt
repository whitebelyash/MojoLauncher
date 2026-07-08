package net.kdt.pojavlaunch.prefs.screens

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.SwitchPreference
import androidx.preference.SwitchPreferenceCompat
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.plugins.LibraryPlugin
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.RendererCompatUtil

class LauncherPreferenceVideoFragment : LauncherPreferenceFragment() {
    override fun onCreatePreferences(b: Bundle?, str: String?) {
        addPreferencesFromResource(R.xml.pref_video)
        val resolution = (LauncherPreferences.PREF_SCALE_FACTOR * 100).toInt()

        val resolutionSeekbar = requirePreference("resolutionRatio", CustomSeekBarPreference::class.java)
        resolutionSeekbar.suffix = " %"

        if (resolution < 25) {
            resolutionSeekbar.value = 100
        } else {
            resolutionSeekbar.value = resolution
        }

        val sustainedPerfSwitch = requirePreference("sustainedPerformance", SwitchPreference::class.java)
        sustainedPerfSwitch.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        sustainedPerfSwitch.isChecked = LauncherPreferences.PREF_SUSTAINED_PERFORMANCE

        requirePreference("alternate_surface", SwitchPreferenceCompat::class.java).isChecked = LauncherPreferences.PREF_USE_ALTERNATE_SURFACE
        requirePreference("force_vsync", SwitchPreferenceCompat::class.java).isChecked = LauncherPreferences.PREF_FORCE_VSYNC

        val angle = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_ANGLE_PLUGIN)
        val angleSwitch = requirePreference("use_angle", SwitchPreferenceCompat::class.java)
        angleSwitch.isVisible = angle != null
        angleSwitch.isChecked = LauncherPreferences.PREF_USE_ANGLE

        val legacyZink = requirePreference("zinkForceLegacy", SwitchPreference::class.java)
        legacyZink.isChecked = LauncherPreferences.PREF_ZINK_FORCE_LEGACY
        if (!Architecture.isx86Device()) {
            val zink = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_ZINK_PLUGIN)
            legacyZink.isVisible = zink != null
        } else {
            legacyZink.isVisible = false
        }

        val rendererListPreference = requirePreference("renderer", ListPreference::class.java)
        val renderersList = RendererCompatUtil.getCompatibleRenderers(context)
        rendererListPreference.entries = renderersList.rendererDisplayNames
        rendererListPreference.entryValues = renderersList.rendererIds.toTypedArray()

        computeVisibility()
    }

    override fun onResume() {
        super.onResume()
        val activity = activity
        if (activity != null) {
            requirePreference("ignoreNotch").isVisible = LauncherPreferences.hasNotch(activity)
        }
    }

    override fun onSharedPreferenceChanged(p: SharedPreferences, s: String) {
        super.onSharedPreferenceChanged(p, s)
        computeVisibility()
    }

    private fun computeVisibility() {
        requirePreference("force_vsync", SwitchPreferenceCompat::class.java)
            .isVisible = LauncherPreferences.PREF_USE_ALTERNATE_SURFACE
    }
}
