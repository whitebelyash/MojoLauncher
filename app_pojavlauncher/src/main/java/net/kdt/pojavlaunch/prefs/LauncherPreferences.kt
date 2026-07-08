package net.kdt.pojavlaunch.prefs

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.utils.JREUtils
import java.io.IOException
import git.artdeell.mojo.R

object LauncherPreferences {
    const val PREF_KEY_CURRENT_INSTANCE = "currentInstance"
    const val PREF_KEY_SKIP_NOTIFICATION_CHECK = "skipNotificationPermissionCheck"

    var DEFAULT_PREF: SharedPreferences? = null
    var PREF_RENDERER = "opengles2"
    var PREF_IGNORE_NOTCH = false
    var PREF_BUTTONSIZE = 100f
    var PREF_MOUSESCALE = 1f
    var PREF_LONGPRESS_TRIGGER = 300
    var PREF_DEFAULTCTRL_PATH: String = Tools.CTRLDEF_FILE
    var PREF_CUSTOM_JAVA_ARGS: String? = null
    var PREF_FORCE_ENGLISH = false
    const val PREF_VERSION_REPOS = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    var PREF_DISABLE_GESTURES = false
    var PREF_DISABLE_SWAP_HAND = false
    var PREF_MOUSESPEED = 1f
    var PREF_RAM_ALLOCATION = 0
    var PREF_DEFAULT_RUNTIME: String? = null
    var PREF_SUSTAINED_PERFORMANCE = false
    var PREF_VIRTUAL_MOUSE_START = false
    var PREF_USE_ALTERNATE_SURFACE = true
    var PREF_JAVA_SANDBOX = true
    var PREF_SCALE_FACTOR = 1f
    var PREF_ENABLE_GYRO = false
    var PREF_GYRO_SENSITIVITY = 1f
    var PREF_GYRO_SAMPLE_RATE = 16
    var PREF_GYRO_SMOOTHING = true
    var PREF_GYRO_INVERT_X = false
    var PREF_GYRO_INVERT_Y = false
    var PREF_FORCE_VSYNC = false
    var PREF_USE_ANGLE = false
    var PREF_BUTTON_ALL_CAPS = true
    var PREF_DUMP_SHADERS = false
    var PREF_DEADZONE_SCALE = 1f
    var PREF_BIG_CORE_AFFINITY = false
    var PREF_ZINK_PREFER_SYSTEM_DRIVER = false
    var PREF_ZINK_FORCE_LEGACY = false
    var PREF_VERIFY_MANIFEST = true
    var PREF_DOWNLOAD_SOURCE = "default"
    var PREF_SKIP_NOTIFICATION_PERMISSION_CHECK = false
    var PREF_VSYNC_IN_ZINK = true
    var PREF_RAPID_START = true
    var PREF_VERIFY_FILES = true
    var PREF_FREEDRENO_SYSMEM = false
    var PREF_KEYBOARD_AUTOPANNING = true

