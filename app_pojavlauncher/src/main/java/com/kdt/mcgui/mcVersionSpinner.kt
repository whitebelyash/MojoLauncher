package com.kdt.mcgui

import android.view.ViewGroup.LayoutParams
import android.widget.AdapterView

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.transition.Slide
import android.transition.Transition
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ListView
import android.widget.PopupWindow

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity

import git.artdeell.mojo.R

import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.fragments.InstanceEditorFragment
import net.kdt.pojavlaunch.fragments.ProfileTypeSelectFragment
import net.kdt.pojavlaunch.instances.DisplayInstance
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.instances.InstanceAdapter
import net.kdt.pojavlaunch.instances.InstanceAdapterExtra

import java.io.IOException

import fr.spse.extended_view.ExtendedTextView

class mcVersionSpinner : ExtendedTextView {
    companion object {
        private const val VERSION_SPINNER_PROFILE_CREATE = 0
    }

    constructor(@NonNull context: Context) : super(context) { init() }
    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }

    private var mListView: ListView? = null
    private var mPopupWindow: PopupWindow? = null
    private var mPopupAnimation: Any? = null
    private var mSelectedIndex: Int = 0

    private val mProfileAdapter = InstanceAdapter(arrayOf(
        InstanceAdapterExtra(VERSION_SPINNER_PROFILE_CREATE,
            R.string.create_instance,
            ResourcesCompat.getDrawable(resources, R.drawable.ic_add, null)!!)
    ))

    fun setProfileSelection(position: Int) {
        setSelection(position)
        Instances.setSelectedInstance(mProfileAdapter.getItem(position) as DisplayInstance)
    }

    fun setSelection(position: Int) {
        mListView?.setSelection(position)
        mProfileAdapter.setView(this, position, false)
        mSelectedIndex = position
        mProfileAdapter.applySelectionIndex(mSelectedIndex)
    }

    fun openProfileEditor(fragmentActivity: FragmentActivity) {
        val currentSelection = mProfileAdapter.getItem(mSelectedIndex)
        if (currentSelection is InstanceAdapterExtra) {
            performExtraAction(currentSelection as InstanceAdapterExtra)
        } else {
            Tools.swapFragment(fragmentActivity, InstanceEditorFragment::class.java, InstanceEditorFragment.TAG, null)
        }
    }

    private fun applyInstances(instances: Instances) {
        mProfileAdapter.applyInstances(instances)
        setSelection(instances.selectedIndex)
    }

    fun reloadProfiles() {
        PojavApplication.sExecutorService.execute {
            try {
                val instances = Instances.loadDisplay()
                Tools.runOnUiThread { applyInstances(instances) }
            } catch (e: IOException) {
                Tools.runOnUiThread { Tools.showError(context, e) }
            }
        }
    }

    private fun init() {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen._12ssp).toFloat())
        gravity = Gravity.CENTER_VERTICAL
        val startPadding = context.resources.getDimensionPixelOffset(R.dimen._17sdp)
        val endPadding = context.resources.getDimensionPixelOffset(R.dimen._5sdp)
        setPaddingRelative(startPadding, 0, endPadding, 0)
        compoundDrawablePadding = startPadding
        addOnAttachStateChangeListener(ExtraAttachListener())
        setSelection(0)

        setOnClickListener {
            val offset = -context.resources.getDimensionPixelOffset(R.dimen._4sdp)
            if (mPopupWindow == null) getPopupWindow()
            if (mPopupWindow!!.isShowing) {
                mPopupWindow!!.dismiss()
                return@setOnClickListener
            }
            mPopupWindow!!.showAsDropDown(this@mcVersionSpinner, 0, offset)
            post { mListView!!.setSelection(mSelectedIndex) }
        }
    }

    private fun performExtraAction(extra: InstanceAdapterExtra) {
        if (extra.id == VERSION_SPINNER_PROFILE_CREATE) {
            Tools.swapFragment(context as FragmentActivity, ProfileTypeSelectFragment::class.java,
                ProfileTypeSelectFragment.TAG, null)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun getPopupWindow() {
        mListView = inflate(context, R.layout.spinner_mc_version, null) as ListView
        mListView!!.adapter = mProfileAdapter
        mListView!!.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val item = mProfileAdapter.getItem(position)
            if (item is DisplayInstance) {
                hidePopup(true)
                setProfileSelection(position)
            } else if (item is InstanceAdapterExtra) {
                hidePopup(false)
                performExtraAction(item as InstanceAdapterExtra)
            }
        }

        mPopupWindow = PopupWindow(mListView, LayoutParams.MATCH_PARENT, context.resources.getDimensionPixelOffset(R.dimen._184sdp))
        mPopupWindow!!.elevation = 5f
        mPopupWindow!!.isClippingEnabled = false
        mPopupWindow!!.isOutsideTouchable = true
        mPopupWindow!!.isFocusable = true
        mPopupWindow!!.setTouchInterceptor { _: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                mPopupWindow!!.dismiss()
                return@setTouchInterceptor true
            }
            false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPopupAnimation = Slide(Gravity.BOTTOM)
            mPopupWindow!!.enterTransition = mPopupAnimation as Transition
            mPopupWindow!!.exitTransition = mPopupAnimation as Transition
        }
    }

    private fun hidePopup(animate: Boolean) {
        if (mPopupWindow == null) return
        if (!animate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPopupWindow!!.enterTransition = null
            mPopupWindow!!.exitTransition = null
            mPopupWindow!!.dismiss()
            mPopupWindow!!.enterTransition = mPopupAnimation as Transition
            mPopupWindow!!.exitTransition = mPopupAnimation as Transition
        } else {
            mPopupWindow!!.dismiss()
        }
    }

    inner class ExtraAttachListener : OnAttachStateChangeListener, ExtraListener<Void> {
        override fun onViewAttachedToWindow(@NonNull view: View) {
            reloadProfiles()
            ExtraCore.addExtraListener(ExtraConstants.REFRESH_VERSION_SPINNER, this)
        }

        override fun onViewDetachedFromWindow(@NonNull view: View) {
            ExtraCore.removeExtraListenerFromValue(ExtraConstants.REFRESH_VERSION_SPINNER, this)
        }

        override fun onValueSet(key: String, value: Void): Boolean {
            post { this@mcVersionSpinner.reloadProfiles() }
            ExtraCore.consumeValue(key)
            return false
        }
    }
}
