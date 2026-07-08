package net.kdt.pojavlaunch.utils.jre

import android.util.ArrayMap
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.accounts.Account
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.lifecycle.LifecycleAwareAlertDialog
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.*
import java.io.File
import java.io.IOException
import java.text.ParseException
import java.util.*
import git.artdeell.mojo.R

object GameRunner {
    private fun hasSodium(gameDir: File): Boolean {
        val modsDir = File(gameDir, "mods")
        val mods = modsDir.listFiles { file -> file.isFile && file.name.endsWith(".jar") } ?: return false
        return mods.any { file ->
            val name = file.name
            name.contains("sodium") || name.contains("embeddium") || name.contains("rubidium")
        }
    }

    private fun hasAngelica(gameDir: File): Boolean {
        val modsDir = File(gameDir, "mods")
        val mods = modsDir.listFiles { file -> file.isFile && file.name.endsWith(".jar") } ?: return false
        return mods.any { file -> file.name.contains("angelica") }
    }

    @Throws(ParseException::class)
    private fun affectedByRenderDistanceIssue(version: JVersionList.Version): Boolean {
        if (LauncherPreferences.PREF_USE_ANGLE) return false
        val info = GLInfoUtils.getGlInfo()
        return info.isAdreno() &&
                info.glesMajorVersion >= 3 &&
                DateUtils.dateBefore(DateUtils.getOriginalReleaseDate(version)!!, 2025, 2, 25)
    }

    @Throws(ParseException::class)
    private fun checkRenderDistance(version: JVersionList.Version, gamedir: File): Boolean {
        if (!affectedByRenderDistanceIssue(version)) return false
        if (hasSodium(gamedir)) return false
        try {
            MCOptionUtils.load()
        } catch (e: Exception) {
            Log.e("Tools", "Failed to load config", e)
        }
        val renderDistance = GameOptionsUtils.parseIntDefault(MCOptionUtils.get("renderDistance"), 12)
        return renderDistance > 7
    }

    @Throws(Exception::class)
    private fun isGl4esCompatible(version: JVersionList.Version): Boolean {
        return DateUtils.dateBefore(DateUtils.getOriginalReleaseDate(version)!!, 2025, 1, 7)
    }

    @Throws(Exception::class)
    private fun isCompatContext(version: JVersionList.Version): Boolean {
        return DateUtils.dateBefore(DateUtils.getOriginalReleaseDate(version)!!, 2021, 3, 9)
    }

