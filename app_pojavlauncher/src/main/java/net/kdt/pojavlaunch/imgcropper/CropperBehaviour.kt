package net.kdt.pojavlaunch.imgcropper

import android.graphics.Bitmap
import android.graphics.Canvas

interface CropperBehaviour {
    fun getLargestImageSide(): Int
    fun drawPreHighlight(canvas: Canvas)
    fun onSelectionRectUpdated()
    fun resetTransforms()
    fun applyImage()
    fun pan(dx: Float, dy: Float)
    fun zoom(dz: Float, originX: Float, originY: Float)
    fun crop(targetMaxSide: Int): Bitmap?

    companion object {
        val DUMMY = object : CropperBehaviour {
            override fun getLargestImageSide(): Int = 0
            override fun drawPreHighlight(canvas: Canvas) {}
            override fun onSelectionRectUpdated() {}
            override fun resetTransforms() {}
            override fun applyImage() {}
            override fun pan(dx: Float, dy: Float) {}
            override fun zoom(dz: Float, originX: Float, originY: Float) {}
            override fun crop(targetMaxSide: Int): Bitmap? = null
        }
    }
}
