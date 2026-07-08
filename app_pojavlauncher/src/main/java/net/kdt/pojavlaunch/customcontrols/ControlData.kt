package net.kdt.pojavlaunch.customcontrols

import android.util.ArrayMap
import androidx.annotation.Keep
import net.kdt.pojavlaunch.LwjglGlfwKeycode.GLFW_KEY_UNKNOWN
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.JSONUtils
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Collections

@Keep
class ControlData {
    companion object {
        const val SPECIALBTN_KEYBOARD = -1
        const val SPECIALBTN_TOGGLECTRL = -2
        const val SPECIALBTN_MOUSEPRI = -3
        const val SPECIALBTN_MOUSESEC = -4
        const val SPECIALBTN_VIRTUALMOUSE = -5
        const val SPECIALBTN_MOUSEMID = -6
        const val SPECIALBTN_SCROLLUP = -7
        const val SPECIALBTN_SCROLLDOWN = -8
        const val SPECIALBTN_MENU = -9
        const val SPECIALBTN_KEYBOARDPAN = -10

        private var SPECIAL_BUTTONS: Array<ControlData>? = null
        private var SPECIAL_BUTTON_NAME_ARRAY: MutableList<String>? = null
        private var builder: WeakReference<ExpressionBuilder> = WeakReference(null)
        private var conversionMap: WeakReference<ArrayMap<String, String>> = WeakReference(null)

        init {
            buildExpressionBuilder()
            buildConversionMap()
        }

        fun getSpecialButtons(): Array<ControlData> {
            if (SPECIAL_BUTTONS == null) {
                SPECIAL_BUTTONS = arrayOf(
                    ControlData("Keyboard", intArrayOf(SPECIALBTN_KEYBOARD), "\${margin} * 3 + \${width} * 2", "\${margin}", false),
                    ControlData("GUI", intArrayOf(SPECIALBTN_TOGGLECTRL), "\${margin}", "\${bottom} - \${margin}"),
                    ControlData("PRI", intArrayOf(SPECIALBTN_MOUSEPRI), "\${margin}", "\${screen_height} - \${margin} * 3 - \${height} * 3"),
                    ControlData("SEC", intArrayOf(SPECIALBTN_MOUSESEC), "\${margin} * 3 + \${width} * 2", "\${screen_height} - \${margin} * 3 - \${height} * 3"),
                    ControlData("Mouse", intArrayOf(SPECIALBTN_VIRTUALMOUSE), "\${right}", "\${margin}", false),
                    ControlData("MID", intArrayOf(SPECIALBTN_MOUSEMID), "\${margin}", "\${margin}"),
                    ControlData("SCROLLUP", intArrayOf(SPECIALBTN_SCROLLUP), "\${margin}", "\${margin}"),
                    ControlData("SCROLLDOWN", intArrayOf(SPECIALBTN_SCROLLDOWN), "\${margin}", "\${margin}"),
                    ControlData("MENU", intArrayOf(SPECIALBTN_MENU), "\${margin}", "\${margin}"),
                    ControlData("KeyboardPan", intArrayOf(SPECIALBTN_KEYBOARDPAN), "\${margin}", "\${margin}")
                )
            }
            return SPECIAL_BUTTONS!!
        }

        fun buildSpecialButtonArray(): List<String> {
            if (SPECIAL_BUTTON_NAME_ARRAY == null) {
                val nameList = mutableListOf<String>()
                for (btn in getSpecialButtons()) {
                    nameList.add("SPECIAL_" + btn.name)
                }
                SPECIAL_BUTTON_NAME_ARRAY = nameList
                Collections.reverse(SPECIAL_BUTTON_NAME_ARRAY)
            }
            return SPECIAL_BUTTON_NAME_ARRAY!!
        }

        private fun calculate(math: String): Float {
            setExpression(math)
            return builder.get()!!.build().evaluate().toFloat()
        }

        private fun inflateKeycodeArray(keycodes: IntArray): IntArray {
            val inflatedArray = intArrayOf(GLFW_KEY_UNKNOWN, GLFW_KEY_UNKNOWN, GLFW_KEY_UNKNOWN, GLFW_KEY_UNKNOWN)
            System.arraycopy(keycodes, 0, inflatedArray, 0, keycodes.size)
            return inflatedArray
        }

        private fun buildExpressionBuilder() {
            val expressionBuilder = ExpressionBuilder("1 + 1")
                .function(object : Function("dp", 1) {
                    override fun apply(vararg args: Double): Double {
                        return Tools.pxToDp(args[0].toFloat()).toDouble()
                    }
                })
                .function(object : Function("px", 1) {
                    override fun apply(vararg args: Double): Double {
                        return Tools.dpToPx(args[0].toFloat()).toDouble()
                    }
                })
            builder = WeakReference(expressionBuilder)
        }

        private fun setExpression(stringExpression: String) {
            if (builder.get() == null) buildExpressionBuilder()
            builder.get()!!.expression(stringExpression)
        }

        private fun buildConversionMap() {
            val keyValueMap = ArrayMap<String, String>(10)
            keyValueMap["top"] = "0"
            keyValueMap["left"] = "0"
            keyValueMap["right"] = "DUMMY_RIGHT"
            keyValueMap["bottom"] = "DUMMY_BOTTOM"
            keyValueMap["width"] = "DUMMY_WIDTH"
            keyValueMap["height"] = "DUMMY_HEIGHT"
            keyValueMap["screen_width"] = "DUMMY_DATA"
            keyValueMap["screen_height"] = "DUMMY_DATA"
            keyValueMap["margin"] = ControlInterface.getMarginDistance().toInt().toString()
            keyValueMap["preferred_scale"] = "DUMMY_DATA"
            conversionMap = WeakReference(keyValueMap)
        }
    }

