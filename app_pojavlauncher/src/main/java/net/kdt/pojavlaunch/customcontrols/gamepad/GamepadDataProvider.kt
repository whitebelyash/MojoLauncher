package net.kdt.pojavlaunch.customcontrols.gamepad

import git.artdeell.dnbootstrap.glfw.GrabListener

interface GamepadDataProvider {
    fun getMenuMap(): GamepadMap
    fun getGameMap(): GamepadMap
    fun isGrabbing(): Boolean
    fun attachGrabListener(grabListener: GrabListener)
}
