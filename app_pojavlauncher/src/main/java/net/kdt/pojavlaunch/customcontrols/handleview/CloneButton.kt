package net.kdt.pojavlaunch.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import androidx.annotation.Nullable
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface

@SuppressLint("AppCompatCustomView")
class CloneButton : Button, ActionButtonInterface {
    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, @Nullable attrs: AttributeSet) : super(context, attrs) { init() }

    private var mCurrentlySelectedButton: ControlInterface? = null

    override fun init() {
        setOnClickListener(this)
        isAllCaps = true
        setText(R.string.global_clone)
    }

    override fun shouldBeVisible(): Boolean = mCurrentlySelectedButton != null

    override fun setFollowedView(view: ControlInterface?) {
        mCurrentlySelectedButton = view
    }

    override fun onClick() {
        if (mCurrentlySelectedButton == null) return
        mCurrentlySelectedButton!!.cloneButton()
        mCurrentlySelectedButton!!.getControlLayoutParent()?.removeEditWindow()
    }
}
