package net.kdt.pojavlaunch.prefs.screens

import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.preference.EditTextPreference
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference
import net.kdt.pojavlaunch.prefs.LauncherPreferences

class LauncherPreferenceJavaFragment : LauncherPreferenceFragment() {
    private var mDialogScreen: MultiRTConfigDialog? = null
    private val mVmInstallLauncher: ActivityResultLauncher<Any> =
        registerForActivityResult(OpenDocumentWithExtension("xz")) { data ->
            if (data != null) Tools.installRuntimeFromUri(context, data)
        }

    override fun onCreatePreferences(b: Bundle?, str: String?) {
        val ramAllocation = LauncherPreferences.PREF_RAM_ALLOCATION
        addPreferencesFromResource(R.xml.pref_java)

        val memorySeekbar = requirePreference("allocation", CustomSeekBarPreference::class.java)

        val maxRAM: Int
        val deviceRam = Tools.getTotalDeviceMemory(memorySeekbar.context)
        if (Architecture.is32BitsDevice() || deviceRam < 2048) maxRAM = Math.min(1024, deviceRam)
        else maxRAM = deviceRam - (if (deviceRam < 3064) 800 else 1024)

        memorySeekbar.setMaxKeepIncrement(maxRAM)
        memorySeekbar.value = ramAllocation
        memorySeekbar.suffix = " MB"

        val editJVMArgs = findPreference<EditTextPreference>("javaArgs")
        editJVMArgs?.setOnBindEditTextListener { it.isSingleLine = true }

        requirePreference("install_jre").setOnPreferenceClickListener {
            openMultiRTDialog()
            true
        }
    }

    private fun openMultiRTDialog() {
        if (mDialogScreen == null) {
            mDialogScreen = MultiRTConfigDialog()
            mDialogScreen!!.prepare(context, mVmInstallLauncher)
        }
        mDialogScreen!!.show()
    }
}
