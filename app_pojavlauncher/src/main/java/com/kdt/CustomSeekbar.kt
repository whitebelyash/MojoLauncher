package com.kdt

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.annotation.Nullable
import git.artdeell.mojo.R

@SuppressLint("AppCompatCustomView")
class CustomSeekbar : SeekBar {
    private var mMin = 0
    private var mIncrement = 1
    private var mListener: OnSeekBarChangeListener? = null

    private val mInternalListener = object : OnSeekBarChangeListener {
        private var internalChanges = false
        private var previousProgress = 0

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (internalChanges) return
            internalChanges = true

            var newProgress = progress + mMin
            newProgress = applyIncrement(newProgress)

            if (newProgress != previousProgress) {
                if (mListener != null) {
                    previousProgress = newProgress
                    mListener!!.onProgressChanged(seekBar, newProgress, fromUser)
                }
            }

            setProgress(newProgress)
            internalChanges = false
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            if (internalChanges) return
            mListener?.onStartTrackingTouch(seekBar)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            if (internalChanges) return
            internalChanges = true
            setProgress(seekBar.progress)
            mListener?.onStopTrackingTouch(seekBar)
            internalChanges = false
        }
    }

    constructor(context: Context) : super(context) {
        setup(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setup(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setup(attrs)
    }

    fun setIncrement(increment: Int) {
        mIncrement = increment
    }

    fun setRange(min: Int, max: Int) {
        mMin = min
        super.setMax(max - min)
    }

    @Synchronized override fun setProgress(progress: Int) {
        super.setProgress(applyIncrement(progress - mMin))
    }

    override fun setProgress(progress: Int, animate: Boolean) {
        super.setProgress(applyIncrement(progress - mMin), animate)
    }

    @Synchronized override fun getProgress(): Int {
        return applyIncrement(super.getProgress() + mMin)
    }

    @Synchronized override fun setMin(min: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.setMin(0)
        }
        mMin = min
    }

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        mListener = l
    }

    fun setup(@Nullable attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomSeekbar)
        try {
            setIncrement(typedArray.getInt(R.styleable.CustomSeekbar_seekBarIncrement, 1))
            val min = typedArray.getInt(R.styleable.CustomSeekbar_android_min, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                super.setMin(0)
            }
            setRange(min, super.getMax())
        } finally {
            typedArray.recycle()
        }

        if (super.getProgress() == 0) {
            super.setProgress(super.getProgress() + 1)
            post {
                super.setProgress(super.getProgress() - 1)
                post { super.setOnSeekBarChangeListener(mInternalListener) }
            }
        } else {
            super.setOnSeekBarChangeListener(mInternalListener)
        }
    }

    private fun applyIncrement(progress: Int): Int {
        if (mIncrement < 1) return progress
        var p = progress / mIncrement
        p = p * mIncrement
        return p
    }
}
