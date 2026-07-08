package net.kdt.pojavlaunch.plugins

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.File

class LibraryPlugin private constructor(
    private val appId: String,
    private val libraryPath: String
) {
    companion object {
        private const val TAG = "LibraryPlugin"

        const val ID_ANGLE_PLUGIN = "git.mojo.angle"
        const val ID_FFMPEG_PLUGIN = "git.mojo.ffmpeg"
        const val ID_ZINK_PLUGIN = "git.mojo.zink"

        fun discoverPlugin(ctx: Context, appId: String): LibraryPlugin? {
            val libraryPath: String
            try {
                val pluginPackage = ctx.packageManager.getPackageInfo(
                    appId,
                    PackageManager.GET_SHARED_LIBRARY_FILES
                )
                libraryPath = pluginPackage.applicationInfo.nativeLibraryDir
            } catch (e: Exception) {
                Log.e(TAG, "Plugin discover failed: " + e.message)
                return null
            }
            return LibraryPlugin(appId, libraryPath)
        }
    }

    fun getId(): String = appId
    fun getLibraryPath(): String = libraryPath

    fun resolveAbsolutePath(library: String): String {
        return File(libraryPath, library).absolutePath
    }

    fun checkLibraries(vararg libs: String): Boolean {
        for (lib in libs) {
            if (!File(libraryPath, lib).exists()) return false
        }
        return true
    }
}
