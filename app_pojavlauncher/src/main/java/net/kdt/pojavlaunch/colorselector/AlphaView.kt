package net.kdt.pojavlaunch.colorselector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.math.MathUtils
import net.kdt.pojavlaunch.Tools
import top.defaults.checkerboarddrawable.CheckerboardDrawable

class AlphaView : View {
    private val mCheckerboardDrawable = CheckerboardDrawable.create()
    private val mShaderPaint = Paint()
    private val mBlackPaint: Paint
    private val mViewSize = RectF(0f, 0f, 0f, 0f)
    private var mAlphaSelectionListener: AlphaSelectionListener? = null
    private var mSelectedAlpha = 0
    private var mAlphaDiv = 0f
    private var mScreenDiv = 0f
    private var mWidthThird = 0f

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs) {
        mBlackPaint = Paint()
        mBlackPaint.strokeWidth = Tools.dpToPx(3)
        mBlackPaint.color = Color.BLACK
    }

    fun setAlphaSelectionListener(alphaSelectionListener: AlphaSelectionListener?) {
        mAlphaSelectionListener = alphaSelectionListener
    }

    fun setAlpha(alpha: Int) {
        mSelectedAlpha = alpha
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        mSelectedAlpha = MathUtils.clamp((mAlphaDiv * event.y).toInt(), 0, 0xff)
        mAlphaSelectionListener?.onAlphaSelected(mSelectedAlpha)
        invalidate()
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, old_w: Int, old_h: Int) {
        mViewSize.right = w.toFloat()
        mViewSize.bottom = h.toFloat()
        mShaderPaint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0, Color.WHITE, Shader.TileMode.REPEAT)
        mAlphaDiv = 255f / mViewSize.bottom
        mScreenDiv = mViewSize.bottom / 255f
        mWidthThird = mViewSize.right / 3f
    }

    override fun onDraw(canvas: Canvas) {
        mCheckerboardDrawable.draw(canvas)
        canvas.drawRect(mViewSize, mShaderPaint)
        val linePos = mSelectedAlpha * mScreenDiv
        canvas.drawLine(0f, linePos, mWidthThird, linePos, mBlackPaint)
        canvas.drawLine(mWidthThird * 2, linePos, right.toFloat(), linePos, mBlackPaint)
    }
}
