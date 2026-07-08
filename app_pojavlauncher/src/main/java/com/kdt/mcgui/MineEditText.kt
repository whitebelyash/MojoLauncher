package com.kdt.mcgui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet

class MineEditText : androidx.appcompat.widget.AppCompatEditText {

    constructor(ctx: Context) : super(ctx) { init() }
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs) { init() }

    fun init() {
        setBackgroundColor(Color.parseColor("#131313"))
        setPadding(5, 5, 5, 5)
    }
}