    var isHideable = false
    var dynamicX: String = ""
    var dynamicY: String = ""
    var isToggle = false
    var passThruEnabled = false
    var name: String = ""
    var keycodes: IntArray = intArrayOf()
    var opacity = 0f
    var bgColor = 0
    var strokeColor = 0
    var strokeWidth = 0f
    var cornerRadius = 0f
    var isSwipeable = false
    var displayInGame = false
    var displayInMenu = false
    var bitmapTag: String? = null
    private var width: Float = 0f
    private var height: Float = 0f

    constructor() : this("button")

    constructor(name: String) : this(name, intArrayOf())

    constructor(name: String, keycodes: IntArray) : this(name, keycodes, 100f, 100f)

    constructor(name: String, keycodes: IntArray, x: Float, y: Float) : this(name, keycodes, x, y, 50f, 50f)

    constructor(ctx: android.content.Context, resId: Int, keycodes: IntArray, x: Float, y: Float, isSquare: Boolean) :
            this(ctx.resources.getString(resId), keycodes, x, y, isSquare)

    constructor(name: String, keycodes: IntArray, x: Float, y: Float, isSquare: Boolean) :
            this(name, keycodes, x, y, if (isSquare) 50f else 80f, if (isSquare) 50f else 30f)

    constructor(name: String, keycodes: IntArray, x: Float, y: Float, width: Float, height: Float) :
            this(name, keycodes, x.toString(), y.toString(), width, height, false)

    constructor(name: String, keycodes: IntArray, dynamicX: String, dynamicY: String) :
            this(name, keycodes, dynamicX, dynamicY, 50f, 50f, false)

    constructor(ctx: android.content.Context, resId: Int, keycodes: IntArray, dynamicX: String, dynamicY: String, isSquare: Boolean) :
            this(ctx.resources.getString(resId), keycodes, dynamicX, dynamicY, isSquare)

    constructor(name: String, keycodes: IntArray, dynamicX: String, dynamicY: String, isSquare: Boolean) :
            this(name, keycodes, dynamicX, dynamicY, if (isSquare) 50f else 80f, if (isSquare) 50f else 30f, false)

    constructor(name: String, keycodes: IntArray, dynamicX: String, dynamicY: String, width: Float, height: Float, isToggle: Boolean) :
            this(name, keycodes, dynamicX, dynamicY, width, height, isToggle, 1f, 0x4D000000, -0x1, 0f, 0f, true, true, false, false, null)

    constructor(
        name: String,
        keycodes: IntArray,
        dynamicX: String,
        dynamicY: String,
        width: Float,
        height: Float,
        isToggle: Boolean,
        opacity: Float,
        bgColor: Int,
        strokeColor: Int,
        strokeWidth: Float,
        cornerRadius: Float,
        displayInGame: Boolean,
        displayInMenu: Boolean,
        isSwipable: Boolean,
        mousePassthrough: Boolean,
        bitmapTag: String?
    ) {
        this.name = name
        this.keycodes = inflateKeycodeArray(keycodes)
        this.dynamicX = dynamicX
        this.dynamicY = dynamicY
        this.width = width
        this.height = height
        this.isToggle = isToggle
        this.opacity = opacity
        this.bgColor = bgColor
        this.strokeColor = strokeColor
        this.strokeWidth = strokeWidth
        this.cornerRadius = cornerRadius
        this.displayInGame = displayInGame
        this.displayInMenu = displayInMenu
        this.isSwipeable = isSwipable
        this.passThruEnabled = mousePassthrough
        this.bitmapTag = bitmapTag
    }

    constructor(controlData: ControlData) : this(
        controlData.name,
        controlData.keycodes,
        controlData.dynamicX,
        controlData.dynamicY,
        controlData.width,
        controlData.height,
        controlData.isToggle,
        controlData.opacity,
        controlData.bgColor,
        controlData.strokeColor,
        controlData.strokeWidth,
        controlData.cornerRadius,
        controlData.displayInGame,
        controlData.displayInMenu,
        controlData.isSwipeable,
        controlData.passThruEnabled,
        controlData.bitmapTag
    )

    fun insertDynamicPos(dynamicPos: String, w: Int, h: Int): Float {
        val insertedPos = JSONUtils.insertSingleJSONValue(dynamicPos, fillConversionMap(w, h))
        return calculate(insertedPos)
    }

    fun containsKeycode(keycodeToCheck: Int): Boolean {
        for (keycode in keycodes) {
            if (keycodeToCheck == keycode) return true
        }
        return false
    }

    fun getWidth(): Float = Tools.dpToPx(width)

    fun setWidth(widthInPx: Float) {
        width = Tools.pxToDp(widthInPx)
    }

    fun getHeight(): Float = Tools.dpToPx(height)

    fun setHeight(heightInPx: Float) {
        height = Tools.pxToDp(heightInPx)
    }

    private fun fillConversionMap(w: Int, h: Int): Map<String, String> {
        var valueMap = conversionMap.get()
        if (valueMap == null) {
            buildConversionMap()
            valueMap = conversionMap.get()
        }
        valueMap!!["right"] = (w - getWidth()).toString()
        valueMap["bottom"] = (h - getHeight()).toString()
        valueMap["width"] = getWidth().toString()
        valueMap["height"] = getHeight().toString()
        valueMap["screen_width"] = w.toString()
        valueMap["screen_height"] = h.toString()
        valueMap["preferred_scale"] = LauncherPreferences.PREF_BUTTONSIZE.toString()
        return valueMap
    }
}
