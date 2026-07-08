package net.kdt.pojavlaunch.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import androidx.annotation.Nullable
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface

@SuppressLint("AppCompatCustomView")
class AddSubButton : Button, ActionButtonInterface {
    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, @Nullable attrs: AttributeSet) : super(context, attrs) { init() }

    private var mCurrentlySelectedButton: ControlInterface? = null

    override fun init() {
        setText(R.string.customctrl_addsubbutton)
        setOnClickListener(this)
    }

    override fun shouldBeVisible(): Boolean {
        return mCurrentlySelectedButton != null && mCurrentlySelectedButton is ControlDrawer
    }

    override fun setFollowedView(view: ControlInterface?) {
        mCurrentlySelectedButton = view
    }

    override fun onClick() {
        if (mCurrentlySelectedButton is ControlDrawer) {
            (mCurrentlySelectedButton as ControlDrawer).getControlLayoutParent()?.addSubButton(
                mCurrentlySelectedButton as ControlDrawer,
                ControlData()
            )
        }
    }
}
