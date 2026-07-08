package net.kdt.pojavlaunch.customcontrols.keyboard

import net.kdt.pojavlaunch.AWTInputBridge
import net.kdt.pojavlaunch.AWTInputEvent

class AwtCharSender : CharacterSenderStrategy {
    override fun sendBackspace() {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_BACK_SPACE)
    }

    override fun sendEnter() {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_ENTER)
    }

    override fun sendChars(chars: CharSequence) {
        for (i in chars.indices) AWTInputBridge.sendChar(chars[i])
    }
}
