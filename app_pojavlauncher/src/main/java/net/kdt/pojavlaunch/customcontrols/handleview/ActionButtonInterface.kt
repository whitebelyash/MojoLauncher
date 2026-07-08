package net.kdt.pojavlaunch.customcontrols.handleview

import android.view.View
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface

interface ActionButtonInterface : View.OnClickListener {
    fun init()
    fun setFollowedView(view: ControlInterface?)
    fun onClick()
    fun shouldBeVisible(): Boolean

    override fun onClick(v: View) {
        onClick()
    }
}
