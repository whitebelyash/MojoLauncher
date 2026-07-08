package net.kdt.pojavlaunch.utils.memory

import net.kdt.pojavlaunch.Architecture

class MemoryHoleFinder : SelfMapsParser.Callback {
    private var mPreviousEnd: Long = 0
    private var mLargestHole: Long = -1
    private val mAddressingLimit = Architecture.getAddressSpaceLimit()

    override fun process(begin: Long, end: Long, wholeLine: String): Boolean {
        var b = begin
        if (b >= mAddressingLimit) b = mAddressingLimit
        val holeSize = b - mPreviousEnd
        if (mLargestHole < holeSize) mLargestHole = holeSize
        if (b == mAddressingLimit) return false
        mPreviousEnd = end
        return true
    }

    fun getLargestHole(): Long = mLargestHole
}
