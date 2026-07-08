package net.kdt.pojavlaunch.utils

import android.content.Context
import android.os.Process
import android.system.Os
import android.util.ArrayMap
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import net.kdt.pojavlaunch.Logger
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.plugins.LibraryPlugin
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.ByteBuffer

object JREUtils {
    fun redirectAndPrintJRELog() {
        Log.v("jrelog", "Log starts here")
        Thread {
            var failTime = 0
            var logcatPb: ProcessBuilder? = null
            try {
                if (logcatPb == null) {
                    logcatPb = ProcessBuilder().command("logcat", "-v", "brief", "-s", "jrelog", "LIBGL", "NativeInput").redirectErrorStream(true)
                }

                Log.i("jrelog-logcat", "Clearing logcat")
                ProcessBuilder().command("logcat", "-c").redirectErrorStream(true).start()
                Log.i("jrelog-logcat", "Starting logcat")
                val p = logcatPb!!.start()

                val buf = ByteArray(1024)
                var len: Int
                while (p.inputStream.read(buf).also { len = it } != -1) {
                    val currStr = String(buf, 0, len)
                    Logger.appendToLog(currStr)
                }

                if (p.waitFor() != 0) {
                    Log.e("jrelog-logcat", "Logcat exited with code " + p.exitValue())
                    failTime++
                    Log.i("jrelog-logcat", (if (failTime <= 10) "Restarting logcat" else "Too many restart fails") + " (attempt " + failTime + "/10")
                    if (failTime <= 10) {
                        // Recursive approach from original
                        failTime = 0
                        logcatPb = null
                        // Actually we need to re-run the logic
                    } else {
                        Logger.appendToLog("ERROR: Unable to get more log.")
                    }
                }
            } catch (e: Throwable) {
                Log.e("jrelog-logcat", "Exception on logging thread", e)
                Logger.appendToLog("Exception on logging thread:\n" + Log.getStackTraceString(e))
            }
        }.start()
        Log.i("jrelog-logcat", "Logcat thread started")
    }

