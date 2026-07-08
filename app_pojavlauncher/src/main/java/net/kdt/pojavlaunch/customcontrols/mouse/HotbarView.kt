package net.kdt.pojavlaunch.customcontrols.mouse

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.annotation.Nullable
import git.artdeell.dnbootstrap.glfw.GLFW
import git.artdeell.dnbootstrap.glfw.GrabListener
import net.kdt.pojavlaunch.CallbackBridge
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.MCOptionUtils
import net.kdt.pojavlaunch.utils.MathUtils

class HotbarView : View, MCOptionUtils.MCOptionListener, View.OnLayoutChangeListener, Runnable {
    companion object {
        private val HOTBAR_KEYS = intArrayOf(
            LwjglGlfwKeycode.GLFW_KEY_1, LwjglGlfwKeycode.GLFW_KEY_2, LwjglGlfwKeycode.GLFW_KEY_3,
            LwjglGlfwKeycode.GLFW_KEY_4, LwjglGlfwKeycode.GLFW_KEY_5, LwjglGlfwKeycode.GLFW_KEY_6,
            LwjglGlfwKeycode.GLFW_KEY_7, LwjglGlfwKeycode.GLFW_KEY_8, LwjglGlfwKeycode.GLFW_KEY_9
        )
    }

    private val mDoubleTapDetector = TapDetector(2, TapDetector.DETECTION_METHOD_DOWN)
    private var mParentView: View? = null
    private val mDropGesture = DropGesture(Handler(Looper.getMainLooper()))
    private val mGrabListener = GrabListener { isGrabbing ->
        mLastIndex = -1
        mDropGesture.cancel()
    }

    private var mWidth = 0
    private var mLastIndex = -1
    private var mGuiScale = 0

    constructor(context: Context) : super(context) { initialize() }
    constructor(context: Context, @Nullable attrs: AttributeSet) : super(context, attrs) { initialize() }
    constructor(context: Context, @Nullable attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) { initialize() }
    @Suppress("unused")
    constructor(context: Context, @Nullable attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes) { initialize() }

    private fun initialize() {
        MCOptionUtils.addMCOptionListener(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val parent = parent
        if (parent is View) {
            mParentView = parent
            mParentView!!.addOnLayoutChangeListener(this)
        }
        mGuiScale = MCOptionUtils.getMcScale()
        repositionView()
        GLFW.addGrabListener(mGrabListener)
    }

    private fun repositionView() {
        val layoutParams = layoutParams
        if (layoutParams !is ViewGroup.MarginLayoutParams)
            throw RuntimeException("Incorrect LayoutParams type, expected ViewGroup.MarginLayoutParams")
        val marginLayoutParams = layoutParams as ViewGroup.MarginLayoutParams
        val parent = parent as ViewGroup
        marginLayoutParams.width = mcScale(180).also { mWidth = it }
        val height = mcScale(20)
        marginLayoutParams.height = height
        marginLayoutParams.leftMargin = parent.width / 2 - mWidth / 2
        marginLayoutParams.topMargin = parent.height - height
        layoutParams = marginLayoutParams
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!GLFW.isGrabbing()) return false
        val hasDoubleTapped = mDoubleTapDetector.onTouchEvent(event)

        val actionMasked = event.actionMasked
        if (isLastEventInGesture(actionMasked)) mDropGesture.cancel()
        else mDropGesture.submit()

        val x = event.x
        if (x < 0 || x >= mWidth) {
            mDropGesture.cancel()
            return true
        }
        val hotbarIndex = MathUtils.map(x, 0f, mWidth.toFloat(), 0f, HOTBAR_KEYS.size.toFloat()).toInt()
        if (hotbarIndex == mLastIndex) {
            if (hasDoubleTapped && !LauncherPreferences.PREF_DISABLE_SWAP_HAND)
                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_F)
            return true
        }
        mLastIndex = hotbarIndex
        val hotbarKey = HOTBAR_KEYS[hotbarIndex]
        CallbackBridge.sendKeyPress(hotbarKey)
        mDropGesture.cancel()
        if (!isLastEventInGesture(actionMasked)) mDropGesture.submit()
        return true
    }

    private fun isLastEventInGesture(actionMasked: Int): Boolean {
        return actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL
    }

    private fun mcScale(input: Int): Int {
        return (mGuiScale * input / LauncherPreferences.PREF_SCALE_FACTOR).toInt()
    }

    fun onResolutionChanged() {
        if (parent == null) return
        mGuiScale = MCOptionUtils.getMcScale()
        post { repositionView() }
    }

    override fun onOptionChanged() {
        post(this)
    }

    override fun run() {
        if (parent == null) return
        val scale = MCOptionUtils.getMcScale()
        if (scale == mGuiScale) return
        mGuiScale = scale
        repositionView()
    }

    override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int,
                                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
        if (v == mParentView && (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom)) {
            post { repositionView() }
        }
    }
}
