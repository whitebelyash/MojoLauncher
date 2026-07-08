package net.kdt.pojavlaunch.profiles

import net.kdt.pojavlaunch.extra.ExtraCore.getValue
import android.content.Context
import android.view.LayoutInflater
import android.widget.ExpandableListView
import androidx.appcompat.app.AlertDialog
import net.kdt.pojavlaunch.JVersionList
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.extra.ExtraConstants

object VersionSelectorDialog {
    fun open(context: Context, hideCustomVersions: Boolean, listener: VersionSelectorListener) {
        val builder = AlertDialog.Builder(context)
        val expandableListView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_expendable_list_view, null) as ExpandableListView
        val jVersionList = getValue(ExtraConstants.RELEASE_TABLE) as? JVersionList
        val versionArray = jVersionList?.versions ?: emptyArray()
        val adapter = VersionListAdapter(versionArray, hideCustomVersions, context)

        expandableListView.setAdapter(adapter)
        builder.setView(expandableListView)
        val dialog = builder.show()

        expandableListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val version = adapter.getChild(groupPosition, childPosition) as String
            listener.onVersionSelected(version, adapter.isSnapshotSelected(groupPosition))
            dialog.dismiss()
            true
        }
    }
}
