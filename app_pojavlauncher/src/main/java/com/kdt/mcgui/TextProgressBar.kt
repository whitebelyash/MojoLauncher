package com.kdt.mcgui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.ProgressBar

import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat

import git.artdeell.mojo.R

class TextProgressBar : ProgressBar {
    private var mTextPadding = 0
    private var mTextPaint: Paint = Paint()
    private var mText = ""

    constructor(context: Context) : super(context, null, android.R.attr.progressBarStyleHorizontal) { init() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, android.R.attr.progressBarStyleHorizontal) { init() }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, android.R.attr.progressBarStyleHorizontal) { init() }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, android.R.attr.progressBarStyleHorizontal, defStyleRes) { init() }

    private fun init() {
        progressDrawable = ResourcesCompat.getDrawable(resources, R.drawable.view_text_progressbar, null)
        progress = 35
        mTextPaint.color = Color.WHITE
        mTextPaint.flags = Paint.FAKE_BOLD_TEXT_FLAG
        mTextPaint.isAntiAlias = true
    }

    @Synchronized override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mTextPaint.textSize = ((height - paddingBottom - paddingTop) * 0.55).toFloat()
        val xPos = Math.max(
            Math.min(
                (progress * width / max).toFloat() + mTextPadding,
                width.toFloat() - mTextPaint.measureText(mText) - mTextPadding
            ).toInt(),
            mTextPadding
        )
        val yPos = ((height / 2) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2)).toInt()

        canvas.drawText(mText, xPos.toFloat(), yPos.toFloat(), mTextPaint)
    }

    fun setText(@StringRes resid: Int) {
        setText(context.resources.getText(resid).toString())
    }

    fun setText(text: String) {
        mText = text
        invalidate()
    }

    fun setTextPadding(padding: Int) {
        mTextPadding = padding
    }
}
