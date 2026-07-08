package net.kdt.pojavlaunch.multirt

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import git.artdeell.mojo.R

class MultiRTConfigDialog {
    private var mDialog: AlertDialog? = null
    private var mDialogView: RecyclerView? = null

    fun show() {
        refresh()
        mDialog!!.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        val adapter = mDialogView?.adapter
        adapter?.notifyDataSetChanged()
    }

    fun prepare(activity: Context, installJvmLauncher: ActivityResultLauncher<Any?>) {
        mDialogView = RecyclerView(activity)
        mDialogView!!.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        val adapter = RTRecyclerViewAdapter()
        mDialogView!!.adapter = adapter

        mDialog = AlertDialog.Builder(activity)
            .setTitle(R.string.multirt_config_title)
            .setView(mDialogView)
            .setPositiveButton(R.string.multirt_config_add) { _, _ -> installJvmLauncher.launch(null) }
            .setNeutralButton(R.string.multirt_delete_runtime, null)
            .create()

        mDialog!!.setOnShowListener { dialog ->
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL)
            button.setOnClickListener {
                val isEditing = !adapter.isEditing
                adapter.isEditing = isEditing
                button.setText(if (isEditing) R.string.multirt_config_setdefault else R.string.multirt_delete_runtime)
            }
        }
    }
}
