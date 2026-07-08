package net.kdt.pojavlaunch.customcontrols.mouse

import net.kdt.pojavlaunch.CallbackBridge

class Scroller(private val mScrollThreshold: Float) {
    private var mScrollOvershootH = 0f
    private var mScrollOvershootV = 0f

    fun performScroll(dx: Float, dy: Float) {
        val hScroll = dx / mScrollThreshold + mScrollOvershootH
        val vScroll = dy / mScrollThreshold + mScrollOvershootV
        val hScrollRound = hScroll.toInt()
        val vScrollRound = vScroll.toInt()
        if (hScrollRound != 0 || vScrollRound != 0) CallbackBridge.sendScroll(hScroll, vScroll)
        mScrollOvershootH = hScroll - hScrollRound
        mScrollOvershootV = vScroll - vScrollRound
    }

    fun performScroll(vector: FloatArray) {
        performScroll(vector[0], vector[1])
    }

    fun resetScrollOvershoot() {
        mScrollOvershootH = 0f
        mScrollOvershootV = 0f
    }
}
