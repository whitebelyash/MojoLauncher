package net.kdt.pojavlaunch.instances

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.JSONUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Instance : DisplayInstance() {
    companion object {
        const val ARGS_MODE_REPLACE = 0
        const val ARGS_MODE_MERGE_DEFAULT_FIRST = 1
        const val ARGS_MODE_MERGE_INSTANCE_FIRST = 2
        const val ARGS_MODE_LAST = ARGS_MODE_MERGE_INSTANCE_FIRST

        const val VERSION_LATEST_RELEASE = "latest_release"
        const val VERSION_LATEST_SNAPSHOT = "latest_snapshot"
    }

    @JvmField var installer: InstanceInstaller? = null
    @JvmField var renderer: String? = null
    @JvmField var jvmArgs: String? = null
    @JvmField var argsMode: Int = 0
    @JvmField var selectedRuntime: String? = null
    @JvmField var controlLayout: String? = null
    @JvmField var sharedData: Boolean = false

    protected constructor()

    override fun sanitize() {
        super.sanitize()
        sanitizeArgs()
    }

    private fun sanitizeArgs() {
        if (argsMode > ARGS_MODE_LAST) {
            argsMode = 0
            jvmArgs = null
        }
    }

    @Throws(IOException::class)
    fun write() {
        JSONUtils.writeToFile(Instances.metadataLocation(mInstanceRoot!!), this)
    }

    fun maybeWrite() {
        try {
            write()
        } catch (e: IOException) {
            Log.e("Instance", "Failed to write", e)
        }
    }

    @Throws(IOException::class)
    fun encodeNewIcon(bitmap: Bitmap) {
        FileOutputStream(getInstanceIconLocation()).use { fileOutputStream ->
            bitmap.compress(
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
                    Bitmap.CompressFormat.WEBP
                else
                    Bitmap.CompressFormat.WEBP_LOSSY,
                60,
                fileOutputStream
            )
        }
    }

    fun getLaunchRenderer(): String {
        if (Tools.isValidString(renderer)) return renderer!!
        return LauncherPreferences.PREF_RENDERER
    }

    fun getLaunchArgs(): String {
        if (!Tools.isValidString(jvmArgs)) return LauncherPreferences.PREF_CUSTOM_JAVA_ARGS
        return when (argsMode) {
            ARGS_MODE_REPLACE -> jvmArgs!!
            ARGS_MODE_MERGE_DEFAULT_FIRST -> LauncherPreferences.PREF_CUSTOM_JAVA_ARGS + " " + jvmArgs
            ARGS_MODE_MERGE_INSTANCE_FIRST -> jvmArgs!! + " " + LauncherPreferences.PREF_CUSTOM_JAVA_ARGS
            else -> throw RuntimeException("Unknown value for argsMode: $argsMode")
        }
    }

    fun getLaunchControls(): String {
        if (!Tools.isValidString(controlLayout)) return LauncherPreferences.PREF_DEFAULTCTRL_PATH
        return Tools.CTRLMAP_PATH + "/" + controlLayout
    }

    fun getGameDirectory(): File {
        return if (sharedData) Instances.SHARED_DATA_DIRECTORY else mInstanceRoot!!
    }
}
