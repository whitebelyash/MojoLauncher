package net.kdt.pojavlaunch.utils

import android.util.Log

object GameOptionsUtils {
    private fun parseIntDefault(value: String?, defaultValue: Int): Int {
        if (value == null) return defaultValue
        return try {
            Integer.parseInt(value)
        } catch (_: NumberFormatException) {
            defaultValue
        }
    }

    private fun fixDeathCloud() {
        val info = GLInfoUtils.getGlInfo()
        if (!info.isArm()) return
        val cloudRange = parseIntDefault(MCOptionUtils.get("cloudRange"), 128)
        if (cloudRange <= 64) return
        MCOptionUtils.set("cloudRange", "64")
    }

    private fun disableNarrator() {
        if (parseIntDefault(MCOptionUtils.get("narrator"), 0) == 0) return
        MCOptionUtils.set("narrator", "0")
    }

    private fun disableFullscreen() {
        val fullscreen = MCOptionUtils.get("fullscreen") ?: return
        if (fullscreen == "true") MCOptionUtils.set("fullscreen", "false")
        else if (fullscreen == "1") MCOptionUtils.set("fullscreen", "0")
    }

    fun fixOptions(isLtw: Boolean) {
        try {
            MCOptionUtils.load()
        } catch (e: Exception) {
            Log.e("Tools", "Failed to load config", e)
        }

        if (isLtw) fixDeathCloud()
        disableFullscreen()
        disableNarrator()

        try {
            MCOptionUtils.save()
        } catch (e: Exception) {
            Log.e("Tools", "Failed to save config", e)
        }
    }
}
