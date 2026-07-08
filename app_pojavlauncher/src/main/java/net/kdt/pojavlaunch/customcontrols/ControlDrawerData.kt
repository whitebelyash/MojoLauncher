package net.kdt.pojavlaunch.customcontrols

import androidx.annotation.Keep
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData.Orientation.Companion
import java.util.ArrayList

@Keep
class ControlDrawerData {
    @Keep
    enum class Orientation {
        DOWN, LEFT, UP, RIGHT, FREE;

        companion object {
            fun getOrientations(): Array<Orientation> = arrayOf(DOWN, LEFT, UP, RIGHT, FREE)

            fun orientationToInt(orientation: Orientation): Int = when (orientation) {
                DOWN -> 0
                LEFT -> 1
                UP -> 2
                RIGHT -> 3
                FREE -> 4
            }

            fun intToOrientation(by: Int): Orientation? = when (by) {
                0 -> DOWN
                1 -> LEFT
                2 -> UP
                3 -> RIGHT
                4 -> FREE
                else -> null
            }
        }
    }

    val buttonProperties: ArrayList<ControlData>
    val properties: ControlData
    var orientation: Orientation

    constructor() : this(ArrayList())

    constructor(buttonProperties: ArrayList<ControlData>) :
            this(buttonProperties, ControlData("Drawer", intArrayOf(), 100f, 100f))

    constructor(buttonProperties: ArrayList<ControlData>, properties: ControlData) :
            this(buttonProperties, properties, Orientation.LEFT)

    constructor(buttonProperties: ArrayList<ControlData>, properties: ControlData, orientation: Orientation) {
        this.buttonProperties = buttonProperties
        this.properties = properties
        this.orientation = orientation
    }

    constructor(drawerData: ControlDrawerData) {
        buttonProperties = ArrayList(drawerData.buttonProperties.size)
        for (controlData in drawerData.buttonProperties) {
            buttonProperties.add(ControlData(controlData))
        }
        properties = ControlData(drawerData.properties)
        orientation = drawerData.orientation
    }
}
