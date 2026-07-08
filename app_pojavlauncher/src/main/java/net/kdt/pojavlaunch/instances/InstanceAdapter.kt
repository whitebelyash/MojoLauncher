package net.kdt.pojavlaunch.instances

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.graphics.ColorUtils
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import fr.spse.extended_view.ExtendedTextView

class InstanceAdapter(extraEntries: Array<InstanceAdapterExtra>?) : BaseAdapter() {
    private var mInstances: Instances? = null
    private var mSelectionIndex: Int = 0
    private val mExtraEntires: Array<InstanceAdapterExtra>

    init {
        mExtraEntires = extraEntries ?: emptyArray()
    }

    override fun getCount(): Int {
        return if (mInstances == null) mExtraEntires.size else mInstances!!.list.size + mExtraEntires.size
    }

    override fun getItem(position: Int): Any? {
        if (mInstances == null) return mExtraEntires[position]
        val instanceListSize = mInstances!!.list.size
        val extraPosition = position - instanceListSize
        return when {
            position < instanceListSize -> mInstances!!.list[position]
            extraPosition in 0 until mExtraEntires.size -> mExtraEntires[extraPosition]
            else -> null
        }
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.item_version_profile_layout, parent, false)
        setView(v, position, true)
        return v
    }

    fun setViewInstance(v: View, i: DisplayInstance, idx: Int, displaySelection: Boolean) {
        val extendedTextView = v as ExtendedTextView
        val cachedIcon = InstanceIconProvider.fetchIcon(v.resources, i)
        extendedTextView.setCompoundDrawablesRelative(
            cachedIcon, null,
            extendedTextView.compoundsDrawables[2], null
        )

        var profileName = Tools.validOrNullString(i.name)
        var versionName = Tools.validOrNullString(i.versionId)

        if (Instance.VERSION_LATEST_RELEASE.equals(versionName, ignoreCase = true))
            versionName = v.context.getString(R.string.profiles_latest_release)
        else if (Instance.VERSION_LATEST_SNAPSHOT.equals(versionName, ignoreCase = true))
            versionName = v.context.getString(R.string.profiles_latest_snapshot)

        extendedTextView.text = when {
            versionName == null && profileName != null -> profileName
            versionName != null && profileName == null -> versionName
            else -> String.format("%s - %s", profileName, versionName)
        }

        if (idx == mSelectionIndex && displaySelection) {
            extendedTextView.setBackgroundColor(ColorUtils.setAlphaComponent(Color.WHITE, 60))
        } else {
            extendedTextView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    fun setViewExtra(v: View, extra: InstanceAdapterExtra) {
        val extendedTextView = v as ExtendedTextView
        extendedTextView.setCompoundDrawablesRelative(
            extra.icon, null,
            extendedTextView.compoundsDrawables[2], null
        )
        extendedTextView.text = v.context.getString(extra.name)
        extendedTextView.setBackgroundColor(Color.TRANSPARENT)
    }

    fun setView(v: View, index: Int, displaySelection: Boolean) {
        val `object` = getItem(index)
        when (`object`) {
            is DisplayInstance -> setViewInstance(v, `object`, index, displaySelection)
            is InstanceAdapterExtra -> setViewExtra(v, `object`)
        }
    }

    fun applySelectionIndex(index: Int) {
        mSelectionIndex = index
    }

    fun applyInstances(instances: Instances) {
        mInstances = instances
        mSelectionIndex = instances.selectedIndex
        notifyDataSetChanged()
    }
}
