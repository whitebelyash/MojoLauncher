package net.kdt.pojavlaunch.colorselector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import top.defaults.checkerboarddrawable.CheckerboardDrawable

class ColorSideBySideView : View {
    private val mPaint = Paint()
    private val mCheckerboardDrawable = CheckerboardDrawable.create()
    private var mColor = 0
    private var mAlphaColor = 0
    private var mWidth = 0f
    private var mHeight = 0f
    private var mHalfHeight = 0f

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mPaint.isAntiAlias = true
    }

    fun setColor(color: Int) {
        mColor = ColorSelector.setAlpha(color, 0xff)
        mAlphaColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        mCheckerboardDrawable.draw(canvas)
        mPaint.color = mColor
        canvas.drawRect(0f, 0f, mWidth, mHalfHeight, mPaint)
        mPaint.color = mAlphaColor
        canvas.drawRect(0f, mHalfHeight, mWidth, mHeight, mPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, old_w: Int, old_h: Int) {
        mHalfHeight = h / 2f
        mWidth = w.toFloat()
        mHeight = h.toFloat()
    }
}
