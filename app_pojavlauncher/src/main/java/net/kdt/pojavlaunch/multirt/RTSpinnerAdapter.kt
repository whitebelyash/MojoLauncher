package net.kdt.pojavlaunch.multirt

import android.content.Context
import android.database.DataSetObserver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SpinnerAdapter
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import git.artdeell.mojo.R

class RTSpinnerAdapter(
    @NonNull private val mContext: Context,
    val mRuntimes: MutableList<Runtime>
) : SpinnerAdapter {
    init {
        val runtime = Runtime("<Default>", "", null, 0)
        mRuntimes.add(runtime)
    }

    override fun registerDataSetObserver(observer: DataSetObserver) {}
    override fun unregisterDataSetObserver(observer: DataSetObserver) {}
    override fun getCount(): Int = mRuntimes.size
    override fun getItem(position: Int): Any = mRuntimes[position]
    override fun getItemId(position: Int): Long = mRuntimes[position].name.hashCode().toLong()
    override fun hasStableIds(): Boolean = true

    @NonNull
    override fun getView(
        position: Int,
        @Nullable convertView: View?,
        @NonNull parent: ViewGroup
    ): View {
        val view = convertView ?: LayoutInflater.from(mContext)
            .inflate(R.layout.item_simple_list_1, parent, false)

        val runtime = mRuntimes[position]
        if (position == mRuntimes.size - 1) {
            (view as TextView).text = runtime.name
        } else {
            (view as TextView).text = String.format(
                "%s - %s",
                runtime.name.replace(".tar.xz", ""),
                runtime.versionString
                    ?: view.resources.getString(R.string.multirt_runtime_corrupt)
            )
        }
        return view
    }

    override fun getItemViewType(position: Int): Int = 0
    override fun getViewTypeCount(): Int = 1
    override fun isEmpty(): Boolean = mRuntimes.isEmpty()

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }
}
