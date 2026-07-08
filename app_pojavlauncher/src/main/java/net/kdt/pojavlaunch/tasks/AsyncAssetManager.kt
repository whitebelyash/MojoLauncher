package net.kdt.pojavlaunch.tasks

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.kdt.mcgui.ProgressLayout
import net.kdt.pojavlaunch.Architecture.archAsString
import net.kdt.pojavlaunch.PojavApplication.sExecutorService
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

object AsyncAssetManager {
    fun unpackRuntime(am: AssetManager) {
        var rtVersion: String? = null
        var currentRtVersion = MultiRTUtils.readInternalRuntimeVersion("Internal")
        try {
            rtVersion = Tools.read(am.open("components/jre/version"))
        } catch (e: IOException) {
            Log.e("JREAuto", "JRE was not included on this APK.", e)
        }
        val exactJREName = MultiRTUtils.getExactJreName(8)
        if (currentRtVersion == null && exactJREName != null && exactJREName != "Internal") return
        if (rtVersion == null) return
        if (rtVersion == currentRtVersion) return
        val finalRtVersion = rtVersion
        sExecutorService.execute {
            try {
                MultiRTUtils.installRuntimeNamedBinpack(
                    am.open("components/jre/universal.tar.xz"),
                    am.open("components/jre/bin-${archAsString(Tools.DEVICE_ARCHITECTURE)}.tar.xz"),
                    "Internal", finalRtVersion
                )
                MultiRTUtils.postPrepare("Internal")
            } catch (e: IOException) {
                Log.e("JREAuto", "Internal JRE unpack failed", e)
            }
        }
    }

    fun unpackSingleFiles(ctx: Context) {
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_SINGLE_FILES, 0)
        sExecutorService.execute {
            try {
                Tools.copyAssetFile(ctx, "default.json", Tools.CTRLMAP_PATH, false)
                Tools.copyAssetFile(ctx, "launcher_profiles.json", Tools.DIR_GAME_NEW, false)
                Tools.copyAssetFile(ctx, "resolv.conf", Tools.DIR_DATA, false)
            } catch (e: IOException) {
                Log.e("AsyncAssetManager", "Failed to unpack critical components !")
            }
            ProgressLayout.clearProgress(ProgressLayout.EXTRACT_SINGLE_FILES)
        }
    }

    fun unpackComponents(ctx: Context) {
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_COMPONENTS, 0)
        sExecutorService.execute {
            tryUnpackComponent(ctx, "caciocavallo", false)
            tryUnpackComponent(ctx, "caciocavallo17", false)
            tryUnpackComponent(ctx, "security", true)
            tryUnpackComponent(ctx, "arc_dns_injector", true)
            tryUnpackComponent(ctx, "forge_installer", true)
            tryUnpackComponent(ctx, "authlib-injector", true)
            ProgressLayout.clearProgress(ProgressLayout.EXTRACT_COMPONENTS)
        }
    }

    private fun readInstalledComponentVersion(componentRoot: File): String? {
        val localVersionFile = File(componentRoot, "version")
        try {
            FileInputStream(localVersionFile).use { fileInputStream ->
                return IOUtils.toString(fileInputStream, StandardCharsets.UTF_8)
            }
        } catch (_: IOException) {}
        return null
    }

    private fun readBuiltinComponentVersion(assetManager: AssetManager, componentName: String): String? {
        val componentVersionLocation = "components/$componentName/version"
        try {
            assetManager.open(componentVersionLocation).use { inputStream ->
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8)
            }
        } catch (_: IOException) {}
        return null
    }

    private fun tryUnpackComponent(ctx: Context, component: String, privateDirectory: Boolean) {
        try {
            unpackComponent(ctx, component, privateDirectory)
        } catch (e: IOException) {
            Log.e("AssetUnpacker", "Failed to unpack component $component", e)
        }
    }

    @Throws(IOException::class)
    private fun unpackComponent(ctx: Context, component: String, privateDirectory: Boolean) {
        val am = ctx.assets
        val rootDir = if (privateDirectory) Tools.DIR_DATA else Tools.DIR_GAME_HOME
        val componentTarget = File(rootDir, component)
        val installedVersion = readInstalledComponentVersion(componentTarget)
        val builtinVersion = readBuiltinComponentVersion(am, component)
        if (installedVersion != null && installedVersion == builtinVersion) {
            Log.i("AssetUnpacker", "Component $component is up-to-date, continuing...")
            return
        }
        Log.i("AssetUnpacker", "Updating $component")
        if (componentTarget.exists()) FileUtils.deleteDirectory(componentTarget)
        if (!componentTarget.mkdirs()) throw IOException("Failed to create directory for $component")
        val componentSource = "components/$component"
        val fileList = am.list(componentSource)
        for (fileName in fileList) {
            if (fileName == "version") continue
            val sourcePath = "$componentSource/$fileName"
            Tools.copyAssetFile(ctx, sourcePath, componentTarget.absolutePath, true)
        }
        Tools.write(File(componentTarget, "version"), builtinVersion)
    }

    fun extractDefaultSettings(context: Context, gamedir: File) {
        try {
            val gameDirPath = gamedir.absolutePath
            Tools.copyAssetFile(context, "options.txt", gameDirPath, false)
        } catch (e: IOException) {
            Tools.showError(context, e)
        }
    }
}
