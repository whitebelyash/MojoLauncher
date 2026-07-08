package net.kdt.pojavlaunch.customcontrols

import android.graphics.Point
import com.google.gson.JsonSyntaxException
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.Tools
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

object LayoutConverter {
    @Throws(IOException::class, JsonSyntaxException::class)
    fun loadAndConvertIfNecessary(size: Point, jsonPath: String): CustomControls {
        val jsonFile = File(jsonPath)
        val container = LayoutBitmaps.load(jsonFile)
        val layoutBitmaps = container.mLayoutZip
        val controls = internalLoad(size, container.mControlsJson)
            ?: throw IOException("Unsupported control layout version")
        controls.mLayoutBitmaps = layoutBitmaps
        return controls
    }

    @Throws(JsonSyntaxException::class)
    fun internalLoad(size: Point, jsonLayoutData: String): CustomControls? {
        return try {
            val layoutJobj = JSONObject(jsonLayoutData)
            when {
                !layoutJobj.has("version") -> convertV1Layout(size, layoutJobj)
                else -> {
                    val version = layoutJobj.getInt("version")
                    when (version) {
                        2 -> convertV2Layout(size, layoutJobj)
                        3, 4, 5 -> convertV3_4Layout(layoutJobj)
                        6, 7 -> convertV6_7Layout(layoutJobj)
                        8 -> Tools.GLOBAL_GSON.fromJson(jsonLayoutData, CustomControls::class.java)
                        else -> null
                    }
                }
            }
        } catch (e: JSONException) {
            throw JsonSyntaxException("Failed to load the layout. Maybe it's corrupted?", e)
        }
    }

    fun convertV6_7Layout(oldLayoutJson: JSONObject): CustomControls {
        val layout = Tools.GLOBAL_GSON.fromJson(oldLayoutJson.toString(), CustomControls::class.java)
        for (data in layout.mJoystickDataList) {
            if (data.height > data.width) {
                val ratio = data.height / data.width
                data.dynamicX = data.dynamicX.replace("\${height}", "($ratio * \${height})")
                data.dynamicY = data.dynamicY.replace("\${height}", "($ratio * \${height})") + " + (${ratio - 1} * \${height})"
                data.setHeight(data.width)
            }
        }
        layout.version = 8
        return layout
    }

    private fun convertV3_4Layout(oldLayoutJson: JSONObject): CustomControls {
        val layout = Tools.GLOBAL_GSON.fromJson(oldLayoutJson.toString(), CustomControls::class.java)
        convertStrokeWidth(layout)
        layout.version = 6
        return layout
    }

    @Throws(JSONException::class)
    private fun convertV2Layout(size: Point, oldLayoutJson: JSONObject): CustomControls {
        val layout = Tools.GLOBAL_GSON.fromJson(oldLayoutJson.toString(), CustomControls::class.java)
        val layoutMainArray = oldLayoutJson.getJSONArray("mControlDataList")
        layout.mControlDataList = ArrayList(layoutMainArray.length())
        for (i in 0 until layoutMainArray.length()) {
            val button = layoutMainArray.getJSONObject(i)
            val nButton = Tools.GLOBAL_GSON.fromJson(button.toString(), ControlData::class.java)
            if (!Tools.isValidString(nButton.dynamicX) && button.has("x")) {
                val buttonC = button.getDouble("x")
                val ratio = buttonC / size.x
                nButton.dynamicX = "$ratio * \${screen_width}"
            }
            if (!Tools.isValidString(nButton.dynamicY) && button.has("y")) {
                val buttonC = button.getDouble("y")
                val ratio = buttonC / size.y
                nButton.dynamicY = "$ratio * \${screen_height}"
            }
            layout.mControlDataList.add(nButton)
        }
        val layoutDrawerArray = oldLayoutJson.getJSONArray("mDrawerDataList")
        layout.mDrawerDataList = ArrayList()
        for (i in 0 until layoutDrawerArray.length()) {
            val button = layoutDrawerArray.getJSONObject(i)
            val buttonProperties = button.getJSONObject("properties")
            val nButton = Tools.GLOBAL_GSON.fromJson(button.toString(), ControlDrawerData::class.java)
            if (!Tools.isValidString(nButton.properties.dynamicX) && buttonProperties.has("x")) {
                val buttonC = buttonProperties.getDouble("x")
                val ratio = buttonC / size.x
                nButton.properties.dynamicX = "$ratio * \${screen_width}"
            }
            if (!Tools.isValidString(nButton.properties.dynamicY) && buttonProperties.has("y")) {
                val buttonC = buttonProperties.getDouble("y")
                val ratio = buttonC / size.y
                nButton.properties.dynamicY = "$ratio * \${screen_height}"
            }
            layout.mDrawerDataList.add(nButton)
        }
        convertStrokeWidth(layout)
        layout.version = 3
        return layout
    }

