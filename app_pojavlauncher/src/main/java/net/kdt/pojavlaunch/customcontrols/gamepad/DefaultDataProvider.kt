package net.kdt.pojavlaunch.customcontrols.gamepad

import git.artdeell.dnbootstrap.glfw.GLFW
import git.artdeell.dnbootstrap.glfw.GrabListener

object DefaultDataProvider : GamepadDataProvider {
    override fun getGameMap(): GamepadMap = GamepadMapStore.getGameMap()

    override fun getMenuMap(): GamepadMap = GamepadMapStore.getMenuMap()

    override fun isGrabbing(): Boolean = GLFW.isGrabbing()

    override fun attachGrabListener(grabListener: GrabListener) {
        GLFW.addGrabListener(grabListener)
    }
}
