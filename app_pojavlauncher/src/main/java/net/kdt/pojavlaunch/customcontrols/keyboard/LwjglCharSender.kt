package net.kdt.pojavlaunch.customcontrols.keyboard

import git.artdeell.dnbootstrap.glfw.GLFW
import net.kdt.pojavlaunch.CallbackBridge
import net.kdt.pojavlaunch.LwjglGlfwKeycode

class LwjglCharSender : CharacterSenderStrategy {
    override fun sendBackspace() {
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE)
    }

    override fun sendEnter() {
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER)
    }

    override fun sendChars(chars: CharSequence) {
        GLFW.sendBulkUnicodeEvent(chars.toString(), CallbackBridge.getCurrentMods())
    }
}
