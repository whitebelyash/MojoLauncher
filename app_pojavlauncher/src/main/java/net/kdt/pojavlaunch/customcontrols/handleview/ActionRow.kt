package net.kdt.pojavlaunch.customcontrols.handleview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.annotation.Nullable
import androidx.core.math.MathUtils
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface

class ActionRow : LinearLayout {
    companion object {
        const val SIDE_LEFT = 0x0
        const val SIDE_TOP = 0x1
        const val SIDE_RIGHT = 0x2
        const val SIDE_BOTTOM = 0x3
        const val SIDE_AUTO = 0x4
    }

    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, @Nullable attrs: AttributeSet) : super(context, attrs) { init() }

    val mFollowedViewListener = ViewTreeObserver.OnPreDrawListener {
        if (mFollowedView == null || !mFollowedView!!.isShown) {
            hide()
            return@OnPreDrawListener true
        }
        setNewPosition()
        true
    }

    private val actionButtons = arrayOfNulls<ActionButtonInterface>(3)
    private var mFollowedView: View? = null
    private val mSide = SIDE_AUTO

    private fun init() {
        translationZ = 11f
        visibility = GONE
        orientation = HORIZONTAL
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            resources.getDimensionPixelOffset(R.dimen._40sdp)
        )

        actionButtons[0] = DeleteButton(context)
        actionButtons[1] = CloneButton(context)
        actionButtons[2] = AddSubButton(context)

        for (buttonInterface in actionButtons) {
            val button = buttonInterface as View
            addView(button, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }

        elevation = 5f
    }

    fun setFollowedButton(controlInterface: ControlInterface?) {
        if (mFollowedView != null)
            mFollowedView!!.viewTreeObserver.removeOnPreDrawListener(mFollowedViewListener)

        for (buttonInterface in actionButtons) {
            buttonInterface!!.setFollowedView(controlInterface)
            (buttonInterface as View).visibility = if (buttonInterface.shouldBeVisible()) VISIBLE else GONE
        }

        visibility = VISIBLE
        mFollowedView = controlInterface as? View
        if (mFollowedView != null)
            mFollowedView!!.viewTreeObserver.addOnPreDrawListener(mFollowedViewListener)
    }

    private fun getXPosition(side: Int): Float {
        return when (side) {
            SIDE_LEFT -> mFollowedView!!.x - width
            SIDE_RIGHT -> mFollowedView!!.x + mFollowedView!!.width
            else -> mFollowedView!!.x + mFollowedView!!.width / 2f - width / 2f
        }
    }

    private fun getYPosition(side: Int): Float {
        return when (side) {
            SIDE_TOP -> mFollowedView!!.y - height
            SIDE_BOTTOM -> mFollowedView!!.y + mFollowedView!!.height
            else -> mFollowedView!!.y + mFollowedView!!.height / 2f - height / 2f
        }
    }

    private fun setNewPosition() {
        if (mFollowedView == null) return
        val side = pickSide()
        x = MathUtils.clamp(getXPosition(side), 0f, ((parent as ViewGroup).width - width).toFloat())
        y = getYPosition(side)
    }

    private fun pickSide(): Int {
        if (mFollowedView == null) return mSide
        if (mSide != SIDE_AUTO) return mSide
        val parent = mFollowedView!!.parent as? ViewGroup ?: return mSide
        val futurePos = getYPosition(SIDE_TOP)
        return if (futurePos + height > parent.height + height / 2f) SIDE_TOP
        else if (futurePos < -height / 2f) SIDE_BOTTOM
        else SIDE_TOP
    }

    fun hide() {
        if (mFollowedView != null)
            mFollowedView!!.viewTreeObserver.removeOnPreDrawListener(mFollowedViewListener)
        visibility = GONE
    }
}
