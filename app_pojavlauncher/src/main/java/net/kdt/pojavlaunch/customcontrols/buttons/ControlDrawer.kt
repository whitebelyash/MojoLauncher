package net.kdt.pojavlaunch.customcontrols.buttons

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog
import java.util.ArrayList

@SuppressLint("ViewConstructor")
class ControlDrawer : ControlButton {
    val buttons: ArrayList<ControlSubButton>
    val drawerData: ControlDrawerData
    val parentLayout: ControlLayout
    var areButtonsVisible: Boolean

    constructor(layout: ControlLayout, drawerData: ControlDrawerData) : super(layout, drawerData.properties) {
        buttons = ArrayList(drawerData.buttonProperties.size)
        this.parentLayout = layout
        this.drawerData = drawerData
        areButtonsVisible = layout.getModifiable()
    }

    fun addButton(properties: ControlData) {
        addButton(ControlSubButton(parentLayout, properties, this))
    }

    fun addButton(button: ControlSubButton) {
        buttons.add(button)
        syncButtons()
        setControlButtonVisibility(button, areButtonsVisible)
    }

    private fun setControlButtonVisibility(button: ControlButton, isVisible: Boolean) {
        button.controlView.visibility = if (isVisible) VISIBLE else GONE
    }

    private fun switchButtonVisibility() {
        areButtonsVisible = !areButtonsVisible
        val visibility = if (areButtonsVisible) VISIBLE else GONE
        for (button in buttons) {
            button.controlView.visibility = visibility
        }
    }

    private fun alignButtons() {
        if (drawerData.orientation == ControlDrawerData.Orientation.FREE) return
        val margin = ControlInterface.getMarginDistance().toInt()

        for (i in buttons.indices) {
            when (drawerData.orientation) {
                ControlDrawerData.Orientation.RIGHT -> {
                    buttons[i].setDynamicX(generateDynamicX(x + (drawerData.properties.getWidth() + margin) * (i + 1)))
                    buttons[i].setDynamicY(generateDynamicY(y))
                }
                ControlDrawerData.Orientation.LEFT -> {
                    buttons[i].setDynamicX(generateDynamicX(x - (drawerData.properties.getWidth() + margin) * (i + 1)))
                    buttons[i].setDynamicY(generateDynamicY(y))
                }
                ControlDrawerData.Orientation.UP -> {
                    buttons[i].setDynamicY(generateDynamicY(y - (drawerData.properties.getHeight() + margin) * (i + 1)))
                    buttons[i].setDynamicX(generateDynamicX(x))
                }
                ControlDrawerData.Orientation.DOWN -> {
                    buttons[i].setDynamicY(generateDynamicY(y + (drawerData.properties.getHeight() + margin) * (i + 1)))
                    buttons[i].setDynamicX(generateDynamicX(x))
                }
                else -> {}
            }
            buttons[i].updateProperties()
        }
    }

    private fun resizeButtons() {
        if (drawerData.orientation == ControlDrawerData.Orientation.FREE) return
        for (subButton in buttons) {
            subButton.mProperties.setWidth(mProperties.getWidth())
            subButton.mProperties.setHeight(mProperties.getHeight())
            subButton.updateProperties()
        }
    }

    fun syncButtons() {
        alignButtons()
        resizeButtons()
    }

    fun containsChild(button: ControlInterface): Boolean {
        for (childButton in buttons) {
            if (childButton === button) return true
        }
        return false
    }

    override fun preProcessProperties(properties: ControlData, layout: ControlLayout): ControlData {
        val data = super.preProcessProperties(properties, layout)
        data.isHideable = true
        return data
    }

    override fun setVisible(isVisible: Boolean) {
        val visibility = if (isVisible) VISIBLE else GONE
        this.visibility = visibility
        if (visibility == GONE || areButtonsVisible) {
            for (button in buttons) {
                button.controlView.visibility = if (isVisible) VISIBLE
                else if (!mProperties.isHideable && visibility == GONE) VISIBLE else View.GONE
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!getControlLayoutParent()?.getModifiable()!!) {
            when (event.actionMasked) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> switchButtonVisibility()
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun setX(x: Float) {
        super.setX(x)
        alignButtons()
    }

    override fun setY(y: Float) {
        super.setY(y)
        alignButtons()
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        super.setLayoutParams(params)
        syncButtons()
    }

    override fun canSnap(button: ControlInterface): Boolean {
        val result = super.canSnap(button)
        return result && !containsChild(button)
    }

    fun getDrawerData(): ControlDrawerData = drawerData

    override fun loadEditValues(editControlPopup: EditControlSideDialog) {
        editControlPopup.loadValues(drawerData)
    }

    override fun cloneButton() {
        val cloneData = ControlDrawerData(drawerData)
        cloneData.properties.dynamicX = "0.5 * \${screen_width}"
        cloneData.properties.dynamicY = "0.5 * \${screen_height}"
        (parent as ControlLayout).addDrawer(cloneData)
    }

    override fun removeButton() {
        val layout = getControlLayoutParent()!!
        for (subButton in buttons) {
            layout.removeView(subButton)
        }
        layout.layout?.mDrawerDataList?.remove(drawerData)
        layout.removeView(this)
    }
}
