package com.kdt.mcgui

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import git.artdeell.mojo.R

import fr.spse.extended_view.ExtendedButton

class LauncherMenuButton : ExtendedButton {

    constructor(@NonNull context: Context) : super(context) { setSettings() }
    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) { setSettings() }

    private fun setSettings() {
        val resources = context.resources

        val padding = resources.getDimensionPixelSize(R.dimen._22sdp)
        compoundDrawablePadding = padding
        setPaddingRelative(padding, 0, 0, 0)
        gravity = Gravity.CENTER_VERTICAL

        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen._12ssp).toFloat())

        val sizes = extendedViewData.sizeCompounds
        sizes[0] = resources.getDimensionPixelSize(R.dimen._30sdp)
        extendedViewData.sizeCompounds = sizes
        postProcessDrawables()
    }
}
