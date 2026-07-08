package net.kdt.pojavlaunch.modloaders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListAdapter
import android.widget.TextView
import git.artdeell.mojo.R
import java.util.ArrayList

class BTAVersionListAdapter(
    versionList: BTAUtils.BTAVersionList,
    private val mLayoutInflater: LayoutInflater
) : BaseExpandableListAdapter(), ExpandableListAdapter {
    private val mGroupNames: ArrayList<String>
    private val mGroups: ArrayList<List<BTAUtils.BTAVersion>>

    init {
        val context = mLayoutInflater.context
        mGroupNames = ArrayList(2)
        mGroups = ArrayList(2)
        if (versionList.testedVersions.isNotEmpty()) {
            mGroupNames.add(context.getString(R.string.bta_installer_available_versions))
            mGroups.add(versionList.testedVersions)
        }
        if (versionList.untestedVersions.isNotEmpty()) {
            mGroupNames.add(context.getString(R.string.bta_installer_untested_versions))
            mGroups.add(versionList.untestedVersions)
        }
        if (!versionList.nightlyVersions.isNullOrEmpty()) {
            mGroupNames.add(context.getString(R.string.bta_installer_nightly_versions))
            mGroups.add(versionList.nightlyVersions)
        }
        mGroupNames.trimToSize()
        mGroups.trimToSize()
    }

    override fun getGroupCount() = mGroups.size
    override fun getChildrenCount(i: Int) = mGroups[i].size
    override fun getGroup(i: Int): Any = mGroupNames[i]
    override fun getChild(i: Int, i1: Int): Any = mGroups[i][i1]
    override fun getGroupId(i: Int) = i.toLong()
    override fun getChildId(i: Int, i1: Int) = i1.toLong()
    override fun hasStableIds() = true

    override fun getGroupView(i: Int, b: Boolean, convertView: View?, viewGroup: ViewGroup): View {
        var cv = convertView
        if (cv == null) cv = mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, viewGroup, false)
        (cv as TextView).text = getGroup(i) as String
        return cv
    }

    override fun getChildView(i: Int, i1: Int, b: Boolean, convertView: View?, viewGroup: ViewGroup): View {
        var cv = convertView
        if (cv == null) cv = mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, viewGroup, false)
        (cv as TextView).text = (getChild(i, i1) as BTAUtils.BTAVersion).versionName
        return cv
    }

    override fun isChildSelectable(i: Int, i1: Int) = true
}
