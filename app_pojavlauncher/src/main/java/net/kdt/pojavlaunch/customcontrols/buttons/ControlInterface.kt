package net.kdt.pojavlaunch.customcontrols.buttons

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.annotation.CallSuper
import androidx.core.math.MathUtils
import git.artdeell.dnbootstrap.glfw.GLFW
import git.artdeell.dnbootstrap.glfw.GrabListener
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.LayoutBitmaps
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_BUTTONSIZE

interface ControlInterface : View.OnLongClickListener, GrabListener {
    fun getControlView(): View
    fun getProperties(): ControlData

    fun setProperties(properties: ControlData) {
        setProperties(properties, true)
    }

    fun removeButton()
    fun cloneButton()

    fun setVisible(isVisible: Boolean) {
        if (getProperties().isHideable)
            getControlView().visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    fun handlePressed()
    fun handleReleased()
    fun loadEditValues(editControlDialog: EditControlSideDialog)

    override fun onGrabState(isGrabbing: Boolean) {
        if (getControlLayoutParent() == null || getControlLayoutParent()!!.getModifiable()) return
        setVisible(
            (getProperties().displayInGame && isGrabbing || getProperties().displayInMenu && !isGrabbing)
                    && getControlLayoutParent()!!.areControlVisible()
        )
    }

    fun getControlLayoutParent(): ControlLayout? {
        return getControlView().parent as? ControlLayout
    }

    fun preProcessProperties(properties: ControlData, layout: ControlLayout): ControlData {
        properties.setWidth(Tools.pxToDp(properties.getWidth()) / layout.getLayoutScale() * PREF_BUTTONSIZE)
        properties.setHeight(Tools.pxToDp(properties.getHeight()) / layout.getLayoutScale() * PREF_BUTTONSIZE)
        properties.isHideable = !properties.containsKeycode(ControlData.SPECIALBTN_TOGGLECTRL) &&
                !properties.containsKeycode(ControlData.SPECIALBTN_VIRTUALMOUSE)
        return properties
    }

    fun updateProperties() {
        setProperties(getProperties())
    }

    @CallSuper
    fun setProperties(properties: ControlData, changePos: Boolean) {
        if (changePos && !getControlView().isInLayout) {
            getControlView().requestLayout()
        }
    }

    fun setBackground() {
        val controlView = getControlView()
        var drawable = controlView.background
        val bitmapTag = getProperties().bitmapTag
        if (Tools.isValidString(bitmapTag)) {
            val storage = getControlLayoutParent()!!.getBitmaps()!!
            val bgBitmap = storage.getBitmap(getProperties().bitmapTag!!)
            if (drawable is BitmapDrawable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                drawable.bitmap = bgBitmap
            } else {
                drawable = BitmapDrawable(controlView.resources, bgBitmap)
            }
        } else {
            val gd = if (drawable is GradientDrawable) drawable else GradientDrawable()
            gd.setColor(getProperties().bgColor)
            gd.setStroke(
                Tools.dpToPx(getProperties().strokeWidth * (getControlLayoutParent()!!.getLayoutScale() / 100f)).toInt(),
                getProperties().strokeColor
            )
            gd.cornerRadius = computeCornerRadius(getProperties().cornerRadius)
            drawable = gd
        }
        controlView.background = drawable
    }

    fun setDynamicX(dynamicX: String) {
        getProperties().dynamicX = dynamicX
    }

    fun setDynamicY(dynamicY: String) {
        getProperties().dynamicY = dynamicY
    }

    fun generateDynamicX(x: Float): String {
        val width = getControlLayoutParent()!!.width
        return if (x + (getProperties().getWidth() / 2f) > width / 2f) {
            "${(x + getProperties().getWidth()) / width} * \${screen_width} - \${width}"
        } else {
            "${x / width} * \${screen_width}"
        }
    }

    fun generateDynamicY(y: Float): String {
        val height = getControlLayoutParent()!!.height
        return if (y + (getProperties().getHeight() / 2f) > height / 2f) {
            "${(y + getProperties().getHeight()) / height} * \${screen_height} - \${height}"
        } else {
            "${y / height} * \${screen_height}"
        }
    }

    fun regenerateDynamicCoordinates() {
        getProperties().dynamicX = generateDynamicX(getControlView().x)
        getProperties().dynamicY = generateDynamicY(getControlView().y)
        updateProperties()
    }

    fun applySize(equation: String, button: ControlInterface): String {
        return equation
            .replace("\${right}", "(\${screen_width} - \${width})")
            .replace("\${bottom}", "(\${screen_height} - \${height})")
            .replace("\${height}", "(px(${Tools.pxToDp(button.getProperties().getHeight())}) /$PREF_BUTTONSIZE * \${preferred_scale})")
            .replace("\${width}", "(px(${Tools.pxToDp(button.getProperties().getWidth())}) / $PREF_BUTTONSIZE * \${preferred_scale})")
    }

    fun computeCornerRadius(radiusInPercent: Float): Float {
        val minSize = minOf(getProperties().getWidth(), getProperties().getHeight())
        return (minSize / 2) * (radiusInPercent / 100)
    }

    fun canSnap(button: ControlInterface): Boolean {
        val MIN_DISTANCE = getSnapDistance()
        if (button === this) return false
        return !(net.kdt.pojavlaunch.utils.MathUtils.dist(
            button.getControlView().x + button.getControlView().width / 2f,
            button.getControlView().y + button.getControlView().height / 2f,
            getControlView().x + getControlView().width / 2f,
            getControlView().y + getControlView().height / 2f
        ) > maxOf(
            button.getControlView().width / 2f + getControlView().width / 2f,
            button.getControlView().height / 2f + getControlView().height / 2f
        ) + MIN_DISTANCE)
    }

    fun snapAndAlign(x: Float, y: Float) {
        val MIN_DISTANCE = getSnapDistance()
        var dynamicX = generateDynamicX(x)
        var dynamicY = generateDynamicY(y)

        getControlView().x = x
        getControlView().y = y

        for (button in (getControlView().parent as ControlLayout).getButtonChildren()) {
            if (!canSnap(button)) continue

            val buttonTop = button.getControlView().y
            val buttonBottom = buttonTop + button.getControlView().height
            val buttonLeft = button.getControlView().x
            val buttonRight = buttonLeft + button.getControlView().width

            val top = getControlView().y
            val bottom = getControlView().y + getControlView().height
            val left = getControlView().x
            val right = getControlView().x + getControlView().width

            if (kotlin.math.abs(top - buttonBottom) < MIN_DISTANCE) {
                dynamicY = applySize(button.getProperties().dynamicY, button) + applySize(" + \${height}", button) + " + \${margin}"
            } else if (kotlin.math.abs(buttonTop - bottom) < MIN_DISTANCE) {
                dynamicY = applySize(button.getProperties().dynamicY, button) + " - \${height} - \${margin}"
            }
            if (dynamicY != generateDynamicY(getControlView().y)) {
                if (kotlin.math.abs(buttonLeft - left) < MIN_DISTANCE) {
                    dynamicX = applySize(button.getProperties().dynamicX, button)
                } else if (kotlin.math.abs(buttonRight - right) < MIN_DISTANCE) {
                    dynamicX = applySize(button.getProperties().dynamicX, button) + applySize(" + \${width}", button) + " - \${width}"
                }
            }

            if (kotlin.math.abs(buttonLeft - right) < MIN_DISTANCE) {
                dynamicX = applySize(button.getProperties().dynamicX, button) + " - \${width} - \${margin}"
            } else if (kotlin.math.abs(left - buttonRight) < MIN_DISTANCE) {
                dynamicX = applySize(button.getProperties().dynamicX, button) + applySize(" + \${width}", button) + " + \${margin}"
            }
            if (dynamicX != generateDynamicX(getControlView().x)) {
                if (kotlin.math.abs(buttonTop - top) < MIN_DISTANCE) {
                    dynamicY = applySize(button.getProperties().dynamicY, button)
                } else if (kotlin.math.abs(buttonBottom - bottom) < MIN_DISTANCE) {
                    dynamicY = applySize(button.getProperties().dynamicY, button) + applySize(" + \${height}", button) + " - \${height}"
                }
            }
        }

        setDynamicX(dynamicX)
        setDynamicY(dynamicY)
    }

    fun injectBehaviors() {
        injectProperties()
        injectTouchEventBehavior()
        injectLayoutParamBehavior()
        injectGrabListenerBehavior()
    }

    fun injectGrabListenerBehavior() {
        if (getControlView() == null) {
            Log.e(ControlInterface::class.java.toString(), "Failed to inject grab listener behavior !")
            return
        }

        getControlView().addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                GLFW.addGrabListener(this@ControlInterface)
                getControlView().removeOnAttachStateChangeListener(this)
            }

            override fun onViewDetachedFromWindow(v: View) {}
        })
    }

    fun injectProperties() {
        getControlView().post { getControlView().translationZ = 10f }
    }

    fun injectTouchEventBehavior() {
        getControlView().setOnTouchListener(object : View.OnTouchListener {
            private var mCanTriggerLongClick = true
            private var downX = 0f
            private var downY = 0f
            private var downRawX = 0f
            private var downRawY = 0f

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                if (getControlLayoutParent()?.getModifiable() != true) {
                    view.onTouchEvent(event)
                    return true
                }

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        mCanTriggerLongClick = true
                        downRawX = event.rawX
                        downRawY = event.rawY
                        downX = downRawX - view.x
                        downY = downRawY - view.y
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (kotlin.math.abs(event.rawX - downRawX) > 8 ||
                            kotlin.math.abs(event.rawY - downRawY) > 8
                        ) mCanTriggerLongClick = false
                        getControlLayoutParent()?.adaptPanelPosition()
                        snapAndAlign(
                            MathUtils.clamp(
                                event.rawX - downX, 0f,
                                getControlLayoutParent()!!.width - view.width.toFloat()
                            ),
                            MathUtils.clamp(
                                event.rawY - downY, 0f,
                                getControlLayoutParent()!!.width - view.height.toFloat()
                            )
                        )
                    }

                    MotionEvent.ACTION_UP -> {
                        if (mCanTriggerLongClick) onLongClick(view)
                        view.translationX = 0f
                        view.translationY = 0f
                        view.requestLayout()
                    }
                }
                return true
            }
        })
    }

    fun injectLayoutParamBehavior() {
        getControlView().addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> setBackground() }
    }

    override fun onLongClick(v: View): Boolean {
        if (getControlLayoutParent()?.getModifiable() == true) {
            getControlLayoutParent()?.editControlButton(this)
            getControlLayoutParent()?.mActionRow?.setFollowedButton(this)
        }
        return true
    }

    companion object {
        fun getSnapDistance(): Float = Tools.dpToPx(6f)
        fun getMarginDistance(): Float = Tools.dpToPx(2f)
    }
}
