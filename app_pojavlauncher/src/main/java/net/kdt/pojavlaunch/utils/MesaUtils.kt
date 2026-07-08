package net.kdt.pojavlaunch.utils

import android.content.Context
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.plugins.LibraryPlugin
import net.kdt.pojavlaunch.prefs.LauncherPreferences

object MesaUtils {
    const val MESA_EGL = "libEGL_mesa.so"
    const val MESA_EGL_LEGACY = "libEGL_legacy.so"

    private var zink: LibraryPlugin? = null

    fun initEnvironment(context: Context, renderer: String, envMap: MutableMap<String, String>) {
        when (renderer) {
            "vulkan_zink" -> {
                envMap["GALLIUM_DRIVER"] = "zink"
                envMap["MESA_LOADER_DRIVER_OVERRIDE"] = "zink"
                envMap["MESA_GLSL_VERSION_OVERRIDE"] = "460"
                if (!Architecture.isx86Device() && (LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GLInfoUtils.getGlInfo().isArm())) {
                    zink = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_ZINK_PLUGIN)
                    if (zink == null) return
                    envMap["MESA_GL_VERSION_OVERRIDE"] = "3.3"
                }
            }
            "freedreno_kgsl" -> {
                if (GLInfoUtils.getGlInfo().isAdreno()) {
                    envMap["MESA_LOADER_DRIVER_OVERRIDE"] = "kgsl"
                    if (GLInfoUtils.getGlInfo().isAdreno500Lower()) {
                        envMap["MESA_GL_VERSION_OVERRIDE"] = "3.3"
                        envMap["MESA_GLSL_VERSION_OVERRIDE"] = "330"
                    }
                }
            }
        }
    }

    fun destroyZink() {
        if (zink != null) {
            zink = null
            System.gc()
        }
    }

    fun getPreferredEGL(): String {
        return if (LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GLInfoUtils.getGlInfo().isArm()) {
            if (zink == null) return MESA_EGL
            if (!zink!!.checkLibraries(MESA_EGL_LEGACY)) return MESA_EGL
            zink!!.resolveAbsolutePath(MESA_EGL_LEGACY)
        } else MESA_EGL
    }

    fun getCustomZinkLibraryPath(): String? {
        return if ((LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GLInfoUtils.getGlInfo().isArm()) && zink != null)
            zink!!.libraryPath
        else null
    }
}