    @Throws(JSONException::class)
    private fun convertV1Layout(size: Point, oldLayoutJson: JSONObject): CustomControls {
        val empty = CustomControls()
        val layoutMainArray = oldLayoutJson.getJSONArray("mControlDataList")
        for (i in 0 until layoutMainArray.length()) {
            val button = layoutMainArray.getJSONObject(i)
            val nButton = ControlData()
            val keycodes = intArrayOf(
                LwjglGlfwKeycode.GLFW_KEY_UNKNOWN,
                LwjglGlfwKeycode.GLFW_KEY_UNKNOWN,
                LwjglGlfwKeycode.GLFW_KEY_UNKNOWN,
                LwjglGlfwKeycode.GLFW_KEY_UNKNOWN
            )
            nButton.dynamicX = button.getString("dynamicX")
            nButton.dynamicY = button.getString("dynamicY")
            if (!Tools.isValidString(nButton.dynamicX) && button.has("x")) {
                val buttonC = button.getDouble("x")
                val ratio = buttonC / size.x
                nButton.dynamicX = "$ratio * \${screen_width}"
            }
            if (!Tools.isValidString(nButton.dynamicY) && button.has("y")) {
                val buttonC = button.getDouble("y")
                val ratio = buttonC / size.y
                nButton.dynamicY = "$ratio * \${screen_height}"
            }
            nButton.name = button.getString("name")
            nButton.opacity = ((button.getInt("transparency") - 100) * -1) / 100f
            nButton.passThruEnabled = button.getBoolean("passThruEnabled")
            nButton.isToggle = button.getBoolean("isToggle")
            nButton.setHeight(button.getInt("height").toFloat())
            nButton.setWidth(button.getInt("width").toFloat())
            nButton.bgColor = 0x4d000000.toInt()
            nButton.strokeWidth = 0f
            if (button.getBoolean("isRound")) {
                nButton.cornerRadius = 35f
            }
            var nextIdx = 0
            if (button.getBoolean("holdShift")) {
                keycodes[nextIdx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT
                nextIdx++
            }
            if (button.getBoolean("holdCtrl")) {
                keycodes[nextIdx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL
                nextIdx++
            }
            if (button.getBoolean("holdAlt")) {
                keycodes[nextIdx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT
                nextIdx++
            }
            keycodes[nextIdx] = button.getInt("keycode")
            nButton.keycodes = keycodes
            empty.mControlDataList.add(nButton)
        }
        empty.scaledAt = oldLayoutJson.getDouble("scaledAt").toFloat()
        empty.version = 3
        return empty
    }

    private fun convertStrokeWidth(layout: CustomControls) {
        for (data in layout.mControlDataList) {
            data.strokeWidth = Tools.pxToDp(computeStrokeWidth(data.strokeWidth, data.width, data.height))
        }
        for (data in layout.mDrawerDataList) {
            data.properties.strokeWidth = Tools.pxToDp(
                computeStrokeWidth(data.properties.strokeWidth, data.properties.width, data.properties.height)
            )
            for (subButtonData in data.buttonProperties) {
                subButtonData.strokeWidth = Tools.pxToDp(
                    computeStrokeWidth(subButtonData.strokeWidth, data.properties.width, data.properties.width)
                )
            }
        }
    }

    fun computeStrokeWidth(widthInPercent: Float, width: Float, height: Float): Int {
        val maxSize = maxOf(width, height)
        return ((maxSize / 2) * (widthInPercent / 100)).toInt()
    }
}
