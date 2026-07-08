package com.kdt

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.ScrollView

class DefocusableScrollView : ScrollView {
    private var mKeepFocusing = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    fun setKeepFocusing(shouldKeepFocusing: Boolean) {
        mKeepFocusing = shouldKeepFocusing
    }

    fun isKeepFocusing(): Boolean {
        return mKeepFocusing
    }

    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect): Int {
        if (!mKeepFocusing) return 0
        return super.computeScrollDeltaToGetChildRectOnScreen(rect)
    }
}
