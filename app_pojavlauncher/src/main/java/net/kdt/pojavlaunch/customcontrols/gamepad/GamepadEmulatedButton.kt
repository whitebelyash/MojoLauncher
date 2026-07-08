package net.kdt.pojavlaunch.customcontrols.gamepad

import android.view.KeyEvent

open class GamepadEmulatedButton {
    var keycodes: ShortArray = ShortArray(0)
    protected var mIsDown = false

    fun update(event: KeyEvent) {
        val isKeyDown = event.action == KeyEvent.ACTION_DOWN
        update(isKeyDown)
    }

    fun update(isKeyDown: Boolean) {
        if (isKeyDown != mIsDown) {
            mIsDown = isKeyDown
            onDownStateChanged(mIsDown)
        }
    }

    open fun resetButtonState() {
        if (mIsDown) Gamepad.sendInput(keycodes, false)
        mIsDown = false
    }

    protected open fun onDownStateChanged(isDown: Boolean) {
        Gamepad.sendInput(keycodes, mIsDown)
    }
}
