package com.kdt

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Collections

class SimpleArrayAdapter<T>(objects: MutableList<T>?) : BaseAdapter() {
    private var mObjects: MutableList<T> = ArrayList()

    init {
        setObjects(objects)
    }

    fun setObjects(@Nullable objects: MutableList<T>?) {
        if (objects == null) {
            if (mObjects !== Collections.emptyList<T>()) {
                mObjects = ArrayList<T>()
                notifyDataSetChanged()
            }
        } else {
            if (objects !== mObjects) {
                mObjects = objects
                notifyDataSetChanged()
            }
        }
    }

    override fun getCount(): Int {
        return mObjects.size
    }

    override fun getItem(position: Int): T {
        return mObjects[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @NonNull
    override fun getView(position: Int, @Nullable convertView: View?, @NonNull parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        val v = view as TextView
        v.text = mObjects[position].toString()
        return v
    }
}
