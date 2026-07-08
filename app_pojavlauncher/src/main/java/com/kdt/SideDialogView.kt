package com.kdt

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView

import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat

import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools

abstract class SideDialogView(context: Context, parent: ViewGroup, @LayoutRes private val mLayoutId: Int) {
    protected val mMargin: Int = context.resources.getDimensionPixelOffset(R.dimen._20sdp)
    private val mParent: ViewGroup = parent
    private var mDialogLayout: ViewGroup? = null
    private var mScrollView: DefocusableScrollView? = null
    protected var mDialogContent: View? = null

    private var mSideDialogAnimator: ObjectAnimator? = null
    protected var mDisplaying = false

    private var mStartButton: Button? = null
    private var mEndButton: Button? = null
    private var mTitleTextview: TextView? = null
    private var mTitleDivider: View? = null

    private var mStartButtonStringId = 0
    private var mEndButtonStringId = 0
    private var mTitleStringId = 0
    private var mStartButtonListener: View.OnClickListener? = null
    private var mEndButtonListener: View.OnClickListener? = null

    fun setTitle(@StringRes textId: Int) {
        mTitleStringId = textId
        if (mDialogLayout != null) {
            mTitleTextview!!.setText(textId)
            mTitleTextview!!.visibility = View.VISIBLE
            mTitleDivider!!.visibility = View.VISIBLE
        }
    }

    fun setStartButtonListener(@StringRes textId: Int, @Nullable listener: View.OnClickListener?) {
        mStartButtonStringId = textId
        mStartButtonListener = listener
        if (mDialogLayout != null) setButton(mStartButton!!, textId, listener)
    }

    fun setEndButtonListener(@StringRes textId: Int, @Nullable listener: View.OnClickListener?) {
        mEndButtonStringId = textId
        mEndButtonListener = listener
        if (mDialogLayout != null) setButton(mEndButton!!, textId, listener)
    }

    private fun setButton(@NonNull button: Button, @StringRes textId: Int, @Nullable listener: View.OnClickListener?) {
        button.setText(textId)
        button.setOnClickListener(listener)
        button.visibility = View.VISIBLE
    }

    private fun inflateLayout() {
        if (mDialogLayout != null) {
            Log.w("SideDialogView", "Layout already inflated")
            return
        }

        mDialogLayout = LayoutInflater.from(mParent.context).inflate(R.layout.dialog_side_dialog, mParent, false) as ViewGroup
        mScrollView = mDialogLayout!!.findViewById(R.id.side_dialog_scrollview)
        mStartButton = mDialogLayout!!.findViewById(R.id.side_dialog_start_button)
        mEndButton = mDialogLayout!!.findViewById(R.id.side_dialog_end_button)
        mTitleTextview = mDialogLayout!!.findViewById(R.id.side_dialog_title_textview)
        mTitleDivider = mDialogLayout!!.findViewById(R.id.side_dialog_title_divider)

        LayoutInflater.from(mParent.context).inflate(mLayoutId, mScrollView, true)
        mDialogContent = mScrollView!!.getChildAt(0)

        mParent.addView(mDialogLayout)

        mSideDialogAnimator = ObjectAnimator.ofFloat(mDialogLayout, "x", 0f).setDuration(600)
        mSideDialogAnimator!!.interpolator = AccelerateDecelerateInterpolator()

        mDialogLayout!!.elevation = 10f
        mDialogLayout!!.translationZ = 10f

        mDialogLayout!!.visibility = View.VISIBLE
        mDialogLayout!!.background = ResourcesCompat.getDrawable(mDialogLayout!!.resources, R.drawable.background_control_editor, null)

        mDialogLayout!!.x = -mDialogLayout!!.resources.getDimensionPixelOffset(R.dimen._280sdp).toFloat()

        if (mTitleStringId != 0) setTitle(mTitleStringId)
        if (mStartButtonStringId != 0) setStartButtonListener(mStartButtonStringId, mStartButtonListener)
        if (mEndButtonStringId != 0) setEndButtonListener(mEndButtonStringId, mEndButtonListener)
    }

    private fun deflateLayout() {
        if (mDialogLayout == null) {
            Log.w("SideDialogView", "Layout not inflated")
            return
        }

        mSideDialogAnimator!!.removeAllUpdateListeners()
        mSideDialogAnimator!!.removeAllListeners()

        mParent.removeView(mDialogLayout)

        mDialogLayout = null
        mScrollView = null
        mSideDialogAnimator = null
        mDialogContent = null
        mTitleTextview = null
        mTitleDivider = null
        mStartButton = null
        mEndButton = null
    }

    @CallSuper
    fun appear(fromRight: Boolean) {
        if (mDialogLayout == null) {
            inflateLayout()
            onInflate()
        }

        onAppear()
        val parent = getParent()
        mScrollView!!.post {
            if (mDialogLayout == null) return@post
            if (mSideDialogAnimator == null) throw RuntimeException("Unexpected side animator state when dialog is inflated")
            if (fromRight) {
                if (!mDisplaying || !isAtRight()) {
                    mSideDialogAnimator!!.setFloatValues(parent.width.toFloat(), (parent.width - mScrollView!!.width - mMargin).toFloat())
                    mSideDialogAnimator!!.start()
                    mDisplaying = true
                }
            } else {
                if (!mDisplaying || isAtRight()) {
                    mSideDialogAnimator!!.setFloatValues(-mDialogLayout!!.width.toFloat(), mMargin.toFloat())
                    mSideDialogAnimator!!.start()
                    mDisplaying = true
                }
            }
        }
    }

    protected fun isAtRight(): Boolean {
        if (mDialogLayout == null) throw RuntimeException("attempted to check dialog position when deflated")
        return mDialogLayout!!.x > getParent().width / 2f
    }

    @CallSuper
    fun disappear(destroy: Boolean) {
        if (mDialogLayout == null) {
            Log.w("SideDialogView", "Layout not inflated")
            return
        }

        if (!mDisplaying) {
            if (destroy) {
                onDisappear()
                onDestroy()
                deflateLayout()
            }
            return
        }

        mDisplaying = false
        if (isAtRight())
            mSideDialogAnimator!!.setFloatValues((getParent().width - mDialogLayout!!.width - mMargin).toFloat(), getParent().width.toFloat())
        else
            mSideDialogAnimator!!.setFloatValues(mMargin.toFloat(), -mDialogLayout!!.width.toFloat())

        if (destroy) {
            onDisappear()
            onDestroy()
            mSideDialogAnimator!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    deflateLayout()
                }
            })
        }

        mSideDialogAnimator!!.start()
    }

    private fun getParent(): ViewGroup {
        return mDialogLayout!!.parent as ViewGroup
    }

    fun isDisplaying(): Boolean {
        return mDisplaying
    }

    protected open fun onInflate() {}
    protected open fun onAppear() {}
    protected open fun onDisappear() {}
    protected open fun onDestroy() {}
}
