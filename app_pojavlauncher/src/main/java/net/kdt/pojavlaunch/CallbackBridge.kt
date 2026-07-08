package net.kdt.pojavlaunch

import android.content.Intent
import android.net.Uri
import android.view.Choreographer
import android.view.KeyEvent
import androidx.annotation.Keep
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import java.io.File
import git.artdeell.dnbootstrap.glfw.GLFW

object CallbackBridge {
    val sChoreographer = Choreographer.getInstance()

    @Volatile var windowWidth = 0
    @Volatile var windowHeight = 0
    @Volatile var holdingAlt = false
    @Volatile var holdingCapslock = false
    @Volatile var holdingCtrl = false
    @Volatile var holdingNumlock = false
    @Volatile var holdingShift = false

    fun performClick(button: Int) {
        val ox = GLFW.cursorX
        val oy = GLFW.cursorY
        GLFW.sendMouseEvent(button, 1, currentMods)
        sChoreographer.postFrameCallbackDelayed({ l ->
            GLFW.cursorX = ox
            GLFW.cursorY = oy
            GLFW.sendMouseEvent(button, 0, currentMods)
        }, 33)
    }

    fun sendKeyPress(keyCode: Int) {
        GLFW.sendKeyEvent(keyCode, true, currentMods)
        GLFW.sendKeyEvent(keyCode, false, currentMods)
    }

    fun sendMouseButton(button: Int, status: Boolean) {
        sendMouseKeycode(button, currentMods, status)
    }

    fun sendMouseKeycode(button: Int, modifiers: Int, isDown: Boolean) {
        GLFW.sendMouseEvent(button, if (isDown) 1 else 0, modifiers)
    }

    fun sendScroll(xoffset: Double, yoffset: Double) {
        GLFW.sendScrollEvent(xoffset, yoffset)
    }

    val currentMods: Int
        get() {
            var currMods = 0
            if (holdingAlt) currMods = currMods or LwjglGlfwKeycode.GLFW_MOD_ALT
            if (holdingCapslock) currMods = currMods or LwjglGlfwKeycode.GLFW_MOD_CAPS_LOCK
            if (holdingCtrl) currMods = currMods or LwjglGlfwKeycode.GLFW_MOD_CONTROL
            if (holdingNumlock) currMods = currMods or LwjglGlfwKeycode.GLFW_MOD_NUM_LOCK
            if (holdingShift) currMods = currMods or LwjglGlfwKeycode.GLFW_MOD_SHIFT
            return currMods
        }

    fun setModifiers(keyEvent: KeyEvent) {
        holdingAlt = keyEvent.isAltPressed
        holdingCapslock = keyEvent.isCapsLockOn
        holdingCtrl = keyEvent.isCtrlPressed
        holdingNumlock = keyEvent.isNumLockOn
        holdingShift = keyEvent.isShiftPressed
    }

    fun setModifiers(keyCode: Int, isDown: Boolean) {
        when (keyCode) {
            LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT -> holdingShift = isDown
            LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL -> holdingCtrl = isDown
            LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT -> holdingAlt = isDown
            LwjglGlfwKeycode.GLFW_KEY_CAPS_LOCK -> holdingCapslock = isDown
            LwjglGlfwKeycode.GLFW_KEY_NUM_LOCK -> holdingNumlock = isDown
        }
    }

    @Keep
    fun openLink(link: String) {
        ContextExecutor.executeActivity { ctx ->
            try {
                if (link.startsWith("file:")) {
                    var truncLength = 5
                    if (link.startsWith("file://")) truncLength = 7
                    val path = link.substring(truncLength)
                    Tools.openPath(ctx, File(path), false)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(Uri.parse(link), "*/*")
                    ctx.startActivity(intent)
                }
            } catch (th: Throwable) {
                Tools.showError(ctx, th)
            }
        }
    }

    @Suppress("unused")
    fun openPath(path: String) {
        ContextExecutor.executeActivity { ctx ->
            try {
                Tools.openPath(ctx, File(path), false)
            } catch (th: Throwable) {
                Tools.showError(ctx, th)
            }
        }
    }

    @JvmStatic external fun minibridgeInit()

    init {
        System.loadLibrary("pojavexec")
        minibridgeInit()
    }
}
