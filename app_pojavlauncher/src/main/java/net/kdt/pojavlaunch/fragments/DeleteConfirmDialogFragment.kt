package net.kdt.pojavlaunch.fragments

import android.app.Dialog
import android.os.Bundle

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import java.io.IOException

class DeleteConfirmDialogFragment : DialogFragment() {
    private val mInstance: Instance? = Instances.loadSelectedInstance()

    @NonNull
    override fun onCreateDialog(@Nullable savedInstanceState: Bundle?): Dialog {
        if (mInstance == null) dismiss()
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.instance_delete)
            .setMessage(R.string.instance_delete_confirmation)
            .setPositiveButton(R.string.global_delete) { _, _ ->
                mInstance ?: return@setPositiveButton
                InstanceIconProvider.dropIcon(mInstance!!)
                Tools.removeCurrentFragment(requireActivity())
                try {
                    Instances.removeInstance(mInstance!!)
                } catch (e: IOException) {
                    Tools.showErrorRemote(e)
                }
            }
            .setNegativeButton(R.string.global_no, null)
            .create()
    }

    companion object {
        const val TAG = "delete_dialog_confirm"
    }
}
