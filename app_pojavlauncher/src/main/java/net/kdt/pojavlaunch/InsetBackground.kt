package net.kdt.pojavlaunch

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Insets
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi

@RequiresApi(29)
class InsetBackground(insets: Insets, bgColor: Int) : Drawable() {
    private val mLeftRect = Rect()
    private val mTopRect = Rect()
    private val mRightRect = Rect()
    private val mBottomRect = Rect()
    private val mRectPaint = Paint()
    private val mInsets: Insets

    init {
        Log.i("InsetBackground", insets.toString())
        mInsets = insets
        mRectPaint.color = bgColor
    }

    private fun computeRects(width: Int, height: Int) {
        mLeftRect.set(0, 0, mInsets.left, height)
        mTopRect.set(mInsets.left, 0, width - mInsets.right, mInsets.top)
        mRightRect.set(width - mInsets.right, 0, width, height)
        mBottomRect.set(0, height - mInsets.bottom, width, height)
    }

    override fun onBoundsChange(bounds: Rect) {
        computeRects(bounds.width(), bounds.height())
        invalidateSelf()
    }

    override fun draw(@NonNull canvas: Canvas) {
        canvas.drawRect(mLeftRect, mRectPaint)
        canvas.drawRect(mRightRect, mRectPaint)
        canvas.drawRect(mTopRect, mRectPaint)
        canvas.drawRect(mBottomRect, mRectPaint)
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(@Nullable colorFilter: ColorFilter?) {}

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }
}
