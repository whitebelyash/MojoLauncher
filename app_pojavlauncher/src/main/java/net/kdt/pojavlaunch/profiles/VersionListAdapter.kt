package net.kdt.pojavlaunch.profiles

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListAdapter
import android.widget.TextView
import net.kdt.pojavlaunch.JVersionList
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.FilteredSubList
import java.io.File
import java.util.*

class VersionListAdapter(
    versionList: Array<JVersionList.Version>,
    hideCustomVersions: Boolean,
    ctx: Context
) : BaseExpandableListAdapter(), ExpandableListAdapter {

    private val mLayoutInflater: LayoutInflater =
        ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val mHideCustomVersions = hideCustomVersions
    private val mGroups: Array<String>
    private val mData: Array<List<*>>
    val mSnapshotListPosition: Int
    private val mInstalledVersions: Array<String>?

    init {
        val releaseList = FilteredSubList(versionList) { item -> item.type == "release" }
        val snapshotList = FilteredSubList(versionList) { item -> item.type == "snapshot" }
        val betaList = FilteredSubList(versionList) { item -> item.type == "old_beta" }
        val alphaList = FilteredSubList(versionList) { item -> item.type == "old_alpha" }

        mInstalledVersions = File(Tools.DIR_GAME_NEW + "/versions").list()
        mInstalledVersions?.sort()

        if (!areInstalledVersionsAvailable()) {
            mGroups = arrayOf(
                ctx.getString(R.string.mcl_setting_veroption_release),
                ctx.getString(R.string.mcl_setting_veroption_snapshot),
                ctx.getString(R.string.mcl_setting_veroption_oldbeta),
                ctx.getString(R.string.mcl_setting_veroption_oldalpha)
            )
            mData = arrayOf(releaseList, snapshotList, betaList, alphaList)
            mSnapshotListPosition = 1
        } else {
            mGroups = arrayOf(
                ctx.getString(R.string.mcl_setting_veroption_installed),
                ctx.getString(R.string.mcl_setting_veroption_release),
                ctx.getString(R.string.mcl_setting_veroption_snapshot),
                ctx.getString(R.string.mcl_setting_veroption_oldbeta),
                ctx.getString(R.string.mcl_setting_veroption_oldalpha)
            )
            mData = arrayOf(
                Arrays.asList(*mInstalledVersions),
                releaseList, snapshotList, betaList, alphaList
            )
            mSnapshotListPosition = 2
        }
    }

    override fun getGroupCount(): Int = mGroups.size
    override fun getChildrenCount(groupPosition: Int): Int = mData[groupPosition].size
    override fun getGroup(groupPosition: Int): Any = mData[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return if (isInstalledVersionSelected(groupPosition)) {
            mInstalledVersions!![childPosition]
        } else {
            (mData[groupPosition][childPosition] as JVersionList.Version).id
        }
    }

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()
    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()
    override fun hasStableIds(): Boolean = true

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val v = convertView ?: mLayoutInflater.inflate(
            android.R.layout.simple_expandable_list_item_1, parent, false
        )
        (v as TextView).text = mGroups[groupPosition]
        return v
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val v = convertView ?: mLayoutInflater.inflate(
            android.R.layout.simple_expandable_list_item_1, parent, false
        )
        (v as TextView).text = getChild(groupPosition, childPosition) as String
        return v
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    fun isSnapshotSelected(groupPosition: Int): Boolean = groupPosition == mSnapshotListPosition

    private fun areInstalledVersionsAvailable(): Boolean {
        return !mHideCustomVersions && mInstalledVersions != null && mInstalledVersions.isNotEmpty()
    }

    private fun isInstalledVersionSelected(groupPosition: Int): Boolean {
        return groupPosition == 0 && areInstalledVersionsAvailable()
    }
}
