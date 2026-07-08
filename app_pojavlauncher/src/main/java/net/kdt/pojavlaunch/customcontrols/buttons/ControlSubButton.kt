package net.kdt.pojavlaunch.customcontrols.buttons

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog

@SuppressLint("ViewConstructor")
class ControlSubButton : ControlButton {
    val parentDrawer: ControlDrawer

    constructor(layout: ControlLayout, properties: ControlData, parentDrawer: ControlDrawer) :
            super(layout, properties) {
        this.parentDrawer = parentDrawer
        filterProperties()
    }

    private fun filterProperties() {
        if (parentDrawer.drawerData.orientation != ControlDrawerData.Orientation.FREE) {
            mProperties.setHeight(parentDrawer.properties.getHeight())
            mProperties.setWidth(parentDrawer.properties.getWidth())
        }
        setProperties(mProperties, false)
    }

    override fun setVisible(isVisible: Boolean) {}

    override fun onGrabState(isGrabbing: Boolean) {}

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        if (parentDrawer.drawerData.orientation != ControlDrawerData.Orientation.FREE) {
            params.width = parentDrawer.mProperties.getWidth().toInt()
            params.height = parentDrawer.mProperties.getHeight().toInt()
        }
        super.setLayoutParams(params)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!getControlLayoutParent()?.getModifiable()!! || parentDrawer.drawerData.orientation == ControlDrawerData.Orientation.FREE) {
            return super.onTouchEvent(event)
        }

        if (event.actionMasked == MotionEvent.ACTION_UP) {
            onLongClick(this)
        }
        return true
    }

    override fun cloneButton() {
        val cloneData = ControlData(getProperties())
        cloneData.dynamicX = "0.5 * \${screen_width}"
        cloneData.dynamicY = "0.5 * \${screen_height}"
        (parent as ControlLayout).addSubButton(parentDrawer, cloneData)
    }

    override fun removeButton() {
        parentDrawer.drawerData.buttonProperties.remove(getProperties())
        parentDrawer.drawerData.buttonProperties.remove(getProperties())
        parentDrawer.buttons.remove(this)
        parentDrawer.syncButtons()
        super.removeButton()
    }

    override fun snapAndAlign(x: Float, y: Float) {
        if (parentDrawer.drawerData.orientation == ControlDrawerData.Orientation.FREE)
            super.snapAndAlign(x, y)
    }

    override fun loadEditValues(editControlPopup: EditControlSideDialog) {
        editControlPopup.loadSubButtonValues(getProperties(), parentDrawer.drawerData.orientation)
    }
}
