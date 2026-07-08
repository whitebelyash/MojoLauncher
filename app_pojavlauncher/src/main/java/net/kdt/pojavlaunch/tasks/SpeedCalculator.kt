package net.kdt.pojavlaunch.tasks

class SpeedCalculator(averageDepth: Int = 64) {
    private var mLastMillis: Long = 0
    private var mLastBytes: Long = 0
    private var mIndex = 0
    private val mPreviousInputs = DoubleArray(averageDepth)
    private var mSum = 0.0

    private fun addToAverage(speed: Double): Double {
        mSum -= mPreviousInputs[mIndex]
        mSum += speed
        mPreviousInputs[mIndex] = speed
        if (++mIndex == mPreviousInputs.size) mIndex = 0
        val dLength = mPreviousInputs.size.toDouble()
        return (mSum + dLength / 2.0) / dLength
    }

    fun feed(bytes: Long): Double {
        val millis = System.currentTimeMillis()
        val deltaBytes = bytes - mLastBytes
        val deltaMillis = millis - mLastMillis
        mLastBytes = bytes
        mLastMillis = millis
        val speed = deltaBytes.toDouble() / (deltaMillis.toDouble() / 1000.0)
        return addToAverage(speed)
    }
}
