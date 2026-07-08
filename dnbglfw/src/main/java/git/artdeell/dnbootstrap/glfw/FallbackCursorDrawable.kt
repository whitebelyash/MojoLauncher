package git.artdeell.dnbootstrap.glfw

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.annotation.NonNull
import androidx.annotation.Nullable

class FallbackCursorDrawable : Drawable() {
    private val fallbackRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.RED
    }

    override fun draw(@NonNull canvas: Canvas) {
        canvas.drawRect(bounds, fallbackRectPaint)
    }

    override fun getOpacity(): Int = PixelFormat.TRANSPARENT

    override fun setAlpha(i: Int) {}

    override fun setColorFilter(@Nullable colorFilter: ColorFilter?) {}
}
