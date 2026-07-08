package net.kdt.pojavlaunch.utils.interfaces

import android.view.View
import android.widget.AdapterView

interface SimpleItemSelectedListener : AdapterView.OnItemSelectedListener {
    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
