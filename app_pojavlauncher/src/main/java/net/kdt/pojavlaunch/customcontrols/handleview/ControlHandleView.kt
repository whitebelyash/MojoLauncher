package net.kdt.pojavlaunch.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.Nullable
import androidx.core.content.res.ResourcesCompat
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface

class ControlHandleView : View {
    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, @Nullable attrs: AttributeSet) : super(context, attrs) { init() }

    private val mDrawable: Drawable? = ResourcesCompat.getDrawable(resources, R.drawable.ic_view_handle, context.theme)
    private var mView: ControlInterface? = null
    private var mXOffset = 0f
    private var mYOffset = 0f

    private val mPositionListener = ViewTreeObserver.OnPreDrawListener {
        if (mView == null || !mView!!.getControlView().isShown) {
            hide()
            return@OnPreDrawListener true
        }
        x = mView!!.getControlView().x + mView!!.getControlView().width
        y = mView!!.getControlView().y + mView!!.getControlView().height
        true
    }

    private fun init() {
        val size = resources.getDimensionPixelOffset(R.dimen._22sdp)
        mDrawable?.setBounds(0, 0, size, size)
        layoutParams = ViewGroup.LayoutParams(size, size)
        background = mDrawable
        translationZ = 10.5f
    }

    fun setControlButton(controlInterface: ControlInterface) {
        if (mView != null) mView!!.getControlView().viewTreeObserver.removeOnPreDrawListener(mPositionListener)
        visibility = View.VISIBLE
        mView = controlInterface
        mView!!.getControlView().viewTreeObserver.addOnPreDrawListener(mPositionListener)
        x = controlInterface.getControlView().x + controlInterface.getControlView().width
        y = controlInterface.getControlView().y + controlInterface.getControlView().height
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mXOffset = event.x
                mYOffset = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                x = x + event.x - mXOffset
                y = y + event.y - mYOffset

                mView!!.getProperties().setWidth(x - mView!!.getControlView().x)
                mView!!.getProperties().setHeight(y - mView!!.getControlView().y)
                mView!!.regenerateDynamicCoordinates()
            }
        }
        return true
    }

    fun hide() {
        if (mView != null) mView!!.getControlView().viewTreeObserver.removeOnPreDrawListener(mPositionListener)
        visibility = View.GONE
    }
}
