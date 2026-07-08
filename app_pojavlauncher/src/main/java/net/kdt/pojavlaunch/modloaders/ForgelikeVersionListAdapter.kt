package net.kdt.pojavlaunch.modloaders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListAdapter
import android.widget.TextView
import java.util.ArrayList

class ForgelikeVersionListAdapter(
    forgeVersions: List<String>,
    private val mLayoutInflater: LayoutInflater,
    utils: ForgelikeUtils
) : BaseExpandableListAdapter(), ExpandableListAdapter {
    private val mGameVersions: MutableList<String>
    private val mLoaderVersions: MutableList<MutableList<String>>

    init {
        mGameVersions = ArrayList()
        mLoaderVersions = ArrayList()
        for (version in forgeVersions) {
            if (utils.shouldSkipVersion(version)) continue
            val gameVersion = utils.processVersionString(version)
            val versionList: MutableList<String>
            val gameVersionIndex = mGameVersions.indexOf(gameVersion)
            if (gameVersionIndex != -1) {
                versionList = mLoaderVersions[gameVersionIndex]
            } else {
                versionList = ArrayList()
                mGameVersions.add(gameVersion)
                mLoaderVersions.add(versionList)
            }
            versionList.add(version)
        }
        if (utils.isVersionOrderInversed()) {
            for (versionList in mLoaderVersions) reverseList(versionList)
            reverseList(mLoaderVersions)
            reverseList(mGameVersions)
        }
    }

    override fun getGroupCount() = mGameVersions.size
    override fun getChildrenCount(i: Int) = mLoaderVersions[i].size
    override fun getGroup(i: Int): Any = getGameVersion(i)
    override fun getChild(i: Int, i1: Int): Any = getForgeVersion(i, i1)
    override fun getGroupId(i: Int) = i.toLong()
    override fun getChildId(i: Int, i1: Int) = i1.toLong()
    override fun hasStableIds() = true

    override fun getGroupView(i: Int, b: Boolean, convertView: View?, viewGroup: ViewGroup): View {
        var cv = convertView
        if (cv == null) cv = mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, viewGroup, false)
        (cv as TextView).text = getGameVersion(i)
        return cv
    }

    override fun getChildView(i: Int, i1: Int, b: Boolean, convertView: View?, viewGroup: ViewGroup): View {
        var cv = convertView
        if (cv == null) cv = mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, viewGroup, false)
        (cv as TextView).text = getForgeVersion(i, i1)
        return cv
    }

    private fun getGameVersion(i: Int) = mGameVersions[i]
    private fun getForgeVersion(i: Int, i1: Int) = mLoaderVersions[i][i1]

    override fun isChildSelectable(i: Int, i1: Int) = true

    companion object {
        private fun <T> reverseList(list: MutableList<T>) {
            var i = 0
            var j = list.size - 1
            while (i < j) {
                val temp = list[i]
                list[i] = list[j]
                list[j] = temp
                i++
                j--
            }
        }
    }
}
