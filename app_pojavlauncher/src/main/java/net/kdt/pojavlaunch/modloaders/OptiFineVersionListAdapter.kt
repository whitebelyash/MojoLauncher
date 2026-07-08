package net.kdt.pojavlaunch.modloaders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListAdapter
import android.widget.TextView

class OptiFineVersionListAdapter(
    private val mOptiFineVersions: OptiFineUtils.OptiFineVersions,
    private val mLayoutInflater: LayoutInflater
) : BaseExpandableListAdapter(), ExpandableListAdapter {
    override fun getGroupCount() = mOptiFineVersions.gameVersions.size
    override fun getChildrenCount(i: Int) = mOptiFineVersions.optifineVersions[i].size
    override fun getGroup(i: Int): Any = mOptiFineVersions.gameVersions[i]!!
    override fun getChild(i: Int, i1: Int): Any = mOptiFineVersions.optifineVersions[i][i1]
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
        (cv as TextView).text = (getChild(i, i1) as OptiFineUtils.OptiFineVersion).versionName
        return cv
    }

    override fun isChildSelectable(i: Int, i1: Int) = true
}
