package com.kdt.mcgui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import git.artdeell.mojo.R

class MineButton : androidx.appcompat.widget.AppCompatButton {

    constructor(ctx: Context) : this(ctx, null)

    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs) { init() }

    fun init() {
        typeface = ResourcesCompat.getFont(context, R.font.noto_sans_bold)
        background = ResourcesCompat.getDrawable(resources, R.drawable.mine_button_background, null)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen._13ssp).toFloat())
    }
}