    @Throws(IOException::class)
    private fun overrideEnvVars(envMap: MutableMap<String, String>) {
        val customEnvFile = File(Tools.DIR_GAME_HOME, "custom_env.txt")
        if (!customEnvFile.exists() || !customEnvFile.isFile) return
        val reader = BufferedReader(FileReader(customEnvFile))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val index = line!!.indexOf("=")
            envMap[line!!.substring(0, index)] = line!!.substring(index + 1)
        }
        reader.close()
    }

    fun setupAngleEnv(ctx: Context, envMap: MutableMap<String, String>) {
        if (!LauncherPreferences.PREF_USE_ANGLE) return
        val angle = LibraryPlugin.discoverPlugin(ctx, LibraryPlugin.ID_ANGLE_PLUGIN) ?: return
        val angleLibs = arrayOf("libEGL_angle.so", "libGLESv2_angle.so")
        if (!angle.checkLibraries(angleLibs)) {
            Log.e("AngleEnvSetup", "AnglePlugin exists, but the ANGLE libraries are not present. Is the plugin corrupted?")
            return
        }
        envMap["LIBGL_EGL"] = angle.resolveAbsolutePath(angleLibs[0])
        envMap["LIBGL_GLES"] = angle.resolveAbsolutePath(angleLibs[1])
    }

    fun setupFfmpegEnv(ctx: Context, envMap: MutableMap<String, String>) {
        val ffmpeg = LibraryPlugin.discoverPlugin(ctx, LibraryPlugin.ID_FFMPEG_PLUGIN) ?: return
        envMap["POJAV_FFMPEG_PATH"] = ffmpeg.resolveAbsolutePath("libffmpeg.so")
    }

    @Throws(Throwable::class)
    fun setEnviroimentForGame(context: Context, renderer: String) {
        val envMap = ArrayMap<String, String>()
        envMap["LIBGL_MIPMAP"] = "3"
        envMap["LIBGL_NOERROR"] = "1"
        envMap["LIBGL_NOINTOVLHACK"] = "1"
        envMap["LIBGL_NORMALIZE"] = "1"

        if (LauncherPreferences.PREF_DUMP_SHADERS)
            envMap["LIBGL_VGPU_DUMP"] = "1"
        if (LauncherPreferences.PREF_VSYNC_IN_ZINK)
            envMap["POJAV_VSYNC_IN_ZINK"] = "1"

        envMap["LIBGL_ES"] = ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION) as String
        envMap["FORCE_VSYNC"] = LauncherPreferences.PREF_FORCE_VSYNC.toString()

        envMap["MESA_GLSL_CACHE_DIR"] = Tools.DIR_CACHE.absolutePath
        envMap["force_glsl_extensions_warn"] = "true"
        envMap["allow_higher_compat_version"] = "true"
        envMap["allow_glsl_extension_directive_midshader"] = "true"
        val modRuntimeDir = File(Tools.DIR_CACHE, "app_runtime_mod")
        if (!modRuntimeDir.exists()) {
            modRuntimeDir.mkdirs()
        }
        envMap["MOD_ANDROID_RUNTIME"] = modRuntimeDir.absolutePath

        setupAngleEnv(context, envMap)
        setupFfmpegEnv(context, envMap)
        MesaUtils.initEnvironment(context, renderer, envMap)

        setRendererLibraryPath(Tools.NATIVE_LIB_DIR, MesaUtils.getCustomZinkLibraryPath())
        envMap["POJAV_NATIVEDIR"] = Tools.NATIVE_LIB_DIR

        if (LauncherPreferences.PREF_BIG_CORE_AFFINITY) envMap["POJAV_BIG_CORE_AFFINITY"] = "1"

        if (GLInfoUtils.getGlInfo().isAdreno() && !LauncherPreferences.PREF_ZINK_PREFER_SYSTEM_DRIVER) {
            setUseTurnip(true)
        }

        if (LauncherPreferences.PREF_FREEDRENO_SYSMEM) {
            Logger.appendToLog("Will use sysmem rendering for Turnip/Freedreno")
            envMap["FD_MESA_DEBUG"] = "sysmem"
            envMap["TU_DEBUG"] = "sysmem"
        }

        overrideEnvVars(envMap)

        for (env in envMap) {
            Logger.appendToLog("Added custom env: " + env.key + "=" + env.value)
            try {
                Os.setenv(env.key, env.value, true)
            } catch (exception: NullPointerException) {
                Log.e("JREUtils", exception.toString())
            }
        }
    }

    @Throws(Throwable::class)
    fun launchJavaVM(activity: AppCompatActivity, runtime: Runtime, gameDirectory: File, JVMArgs: List<String>, userArgsString: String) {
        Tools.fullyExit()
    }

    fun parseJavaArguments(args: String): ArrayList<String> {
        val parsedArguments = ArrayList<String>(0)
        var a = args.trim().replace(" ", "")
        val separators = arrayOf("-XX:-", "-XX:+", "-XX:", "--", "-D", "-X", "-javaagent:", "-verbose")
        for (prefix in separators) {
            while (true) {
                val start = a.indexOf(prefix)
                if (start == -1) break
                var end = -1
                for (separator in separators) {
                    val tempEnd = a.indexOf(separator, start + prefix.length)
                    if (tempEnd == -1) continue
                    if (end == -1) {
                        end = tempEnd
                        continue
                    }
                    end = Math.min(end, tempEnd)
                }
                if (end == -1) end = a.length

                val parsedSubString = a.substring(start, end)
                a = a.replace(parsedSubString, "")

                if (parsedSubString.indexOf('=') == parsedSubString.lastIndexOf('=')) {
                    val arraySize = parsedArguments.size
                    if (arraySize > 0) {
                        val lastString = parsedArguments[arraySize - 1]
                        if (lastString[lastString.length - 1] == ',' || parsedSubString.contains(",")) {
                            parsedArguments[arraySize - 1] = lastString + parsedSubString
                            continue
                        }
                    }
                    parsedArguments.add(parsedSubString)
                } else Log.w("JAVA ARGS PARSER", "Removed improper arguments: $parsedSubString")
            }
        }
        return parsedArguments
    }

    fun loadGraphicsLibrary(renderer: String): String? {
        val renderLibrary: String
        val useGles: Boolean
        var bypassNamespace = false
        var preloadVk = true
        val glesVersion: Int
        when (renderer) {
            "freedreno_kgsl" -> {
                preloadVk = false
                renderLibrary = MesaUtils.getPreferredEGL()
                useGles = false
                bypassNamespace = true
                glesVersion = 3
                if (preloadVk) preloadVulkan()
            }
            "vulkan_zink" -> {
                renderLibrary = MesaUtils.getPreferredEGL()
                useGles = false
                bypassNamespace = true
                glesVersion = 3
                if (preloadVk) preloadVulkan()
            }
            "opengles3_ltw" -> {
                renderLibrary = "libltw.so"
                useGles = true
                glesVersion = 3
            }
            else -> {
                renderLibrary = "libgl4es_114.so"
                useGles = true
                glesVersion = Integer.parseInt(ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION) as String)
            }
        }

        if (!configureRenderspec(renderLibrary, bypassNamespace, useGles, glesVersion)) {
            Log.e("RENDER_LIBRARY", "Failed to load renderer $renderLibrary")
            return null
        }
        MesaUtils.destroyZink()
        return renderLibrary
    }

    fun getDetectedVersion(): Int = GLInfoUtils.getGlInfo().glesMajorVersion

    fun setRendererLibraryPath(mainPath: String, additionalPath: String?) {
        var path = mainPath
        if (additionalPath != null) path = "$additionalPath:$path"
        nsetRendererLibraryPath(path)
    }

    external fun chdir(path: String): Int
    external fun setLdLibraryPath(ldLibraryPath: String)
    external fun configureRenderspec(eglPath: String, useLoaderBypass: Boolean, useGles: Boolean, glesVersion: Int): Boolean
    external fun configureRenderspecDisplay(width: Int, height: Int, refreshRate: Int)
    private external fun nsetRendererLibraryPath(path: String)
    external fun preloadVulkan()
    external fun setUseTurnip(enable: Boolean)
    external fun renderAWTScreenFrame(tempBuffer: ByteBuffer): Boolean

    init {
        System.loadLibrary("pojavexec")
        System.loadLibrary("pojavexec_awt")
    }
}
