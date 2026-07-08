package net.kdt.pojavlaunch.customcontrols.buttons

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import git.artdeell.dnbootstrap.glfw.GLFW
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.CallbackBridge
import net.kdt.pojavlaunch.CallbackBridge.sendMouseButton
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.MainActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.buttons.BackgroundTint.DEFAULT_TINT_LIST
import net.kdt.pojavlaunch.customcontrols.buttons.BackgroundTint.TOGGLE_TINT_LIST
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog
import net.kdt.pojavlaunch.prefs.LauncherPreferences

@SuppressLint("ViewConstructor", "AppCompatCustomView")
class ControlButton : TextView, ControlInterface {
    private val mRectPaint = Paint()
    protected var mProperties: ControlData
    private val mControlLayout: ControlLayout
    private var mComputedRadius = 0f
    private var mHasBitmap = false
    protected var mIsToggled = false

    constructor(layout: ControlLayout, properties: ControlData) : super(layout.context) {
        mControlLayout = layout
        mProperties = properties
        gravity = Gravity.CENTER
        setAllCaps(LauncherPreferences.PREF_BUTTON_ALL_CAPS)
        setTextColor(Color.WHITE)
        setPadding(4, 4, 4, 4)
        textSize = 14f
        outlineProvider = null

        setProperties(preProcessProperties(properties, layout))

        injectBehaviors()
    }

    override fun getControlView(): View = this

    override fun getProperties(): ControlData = mProperties

    private fun setupBitmapTint() {
        BackgroundTint.applyToggleTint(context)
        val tintStateList = if (mProperties.isToggle) TOGGLE_TINT_LIST else DEFAULT_TINT_LIST
        backgroundTintList = tintStateList
        backgroundTintMode = PorterDuff.Mode.SRC_ATOP
    }

    private fun setupNormalTint() {
        mComputedRadius = ControlInterface.super.computeCornerRadius(mProperties.cornerRadius)
        backgroundTintList = null
        if (mProperties.isToggle) {
            val value = TypedValue()
            context.theme.resolveAttribute(R.attr.colorAccent, value, true)
            mRectPaint.color = value.data
            mRectPaint.alpha = BackgroundTint.BACKGROUND_TOGGLE_TINT_ALPHA
        } else {
            mRectPaint.color = Color.WHITE
            mRectPaint.alpha = BackgroundTint.BACKGROUND_DEFAULT_TINT_ALPHA
        }
    }

    fun setProperties(properties: ControlData, changePos: Boolean) {
        mProperties = properties
        ControlInterface.super.setProperties(properties, changePos)

        mHasBitmap = Tools.isValidString(mProperties.bitmapTag)

        if (mHasBitmap) setupBitmapTint()
        else setupNormalTint()

        text = properties.name
    }

    @Deprecated("Deprecated in Java")
    override fun setProperties(properties: ControlData) {
        setProperties(properties, true)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mHasBitmap || !isActivated) return
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), mComputedRadius, mComputedRadius, mRectPaint)
    }

    override fun isActivated(): Boolean {
        return super.isActivated() || (mProperties.isToggle && mIsToggled)
    }

    fun loadEditValues(editControlPopup: EditControlSideDialog) {
        editControlPopup.loadValues(getProperties())
    }

    fun cloneButton() {
        val cloneData = ControlData(getProperties())
        cloneData.dynamicX = "0.5 * \${screen_width}"
        cloneData.dynamicY = "0.5 * \${screen_height}"
        (parent as ControlLayout).addControlButton(cloneData)
    }

    fun removeButton() {
        val parent = getControlLayoutParent() ?: return
        parent.layout?.mControlDataList?.remove(getProperties())
        parent.removeView(this)
    }

    override fun handlePressed() {
        if (!getProperties().isToggle) {
            sendKeyPresses(true)
        }
    }

    override fun handleReleased() {
        if (!triggerToggle()) {
            sendKeyPresses(false)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val properties = getProperties()
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
                if (properties.passThruEnabled) {
                    val gameSurface = getControlLayoutParent()?.getGameSurface()
                    gameSurface?.dispatchTouchEvent(event)
                }
            }
        }

        if (properties.isSwipeable) {
            getControlLayoutParent()?.onTouch(this, event)
            return true
        }

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> handlePressed()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> handleReleased()
            else -> return false
        }

        return super.onTouchEvent(event)
    }

    fun triggerToggle(): Boolean {
        if (mProperties.isToggle) {
            mIsToggled = !mIsToggled
            invalidate()
            sendKeyPresses(mIsToggled)
            return true
        }
        return false
    }

    fun sendKeyPresses(isDown: Boolean) {
        isActivated = isDown
        for (keycode in mProperties.keycodes) {
            if (keycode >= LwjglGlfwKeycode.GLFW_KEY_UNKNOWN) {
                CallbackBridge.setModifiers(keycode, isDown)
                val modifiers = CallbackBridge.getCurrentMods()
                GLFW.sendKeyEvent(keycode, isDown, modifiers)
            } else {
                Log.i("punjabilauncher", "sendSpecialKey($keycode,$isDown)")
                sendSpecialKey(keycode, isDown)
            }
        }
    }

    private fun sendSpecialKey(keycode: Int, isDown: Boolean) {
        when (keycode) {
            ControlData.SPECIALBTN_KEYBOARD -> if (isDown) MainActivity.switchKeyboardState(false)
            ControlData.SPECIALBTN_KEYBOARDPAN -> if (isDown) MainActivity.switchKeyboardState(true)
            ControlData.SPECIALBTN_TOGGLECTRL -> if (isDown) getControlLayoutParent()?.toggleControlVisible()
            ControlData.SPECIALBTN_VIRTUALMOUSE -> if (isDown) MainActivity.toggleMouse(context)
            ControlData.SPECIALBTN_MOUSEPRI -> sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, isDown)
            ControlData.SPECIALBTN_MOUSEMID -> sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE, isDown)
            ControlData.SPECIALBTN_MOUSESEC -> sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, isDown)
            ControlData.SPECIALBTN_SCROLLDOWN -> if (!isDown) CallbackBridge.sendScroll(0, 1.0)
            ControlData.SPECIALBTN_SCROLLUP -> if (!isDown) CallbackBridge.sendScroll(0, -1.0)
            ControlData.SPECIALBTN_MENU -> mControlLayout.notifyAppMenu()
        }
    }

    override fun hasOverlappingRendering(): Boolean = false
}
