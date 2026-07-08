package net.kdt.pojavlaunch.customcontrols.mouse

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.NonNull

class CursorContainer(
    private val drawable: Drawable,
    private val xHotspot: Int,
    private val yHotspot: Int
) {
    fun draw(@NonNull canvas: Canvas) {
        canvas.translate(-xHotspot.toFloat(), -yHotspot.toFloat())
        drawable.draw(canvas)
    }

    fun getDrawable(): Drawable = drawable
    fun getXHotspot(): Int = xHotspot
    fun getYHotspot(): Int = yHotspot
}