    fun loadPreferences(ctx: Context) {
        Tools.initStorageConstants(ctx)
        val isDevicePowerful = isDevicePowerful(ctx)

        PREF_RENDERER = DEFAULT_PREF!!.getString("renderer", "opengles2")!!
        PREF_BUTTONSIZE = DEFAULT_PREF!!.getInt("buttonscale", 100).toFloat()
        PREF_MOUSESCALE = DEFAULT_PREF!!.getInt("mousescale", 100) / 100f
        PREF_MOUSESPEED = DEFAULT_PREF!!.getInt("mousespeed", 100) / 100f
        PREF_IGNORE_NOTCH = DEFAULT_PREF!!.getBoolean("ignoreNotch", false)
        PREF_LONGPRESS_TRIGGER = DEFAULT_PREF!!.getInt("timeLongPressTrigger", 300)
        PREF_DEFAULTCTRL_PATH = DEFAULT_PREF!!.getString("defaultCtrl", Tools.CTRLDEF_FILE)!!
        PREF_FORCE_ENGLISH = DEFAULT_PREF!!.getBoolean("force_english", false)
        PREF_DISABLE_GESTURES = DEFAULT_PREF!!.getBoolean("disableGestures", false)
        PREF_DISABLE_SWAP_HAND = DEFAULT_PREF!!.getBoolean("disableDoubleTap", false)
        PREF_RAM_ALLOCATION = DEFAULT_PREF!!.getInt("allocation", findBestRAMAllocation(ctx))
        PREF_CUSTOM_JAVA_ARGS = DEFAULT_PREF!!.getString("javaArgs", "")
        PREF_SUSTAINED_PERFORMANCE = DEFAULT_PREF!!.getBoolean("sustainedPerformance", isDevicePowerful)
        PREF_VIRTUAL_MOUSE_START = DEFAULT_PREF!!.getBoolean("mouse_start", false)
        PREF_USE_ALTERNATE_SURFACE = DEFAULT_PREF!!.getBoolean("alternate_surface", isDevicePowerful)
        PREF_JAVA_SANDBOX = DEFAULT_PREF!!.getBoolean("java_sandbox", true)
        PREF_SCALE_FACTOR = DEFAULT_PREF!!.getInt("resolutionRatio", findBestResolution(ctx, isDevicePowerful)) / 100f
        PREF_ENABLE_GYRO = DEFAULT_PREF!!.getBoolean("enableGyro", false)
        PREF_GYRO_SENSITIVITY = DEFAULT_PREF!!.getInt("gyroSensitivity", 100) / 100f
        PREF_GYRO_SAMPLE_RATE = DEFAULT_PREF!!.getInt("gyroSampleRate", 16)
        PREF_GYRO_SMOOTHING = DEFAULT_PREF!!.getBoolean("gyroSmoothing", true)
        PREF_GYRO_INVERT_X = DEFAULT_PREF!!.getBoolean("gyroInvertX", false)
        PREF_GYRO_INVERT_Y = DEFAULT_PREF!!.getBoolean("gyroInvertY", false)
        PREF_FORCE_VSYNC = DEFAULT_PREF!!.getBoolean("force_vsync", isDevicePowerful)
        PREF_USE_ANGLE = DEFAULT_PREF!!.getBoolean("use_angle", false)
        PREF_BUTTON_ALL_CAPS = DEFAULT_PREF!!.getBoolean("buttonAllCaps", true)
        PREF_DUMP_SHADERS = DEFAULT_PREF!!.getBoolean("dump_shaders", false)
        PREF_DEADZONE_SCALE = DEFAULT_PREF!!.getInt("gamepad_deadzone_scale", 100) / 100f
        PREF_BIG_CORE_AFFINITY = DEFAULT_PREF!!.getBoolean("bigCoreAffinity", false)
        PREF_ZINK_PREFER_SYSTEM_DRIVER = DEFAULT_PREF!!.getBoolean("zinkPreferSystemDriver", false)
        PREF_DOWNLOAD_SOURCE = DEFAULT_PREF!!.getString("downloadSource", "default")!!
        PREF_VERIFY_MANIFEST = DEFAULT_PREF!!.getBoolean("verifyManifest", true)
        PREF_SKIP_NOTIFICATION_PERMISSION_CHECK = DEFAULT_PREF!!.getBoolean(PREF_KEY_SKIP_NOTIFICATION_CHECK, false)
        PREF_VSYNC_IN_ZINK = DEFAULT_PREF!!.getBoolean("vsync_in_zink", true)
        PREF_VERIFY_FILES = DEFAULT_PREF!!.getBoolean("checkGameFiles", true)
        PREF_RAPID_START = DEFAULT_PREF!!.getBoolean("fastStartupCheck", true)
        PREF_FREEDRENO_SYSMEM = DEFAULT_PREF!!.getBoolean("freedrenoSysmem", false)
        PREF_KEYBOARD_AUTOPANNING = DEFAULT_PREF!!.getBoolean("keyboardAutoPanning", true)
        PREF_ZINK_FORCE_LEGACY = DEFAULT_PREF!!.getBoolean("zinkForceLegacy", false)

        val argLwjglLibname = "-Dorg.lwjgl.opengl.libname="
        for (arg in JREUtils.parseJavaArguments(PREF_CUSTOM_JAVA_ARGS!!)) {
            if (arg.startsWith(argLwjglLibname)) {
                DEFAULT_PREF!!.edit().putString("javaArgs",
                    PREF_CUSTOM_JAVA_ARGS!!.replace(arg, "")).apply()
            }
        }

        if (DEFAULT_PREF!!.contains("defaultRuntime")) {
            PREF_DEFAULT_RUNTIME = DEFAULT_PREF!!.getString("defaultRuntime", "")
        } else {
            if (MultiRTUtils.getRuntimes().isEmpty()) {
                PREF_DEFAULT_RUNTIME = ""
                return
            }
            PREF_DEFAULT_RUNTIME = MultiRTUtils.getRuntimes()[0].name
            LauncherPreferences.DEFAULT_PREF!!.edit().putString("defaultRuntime", LauncherPreferences.PREF_DEFAULT_RUNTIME).apply()
        }
    }

    private fun findBestRAMAllocation(ctx: Context): Int {
        val deviceRam = Tools.getTotalDeviceMemory(ctx)
        if (deviceRam < 1024) return 296
        if (deviceRam < 1536) return 448
        if (deviceRam < 2048) return 656
        if (Architecture.is32BitsDevice()) return 696
        if (deviceRam < 3064) return 936
        if (deviceRam < 4096) return 1144
        if (deviceRam < 6144) return 1536
        return 2048
    }

    private fun findBestResolution(context: Context, isDevicePowerful: Boolean): Int {
        val metrics = context.resources.displayMetrics
        val minSide = Math.min(metrics.widthPixels, metrics.heightPixels)
        val targetSide = if (isDevicePowerful) 1080 else 720
        if (minSide <= targetSide) return 100
        val ratio = (100f * targetSide / minSide)
        val increment = context.resources.getInteger(R.integer.resolution_seekbar_increment)
        return (Math.ceil((ratio / increment).toDouble()) * increment).toInt()
    }

    private fun isDevicePowerful(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        if (Tools.getTotalDeviceMemory(context) <= 4096) return false
        val metrics = context.resources.displayMetrics
        if (Math.min(metrics.widthPixels, metrics.heightPixels) < 1080) return false
        if (java.lang.Runtime.getRuntime().availableProcessors() <= 4) return false
        if (hasAllCoreSameFreq()) return false
        return true
    }

    private fun hasAllCoreSameFreq(): Boolean {
        val coreCount = java.lang.Runtime.getRuntime().availableProcessors()
        try {
            val freq0 = Tools.read("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            val freqX = Tools.read("/sys/devices/system/cpu/cpu${coreCount - 1}/cpufreq/cpuinfo_max_freq")
            if (freq0 == freqX) return true
        } catch (e: IOException) {
            Log.e("LauncherPreferences", "Failed to read CPU frequencies", e)
        }
        return false
    }

    fun hasNotch(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return try {
            val cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity.windowManager.currentWindowMetrics.windowInsets.displayCutout!!.boundingRects[0]
            } else {
                activity.window.decorView.rootWindowInsets.displayCutout!!.boundingRects[0]
            }
            cutout.width() != 0 || cutout.height() != 0
        } catch (e: Exception) {
            Log.i("NOTCH DETECTION", "No notch detected, or the device if in split screen mode")
            false
        }
    }
}
