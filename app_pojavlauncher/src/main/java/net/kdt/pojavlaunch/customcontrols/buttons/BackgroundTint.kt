package net.kdt.pojavlaunch.customcontrols.buttons

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import androidx.core.graphics.ColorUtils

object BackgroundTint {
    const val BACKGROUND_DEFAULT_TINT_ALPHA = 60
    const val BACKGROUND_TOGGLE_TINT_ALPHA = 128

    private var lastTheme = System.identityHashCode(BackgroundTint::class.java)

    private val sState = arrayOf(
        intArrayOf(android.R.attr.state_activated)
    )
    private val sDefaultTint = intArrayOf(
        ColorUtils.setAlphaComponent(Color.WHITE, BACKGROUND_DEFAULT_TINT_ALPHA)
    )
    private val sToggleableTint = intArrayOf(
        ColorUtils.setAlphaComponent(Color.WHITE, BACKGROUND_TOGGLE_TINT_ALPHA)
    )

    val DEFAULT_TINT_LIST = ColorStateList(sState, sDefaultTint)
    val TOGGLE_TINT_LIST = ColorStateList(sState, sToggleableTint)

    fun applyToggleTint(context: Context) {
        val theme = context.theme
        val themeHash = theme.hashCode()
        if (themeHash == lastTheme) return
        val value = TypedValue()
        theme.resolveAttribute(android.R.attr.colorAccent, value, true)
        sToggleableTint[0] = ColorUtils.setAlphaComponent(value.data, BACKGROUND_TOGGLE_TINT_ALPHA)
        lastTheme = themeHash
    }
}
