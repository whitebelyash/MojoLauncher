package net.kdt.pojavlaunch.customcontrols

import android.content.Context
import androidx.annotation.Keep
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.Tools
import java.io.FileOutputStream
import java.io.IOException

@Keep
class CustomControls {
    var version = -1
    var scaledAt = 0f
    var mControlDataList: MutableList<ControlData> = ArrayList()
    var mDrawerDataList: MutableList<ControlDrawerData> = ArrayList()
    var mJoystickDataList: MutableList<ControlJoystickData> = ArrayList()
    var mLayoutBitmaps: LayoutBitmaps? = null

    constructor()

    constructor(
        mControlDataList: MutableList<ControlData>,
        mDrawerDataList: MutableList<ControlDrawerData>,
        mJoystickDataList: MutableList<ControlJoystickData>
    ) {
        this.mControlDataList = mControlDataList
        this.mDrawerDataList = mDrawerDataList
        this.mJoystickDataList = mJoystickDataList
        this.scaledAt = 100f
    }

    @Suppress("unused")
    constructor(ctx: Context) : this() {
        mControlDataList.add(ControlData(ControlData.getSpecialButtons()[0]))
        mControlDataList.add(ControlData(ControlData.getSpecialButtons()[1]))
        mControlDataList.add(ControlData(ControlData.getSpecialButtons()[2]))
        mControlDataList.add(ControlData(ControlData.getSpecialButtons()[3]))
        mControlDataList.add(ControlData(ControlData.getSpecialButtons()[4]))

        mControlDataList.add(
            ControlData(ctx, git.artdeell.mojo.R.string.control_debug, intArrayOf(LwjglGlfwKeycode.GLFW_KEY_F3),
                "\${margin}", "\${margin}", false)
        )
        mControlDataList.add(
            ControlData(ctx, git.artdeell.mojo.R.string.control_chat, intArrayOf(LwjglGlfwKeycode.GLFW_KEY_T),
                "\${margin} * 2 + \${width}", "\${margin}", false)
        )
        mControlDataList.add(
            ControlData(ctx, git.artdeell.mojo.R.string.control_listplayers, intArrayOf(LwjglGlfwKeycode.GLFW_KEY_TAB),
                "\${margin} * 4 + \${width} * 3", "\${margin}", false)
        )
        mControlDataList.add(
            ControlData(ctx, git.artdeell.mojo.R.string.control_thirdperson, intArrayOf(LwjglGlfwKeycode.GLFW_KEY_F5),
                "\${margin}", "\${height} + \${margin}", false)
        )

        mControlDataList.add(
            ControlData(ctx, git.artdeell.mojo.R.string.control_up, intArrayOf(LwjglGlfwKeycode.GLFW_KEY_W),
                "\${margin} * 2 + \${width}", "\${bottom} - \${margin} * 3 - \${height} * 2", true)
        )
        mControlDataList.add(
            ControlData(ctx, git.artdeell.mojo.R.string.control_left, intArrayOf(LwjglGlfwKeycode.GLFW_KEY_A),
                "\${margin}", "\${bottom} - \${margin} * 2 - \${height}", true)
        )
        mControlDataList.add(
            ControlData(ctx, git.artdeell.mojo.R.string.control_down, intArrayOf(LwjglGlfwKeycode.GLFW_KEY_S),
                "\${margin} * 2 + \${width}", "\${bottom} - \${margin}", true)
        )
        mControlDataList.add(
            ControlData(ctx, git.artdeell.mojo.R.string.control_right, intArrayOf(LwjglGlfwKeycode.GLFW_KEY_D),
                "\${margin} * 3 + \${width} * 2", "\${bottom} - \${margin} * 2 - \${height}", true)
        )

        mControlDataList.add(
            ControlData(ctx, git.artdeell.mojo.R.string.control_inventory, intArrayOf(LwjglGlfwKeycode.GLFW_KEY_E),
                "\${margin} * 3 + \${width} * 2", "\${bottom} - \${margin}", true)
        )

        val shiftData = ControlData(ctx, git.artdeell.mojo.R.string.control_shift,
            intArrayOf(LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT),
            "\${margin} * 2 + \${width}", "\${screen_height} - \${margin} * 2 - \${height} * 2", true)
        shiftData.isToggle = true
        mControlDataList.add(shiftData)
        mControlDataList.add(
            ControlData(ctx, git.artdeell.mojo.R.string.control_jump, intArrayOf(LwjglGlfwKeycode.GLFW_KEY_SPACE),
                "\${right} - \${margin} * 2 - \${width}", "\${bottom} - \${margin} * 2 - \${height}", true)
        )

        version = 8
    }

    @Throws(IOException::class)
    fun save(path: String) {
        version = 8
        val jsonControls = Tools.GLOBAL_GSON.toJson(this)
        FileOutputStream(path).use { fileOutputStream ->
            LayoutBitmaps.store(fileOutputStream, LayoutBitmaps.ControlsContainer(jsonControls, mLayoutBitmaps!!))
        }
    }
}
