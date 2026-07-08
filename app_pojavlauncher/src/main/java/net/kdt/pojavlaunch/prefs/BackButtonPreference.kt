package net.kdt.pojavlaunch.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore

class BackButtonPreference : Preference {

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    @Suppress("unused")
    constructor(context: Context) : this(context, null)

    private fun init() {
        if (title == null) {
            setTitle(R.string.preference_back_title)
        }
        if (icon == null) {
            setIcon(R.drawable.ic_px_arrow_left)
        }
    }

    override fun onClick() {
        ExtraCore.setValue(ExtraConstants.BACK_PREFERENCE, "true")
    }
}
