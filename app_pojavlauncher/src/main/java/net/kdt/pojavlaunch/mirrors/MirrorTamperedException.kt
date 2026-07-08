package net.kdt.pojavlaunch.mirrors

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.text.Html
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.ShowErrorActivity
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask
import net.kdt.pojavlaunch.prefs.LauncherPreferences

class MirrorTamperedException : Exception(), ContextExecutorTask {
    private companion object {
        private const val serialVersionUID = -7482301619612640658L
    }

    override fun executeWithActivity(activity: Activity) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.dl_tampered_manifest_title)
        builder.setMessage(Html.fromHtml(activity.getString(R.string.dl_tampered_manifest)))
        addButtons(builder)
        ShowErrorActivity.installRemoteDialogHandling(activity, builder)
        builder.show()
    }

    private fun addButtons(builder: AlertDialog.Builder) {
        builder.setPositiveButton(R.string.dl_switch_to_official_site) { _, _ ->
            LauncherPreferences.DEFAULT_PREF.edit().putString("downloadSource", "default").apply()
            LauncherPreferences.PREF_DOWNLOAD_SOURCE = "default"
        }
        builder.setNegativeButton(R.string.dl_turn_off_manifest_checks) { _, _ ->
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean("verifyManifest", false).apply()
            LauncherPreferences.PREF_VERIFY_MANIFEST = false
        }
        builder.setNeutralButton(android.R.string.cancel) { _, _ -> }
    }

    override fun executeWithApplication(context: Context) {}
}
