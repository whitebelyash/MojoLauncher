package net.kdt.pojavlaunch.utils.interfaces

import android.widget.SeekBar

interface SimpleSeekBarListener : SeekBar.OnSeekBarChangeListener {
    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
}
