package net.kdt.pojavlaunch.prefs.screens

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.tasks.DataMigrator
import net.kdt.pojavlaunch.utils.GLInfoUtils
import net.kdt.pojavlaunch.utils.RendererCompatUtil

class LauncherPreferenceMiscellaneousFragment : LauncherPreferenceFragment() {

    private val mMigrateLauncher: ActivityResultLauncher<Uri?> = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            AlertDialog.Builder(getLauncherActivity())
                .setTitle(R.string.migration_progress_warning_title)
                .setMessage(R.string.migration_progress_warning_summary)
                .setPositiveButton(android.R.string.ok) { _, _ -> DataMigrator(getLauncherActivity(), uri).migrateData() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun onCreatePreferences(b: Bundle?, str: String?) {
        mVisibilityUpdater = Runnable { updateVisibility() }
        addPreferencesFromResource(R.xml.pref_misc)
        val driverPreference = requirePreference("zinkPreferSystemDriver")
        val packageManager = driverPreference.context.packageManager
        val supportsTurnip = RendererCompatUtil.checkVulkanSupport(packageManager) && GLInfoUtils.getGlInfo().isAdreno()
        driverPreference.isVisible = supportsTurnip
        val importPreference = requirePreference("runDataMigration")
        importPreference.setOnPreferenceClickListener {
            if (getLauncherActivity().hasActiveProcesses()) {
                Toast.makeText(context, R.string.tasks_ongoing, Toast.LENGTH_SHORT).show()
                return@setOnPreferenceClickListener true
            }
            mMigrateLauncher.launch(null)
            true
        }
        setupMicrophoneRequestPreference()
    }

    private fun updateVisibility() {
        requirePreference("microphoneAccessRequest").isVisible = !getLauncherActivity().checkForPermissionRationale(33, Manifest.permission.RECORD_AUDIO)
    }

    override fun onResume() {
        super.onResume()
    }

    private fun setupMicrophoneRequestPreference() {
        val mRequestMicrophonePermissionPreference = requirePreference("microphoneAccessRequest")
        val activity = activity
        if (activity is net.kdt.pojavlaunch.LauncherActivity) {
            mRequestMicrophonePermissionPreference.setOnPreferenceClickListener {
                activity.askForPermission(23, Manifest.permission.RECORD_AUDIO)
                true
            }
        } else {
            mRequestMicrophonePermissionPreference.isVisible = false
        }
        updateVisibility()
    }
}
