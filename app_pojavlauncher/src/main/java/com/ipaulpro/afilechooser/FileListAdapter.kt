package com.ipaulpro.afilechooser

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import java.io.File
import java.util.ArrayList
import java.util.List

import git.artdeell.mojo.R

class FileListAdapter(context: Context) : BaseAdapter() {
    private val mInflater = LayoutInflater.from(context)
    private var mData = ArrayList<File>()

    private val ICON_FOLDER = R.drawable.ic_px_folder
    private val ICON_FILE = R.drawable.ic_px_file

    fun add(file: File) {
        mData.add(file)
        notifyDataSetChanged()
    }

    fun remove(file: File) {
        mData.remove(file)
        notifyDataSetChanged()
    }

    fun insert(file: File, index: Int) {
        mData.add(index, file)
        notifyDataSetChanged()
    }

    fun clear() {
        mData.clear()
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): File {
        return mData[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return mData.size
    }

    fun getListItems(): List<File> {
        return mData as List<File>
    }

    fun setListItems(data: List<File>) {
        mData = data as ArrayList<File>
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = convertView
        if (row == null) row = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false)

        val view = row as TextView
        val file = getItem(position)
        view.text = file.name

        val icon = if (file.isDirectory) ICON_FOLDER else ICON_FILE
        view.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
        view.compoundDrawablePadding = 20
        return row
    }
}