    @Throws(InterruptedException::class)
    private fun showDialog(activity: AppCompatActivity, message: Int): Boolean {
        val dialogCreator = LifecycleAwareAlertDialog.DialogCreator { _, dialogBuilder ->
            dialogBuilder.setMessage(activity.getString(message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
        }
        return LifecycleAwareAlertDialog.haltOnDialog(activity.lifecycle, activity, dialogCreator)
    }

    @Throws(InterruptedException::class, IOException::class)
    private fun switchLtw(hasLtw: Boolean, instance: Instance, activity: AppCompatActivity, resId: Int): String? {
        if (hasLtw) {
            val ltwRenderer = "opengles3_ltw"
            instance.renderer = ltwRenderer
            instance.write()
            return ltwRenderer
        } else {
            showDialog(activity, resId)
            System.exit(0)
            return null
        }
    }

    @Throws(Throwable::class)
    fun launchGame(activity: AppCompatActivity, account: Account, instance: Instance, versionId: String, classpath: Array<File>, rendererName: String) {
        var rName = rendererName
        var freeDeviceMemory = Tools.getFreeDeviceMemory(activity)
        var localeString: Int
        var freeAddressSpace = if (Architecture.is32BitsDevice()) Tools.getMaxContinuousAddressSpaceSize() else -1
        Log.i("MemStat", "Free RAM: $freeDeviceMemory Addressable: $freeAddressSpace")
        if (freeDeviceMemory > freeAddressSpace && freeAddressSpace != -1) {
            freeDeviceMemory = freeAddressSpace
            localeString = R.string.address_memory_warning_msg
        } else {
            localeString = R.string.memory_warning_msg
        }

        if (LauncherPreferences.PREF_RAM_ALLOCATION > freeDeviceMemory) {
            val finalDeviceMemory = freeDeviceMemory
            val dialogCreator = LifecycleAwareAlertDialog.DialogCreator { _, builder ->
                builder.setMessage(activity.getString(localeString, finalDeviceMemory, LauncherPreferences.PREF_RAM_ALLOCATION))
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
            }
            if (LifecycleAwareAlertDialog.haltOnDialog(activity.lifecycle, activity, dialogCreator)) return
        }

        val gamedir = instance.gameDirectory
        val versionInfo = Tools.getVersionInfo(versionId)

        if (isCompatContext(versionInfo) && !hasAngelica(gamedir) && rName == "opengles3_ltw") {
            instance.renderer = rName = "opengles2"
            instance.write()
        }

        val isGl4es = rName == "opengles2"
        val ltwSupported = RendererCompatUtil.getCompatibleRenderers(activity).rendererIds.contains("opengles3_ltw")
        if (!isCompatContext(versionInfo) && isGl4es && hasSodium(gamedir)) {
            rName = switchLtw(ltwSupported, instance, activity, R.string.compat_sodium_not_supported) ?: return
        }

        if (!isGl4esCompatible(versionInfo) && isGl4es) {
            rName = switchLtw(ltwSupported, instance, activity, R.string.compat_version_not_supported) ?: return
        }
        RendererCompatUtil.releaseRenderersCache()

        val isLtw = rName == "opengles3_ltw"

        if (isLtw && checkRenderDistance(versionInfo, gamedir)) {
            if (showDialog(activity, R.string.ltw_render_distance_warning_msg)) return
            try {
                MCOptionUtils.set("renderDistance", "7")
                MCOptionUtils.save()
            } catch (e: Exception) {
                Log.e("Tools", "Failed to fix render distance setting", e)
            }
        }

        GameOptionsUtils.fixOptions(isLtw)

        if (isLtw && GLInfoUtils.getGlInfo().forcedMsaa) {
            if (showDialog(activity, R.string.ltw_4x_msaa_warning_msg)) return
        }

        var requiredJavaVersion = 8
        if (versionInfo.javaVersion != null) requiredJavaVersion = versionInfo.javaVersion.majorVersion

        val runtime = MultiRTUtils.forceReread(pickRuntime(instance, requiredJavaVersion))

        disableSplash(gamedir)
        val launchArgs = getMoJsonClientArgs(account, versionInfo, gamedir)

        OldVersionsUtils.selectOpenGlVersion(versionInfo)

        val launchClassPath = ArrayList<String>(classpath.size)
        for (classpathEntry in classpath) {
            val entryPath = classpathEntry.absolutePath
            if (!classpathEntry.exists()) {
                Log.w("GameRunner", "Skipped classpath entry $entryPath because it is missing")
            }
            launchClassPath.add(entryPath)
        }
        launchClassPath.trimToSize()

        val javaArgList = ArrayList<String>()

        if (versionInfo.logging?.client?.file != null) {
            var configFile = Tools.DIR_DATA + "/security/" + versionInfo.logging.client.file.id.replace("client", "log4j-rce-patch")
            if (!File(configFile).exists()) {
                configFile = Tools.DIR_GAME_NEW + "/" + versionInfo.logging.client.file.id
            }
            javaArgList.add("-Dlog4j.configurationFile=$configFile")
        }

        val versionSpecificNativesDir = File(Tools.DIR_CACHE, "natives/$versionId")
        if (versionSpecificNativesDir.exists()) {
            val dirPath = versionSpecificNativesDir.absolutePath
            javaArgList.add("-Djava.library.path=$dirPath:${Tools.NATIVE_LIB_DIR}")
            javaArgList.add("-Djna.boot.library.path=$dirPath")
        }

        val lwjglExtractDir = File(Tools.DIR_CACHE, "lwjgl_native/$versionId")
        FileUtils.ensureDirectory(lwjglExtractDir)
        javaArgList.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=${lwjglExtractDir.absolutePath}")

        addAuthlibInjectorArgs(javaArgList, account)

        javaArgList.addAll(getMoJsonJvmArgs(versionId))

        javaArgList.addAll(JREUtils.parseJavaArguments(instance.launchArgs))

        JREUtils.setEnviroimentForGame(activity, rName)
        JREUtils.chdir(instance.gameDirectory!!.absolutePath)

        var rendererLibrary = JREUtils.loadGraphicsLibrary(rName)
        if (rendererLibrary == null) {
            Log.i("GameRunner", "Falling back to GL4ES 1.1.4")
            rName = "opengles2"
            rendererLibrary = JREUtils.loadGraphicsLibrary(rName)
        }
        if (rendererLibrary == null) {
            if (showDialog(activity, R.string.gr_err_renderer_load_Failed)) return
            System.exit(0)
        }
        javaArgList.add("-Dorg.lwjgl.opengl.libname=libGLMojo.so")
        javaArgList.add("-Dorg.lwjgl.freetype.libname=${Tools.NATIVE_LIB_DIR}/libfreetype.so")

        activity.runOnUiThread {
            Toast.makeText(activity, activity.getString(R.string.autoram_info_msg, LauncherPreferences.PREF_RAM_ALLOCATION), Toast.LENGTH_SHORT).show()
        }

        Log.i("GameRunner", "Running with ${launchArgs.toString()}")

        try {
            JavaRunner.nativeSetupExit(activity)
            JavaRunner.startJvm(runtime, javaArgList, launchClassPath, versionInfo.mainClass, launchArgs)
        } catch (e: VMLoadException) {
            val dialogCreator = LifecycleAwareAlertDialog.DialogCreator { _, builder ->
                builder.setMessage(e.toString(activity)).setPositiveButton(android.R.string.ok) { _, _ -> }
            }
            if (LifecycleAwareAlertDialog.haltOnDialog(activity.lifecycle, activity, dialogCreator)) return
        }

        Tools.fullyExit()
    }

    private fun disableSplash(dir: File) {
        val configDir = File(dir, "config")
        if (FileUtils.ensureDirectorySilently(configDir)) {
            val forgeSplashFile = File(dir, "config/splash.properties")
            var forgeSplashContent = "enabled=true"
            try {
                if (forgeSplashFile.exists()) {
                    forgeSplashContent = Tools.read(forgeSplashFile.absolutePath)
                }
                if (forgeSplashContent.contains("enabled=true")) {
                    Tools.write(forgeSplashFile, forgeSplashContent.replace("enabled=true", "enabled=false"))
                }
            } catch (e: IOException) {
                Log.w(Tools.APP_NAME, "Could not disable Forge 1.12.2 and below splash screen!", e)
            }
        } else {
            Log.w(Tools.APP_NAME, "Failed to create the configuration directory")
        }
    }

    private fun addAuthlibInjectorArgs(javaArgList: MutableList<String>, account: Account) {
        val injectorUrl = account.authType.injectorUrl ?: return
        javaArgList.add("-javaagent:${Tools.DIR_DATA}/authlib-injector/authlib-injector.jar=$injectorUrl")
    }

    private fun getMoJsonJvmArgs(versionName: String): List<String> {
        val versionInfo = Tools.getVersionInfo(versionName, true)
        if (versionInfo.inheritsFrom == null || versionInfo.arguments?.jvm == null) {
            return Collections.emptyList()
        }

        val varArgMap = ArrayMap<String, String>()
        varArgMap["classpath_separator"] = ":"
        varArgMap["library_directory"] = Tools.DIR_HOME_LIBRARY
        varArgMap["version_name"] = versionInfo.id
        varArgMap["natives_directory"] = Tools.NATIVE_LIB_DIR

        val clientVmArgs = ArrayList<String>()
        if (versionInfo.arguments != null) {
            for (arg in versionInfo.arguments.jvm!!) {
                if (arg is String) {
                    clientVmArgs.add(arg)
                }
            }
        }
        return JSONUtils.insertJSONValueList(clientVmArgs, varArgMap)
    }

    private fun getMoJsonClientArgs(profile: Account, versionInfo: JVersionList.Version, gameDir: File): List<String> {
        val username = profile.username
        var versionName = versionInfo.id
        if (versionInfo.inheritsFrom != null) {
            versionName = versionInfo.inheritsFrom
        }

        var userType = "mojang"
        try {
            val creationDate = DateUtils.getOriginalReleaseDate(versionInfo)
            if (creationDate != null && !DateUtils.dateBefore(creationDate, 2022, 9, 26)) {
                userType = "msa"
            }
        } catch (e: ParseException) {
            Log.e("CheckForProfileKey", "Failed to determine profile creation date, using \"mojang\"", e)
        }

        val varArgMap = ArrayMap<String, String>()
        varArgMap["auth_session"] = profile.accessToken
        varArgMap["auth_access_token"] = profile.accessToken
        varArgMap["auth_player_name"] = username
        varArgMap["auth_uuid"] = profile.profileId.replace("-", "")
        varArgMap["auth_xuid"] = profile.xuid
        varArgMap["assets_root"] = Tools.ASSETS_PATH
        varArgMap["assets_index_name"] = versionInfo.assets
        varArgMap["game_assets"] = Tools.ASSETS_PATH
        varArgMap["game_directory"] = gameDir.absolutePath
        varArgMap["user_properties"] = "{}"
        varArgMap["user_type"] = userType
        varArgMap["version_name"] = versionName
        varArgMap["version_type"] = versionInfo.type

        val clientArgs = ArrayList<String>()
        if (versionInfo.arguments?.game != null) {
            for (arg in versionInfo.arguments.game!!) {
                if (arg is String) {
                    clientArgs.add(arg)
                }
            }
        }
        if (versionInfo.minecraftArguments != null) {
            clientArgs.addAll(splitAndFilterEmpty(versionInfo.minecraftArguments))
        }
        return JSONUtils.insertJSONValueList(clientArgs, varArgMap)
    }

    private fun splitAndFilterEmpty(argStr: String): List<String> {
        return argStr.split(" ").filter { s: String -> s.isNotEmpty() }
    }

    @NonNull
    fun pickRuntime(instance: Instance, targetJavaVersion: Int): String {
        var runtime = Tools.getSelectedRuntime(instance)
        val pickedRuntime = MultiRTUtils.read(runtime)
        if (runtime == null || pickedRuntime.javaVersion == 0 || pickedRuntime.javaVersion < targetJavaVersion) {
            val preferredRuntime = MultiRTUtils.getNearestJreName(targetJavaVersion)
                ?: throw RuntimeException("Failed to autopick runtime!")
            if (instance.selectedRuntime != null) {
                instance.selectedRuntime = preferredRuntime
                instance.maybeWrite()
            }
            runtime = preferredRuntime
        }
        return runtime
    }
}
