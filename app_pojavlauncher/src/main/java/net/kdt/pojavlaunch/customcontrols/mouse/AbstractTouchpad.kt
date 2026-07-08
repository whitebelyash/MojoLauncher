package net.kdt.pojavlaunch.customcontrols.mouse

interface AbstractTouchpad {
    fun getDisplayState(): Boolean

    fun applyMotionVector(vector: FloatArray) {
        applyMotionVector(vector[0], vector[1])
    }

    fun applyMotionVector(x: Float, y: Float)
    fun enable(supposed: Boolean)
    fun disable()
}
