package net.kdt.pojavlaunch.utils.jre

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import net.kdt.pojavlaunch.ShowErrorActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask
import git.artdeell.mojo.R

class RuntimeSelectionException(
    private val mRuntimeState: Int,
    private val mRuntimeVersion: Int
) : Exception(), ContextExecutorTask {

    companion object {
        const val RUNTIME_STATE_INSTALLATION_FAILED = 0
        const val RUNTIME_STATE_SELECTION_FAILED = 1
        const val RUNTIME_STATE_INTERNAL_RUNTIME_MISSING = 2
        private const val serialVersionUID = -7482301619612640658L
    }

    override fun executeWithActivity(activity: Activity) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.runtime_error_title)
        val msgString = when (mRuntimeState) {
            RUNTIME_STATE_INSTALLATION_FAILED -> R.string.runtime_error_install_failed
            RUNTIME_STATE_INTERNAL_RUNTIME_MISSING -> R.string.runtime_error_missing
            RUNTIME_STATE_SELECTION_FAILED -> R.string.multirt_nocompatiblert
            else -> throw RuntimeException("Unknown runtime state")
        }
        builder.setMessage(activity.getString(msgString, mRuntimeVersion))
        builder.setPositiveButton(android.R.string.ok) { _: android.content.DialogInterface?, _: Int -> }
        if (mRuntimeState == RUNTIME_STATE_INSTALLATION_FAILED || cause != null) {
            builder.setNegativeButton(R.string.error_show_more) { _: android.content.DialogInterface?, _: Int ->
                Tools.showError(activity, R.string.runtime_error_title, cause, activity is ShowErrorActivity)
            }
        }
        ShowErrorActivity.installRemoteDialogHandling(activity, builder)
        builder.show()
    }

    override fun executeWithApplication(context: Context) {}
}
