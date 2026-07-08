package net.kdt.pojavlaunch.prefs.screens

import android.os.Bundle
import androidx.preference.SwitchPreference
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.utils.GLInfoUtils

class LauncherPreferenceExperimentalFragment : LauncherPreferenceFragment() {

    override fun onCreatePreferences(b: Bundle?, str: String?) {
        addPreferencesFromResource(R.xml.pref_experimental)
        val pref = requirePreference("freedrenoSysmem", SwitchPreference::class.java)
        val hasFreedreno = GLInfoUtils.getGlInfo().isAdreno()
        pref.isVisible = hasFreedreno
    }
}
