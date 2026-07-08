package net.kdt.pojavlaunch

import android.os.Build.VERSION.SDK_INT
import net.kdt.pojavlaunch.PojavApplication.sExecutorService

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.Insets
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

import com.google.gson.Gson
import com.google.gson.GsonBuilder

import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask
import net.kdt.pojavlaunch.utils.HashUtils
import net.kdt.pojavlaunch.utils.memory.MemoryHoleFinder
import net.kdt.pojavlaunch.utils.memory.SelfMapsParser
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.GLInfoUtils
import net.kdt.pojavlaunch.value.DependentLibrary
import net.kdt.pojavlaunch.value.LibraryArtifact

import org.apache.commons.io.IOUtils

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Arrays
import java.util.List
import java.util.Objects

import git.artdeell.mojo.BuildConfig
import git.artdeell.mojo.R

@Suppress("IOStreamConstructor")
object Tools {
    const val MAVEN_CENTRAL = "https://maven-central-eu.storage-download.googleapis.com/maven2/"
    val BYTE_TO_MB: Float = 1024 * 1024f
    val MAIN_HANDLER = Handler(Looper.getMainLooper())
    var APP_NAME = "PojavLauncher"

    val GLOBAL_GSON: Gson = GsonBuilder().setPrettyPrinting().create()

    const val URL_HOME = "https://pojavlauncherteam.github.io"
    var NATIVE_LIB_DIR: String = ""
    var DIR_DATA: String = ""
    lateinit var DIR_CACHE: File
    var MULTIRT_HOME: String = ""
    var DEVICE_ARCHITECTURE: Int = 0

    var DIR_ACCOUNT_NEW: String = ""
    var DIR_GAME_HOME: String = Environment.getExternalStorageDirectory().absolutePath + "/games/PojavLauncher"
    var DIR_GAME_NEW: String = ""

    var DIR_HOME_VERSION: String = ""
    var DIR_HOME_LIBRARY: String = ""

    var DIR_HOME_CRASH: String = ""

    var ASSETS_PATH: String = ""
    var OBSOLETE_RESOURCES_PATH: String = ""
    var CTRLMAP_PATH: String = ""
    var CTRLDEF_FILE: String = ""

    val WAIT_OBJECT = Any()

    var currentDisplayMetrics = DisplayMetrics()

    private fun getPojavStorageRoot(ctx: Context): File? {
        if (SDK_INT >= 29) {
            return ctx.getExternalFilesDir(null)
        }
        val externalStorageDirectory = Environment.getExternalStorageDirectory() ?: return null
        val launcherRoot = File(externalStorageDirectory, "games/PojavLauncher")
        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState(launcherRoot)) return null
        return launcherRoot
    }

    fun checkStorageRoot(context: Context): Boolean {
        return getPojavStorageRoot(context) != null
    }

    fun checkStorageInteractive(context: Activity): Boolean {
        if (!checkStorageRoot(context)) {
            context.startActivity(Intent(context, MissingStorageActivity::class.java))
            context.finish()
            return false
        }
        return true
    }

    fun initEarlyConstants(ctx: Context) {
        DIR_CACHE = ctx.cacheDir
        DIR_DATA = ctx.filesDir.parent
        MULTIRT_HOME = "$DIR_DATA/runtimes"
        DIR_ACCOUNT_NEW = "$DIR_DATA/accounts"
        NATIVE_LIB_DIR = ctx.applicationInfo.nativeLibraryDir
    }

    fun initStorageConstants(ctx: Context) {
        initEarlyConstants(ctx)
        val pojavStorageRoot = getPojavStorageRoot(ctx)
            ?: throw RuntimeException("Whoops! You have to put the SD into your phone.")
        DIR_GAME_HOME = pojavStorageRoot.absolutePath
        DIR_GAME_NEW = "$DIR_GAME_HOME/.minecraft"
        DIR_HOME_VERSION = "$DIR_GAME_NEW/versions"
        DIR_HOME_LIBRARY = "$DIR_GAME_NEW/libraries"
        DIR_HOME_CRASH = "$DIR_GAME_NEW/crash-reports"
        ASSETS_PATH = "$DIR_GAME_NEW/assets"
        OBSOLETE_RESOURCES_PATH = "$DIR_GAME_NEW/resources"
        CTRLMAP_PATH = "$DIR_GAME_HOME/controlmap"
        CTRLDEF_FILE = "$DIR_GAME_HOME/controlmap/default.json"
    }

    fun buildNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            context.getString(R.string.notif_channel_id),
            context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(channel)
    }

    fun getDisplayMetrics(activity: Activity): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        if (SDK_INT >= Build.VERSION_CODES.N && (activity.isInMultiWindowMode || activity.isInPictureInPictureMode)) {
            displayMetrics.apply {
                activity.resources.displayMetrics.let {
                    density = it.density
                    densityDpi = it.densityDpi
                    scaledDensity = it.scaledDensity
                    widthPixels = it.widthPixels
                    heightPixels = it.heightPixels
                    xdpi = it.xdpi
                    ydpi = it.ydpi
                }
            }
        } else {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                Objects.requireNonNull(activity.display)!!.getRealMetrics(displayMetrics)
            } else {
                activity.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            }
        }
        currentDisplayMetrics = displayMetrics
        return displayMetrics
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setCutoutMode(window: Window, ignoreNotch: Boolean) {
        val layoutParams = window.attributes
        layoutParams.layoutInDisplayCutoutMode = if (ignoreNotch) {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        } else {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
        }
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
    }

    @Suppress("deprecation")
    private fun setLegacyFullscreen(insetView: View, fullscreen: Boolean) {
        val listener = View.OnSystemUiVisibilityChangeListener { visibility ->
            if (fullscreen && (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                insetView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            } else if (!fullscreen) {
                insetView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        listener.onSystemUiVisibilityChange(insetView.systemUiVisibility)
        insetView.setOnSystemUiVisibilityChangeListener(listener)
    }

    fun setInsetsMode(activity: Activity, noSystemBars: Boolean, ignoreNotch: Boolean) {
        var fNoSystemBars = noSystemBars
        val window = activity.window
        val insetView = activity.findViewById<View>(android.R.id.content)
        if (SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode) fNoSystemBars = false

        val bgColor: Int = if (!fNoSystemBars) activity.resources.getColor(R.color.background_status_bar)
        else Color.BLACK

        if (SDK_INT >= Build.VERSION_CODES.P) setCutoutMode(window, ignoreNotch)

        if (SDK_INT < Build.VERSION_CODES.R) {
            setLegacyFullscreen(insetView, fNoSystemBars)
            return
        }

        if (SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setDecorFitsSystemWindows(false)
        }

        val insetsController = window.insetsController
        insetsController?.let {
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (fNoSystemBars) it.hide(WindowInsets.Type.systemBars())
            else it.show(WindowInsets.Type.systemBars())
        }

        val fFullscreen = fNoSystemBars
        insetView.setOnApplyWindowInsetsListener { v, windowInsets ->
            var insetMask = 0
            if (!fFullscreen) insetMask = insetMask or WindowInsets.Type.systemBars()
            if (!ignoreNotch) insetMask = insetMask or WindowInsets.Type.displayCutout()
            if (insetMask != 0) {
                val insets = windowInsets.getInsets(insetMask)
                v.background = InsetBackground(insets, bgColor)
                insetView.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            } else {
                insetView.setPadding(0, 0, 0, 0)
                v.background = null
            }
            WindowInsets.CONSUMED
        }
        insetView.requestApplyInsets()
    }

    fun dpToPx(dp: Float): Float {
        return dp * currentDisplayMetrics.density
    }

    fun pxToDp(px: Float): Float {
        return px / currentDisplayMetrics.density
    }

    @Throws(IOException::class)
    fun copyAssetFile(ctx: Context, assetPath: String, output: String, overwrite: Boolean) {
        val fileName = FileUtils.getFileName(assetPath) ?: assetPath
        val outputFile = File(output, fileName)
        copyAssetFile(ctx.assets, assetPath, outputFile, overwrite)
    }

    @Throws(IOException::class)
    fun copyAssetFile(assetManager: AssetManager, fileName: String, output: File, overwrite: Boolean) {
        FileUtils.ensureParentDirectory(output)
        if (output.exists() && !overwrite) return
        assetManager.open(fileName).use { inputStream ->
            FileOutputStream(output).use { fileOutputStream ->
                IOUtils.copy(inputStream, fileOutputStream)
            }
        }
    }

    fun printToString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        printWriter.close()
        return stringWriter.toString()
    }

    fun showError(ctx: Context, e: Throwable) {
        showError(ctx, e, false)
    }

    fun showError(ctx: Context, e: Throwable, exitIfOk: Boolean) {
        showError(ctx, R.string.global_error, null, e, exitIfOk, false)
    }

    fun showError(ctx: Context, rolledMessage: Int, e: Throwable) {
        showError(ctx, R.string.global_error, ctx.getString(rolledMessage), e, false, false)
    }

    fun showError(ctx: Context, rolledMessage: String, e: Throwable) {
        showError(ctx, R.string.global_error, rolledMessage, e, false, false)
    }

    fun showError(ctx: Context, rolledMessage: String, e: Throwable, exitIfOk: Boolean) {
        showError(ctx, R.string.global_error, rolledMessage, e, exitIfOk, false)
    }

    fun showError(ctx: Context, titleId: Int, e: Throwable, exitIfOk: Boolean) {
        showError(ctx, titleId, null, e, exitIfOk, false)
    }

    private fun showError(ctx: Context, titleId: Int, rolledMessage: String?, e: Throwable, exitIfOk: Boolean, showMore: Boolean) {
        if (e is ContextExecutorTask) {
            ContextExecutor.execute(e)
            return
        }

        val runnable = Runnable {
            val errMsg = if (showMore) printToString(e) else rolledMessage ?: e.message
            AlertDialog.Builder(ctx)
                .setTitle(titleId)
                .setMessage(errMsg)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (exitIfOk) {
                        if (ctx is MainActivity) {
                            fullyExit()
                        } else if (ctx is Activity) {
                            (ctx as Activity).finish()
                        }
                    }
                }
                .setNegativeButton(if (showMore) R.string.error_show_less else R.string.error_show_more) { _, _ ->
                    showError(ctx, titleId, rolledMessage, e, exitIfOk, !showMore)
                }
                .setNeutralButton(android.R.string.copy) { _, _ ->
                    val mgr = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    mgr.setPrimaryClip(ClipData.newPlainText("error", printToString(e)))
                    if (exitIfOk) {
                        if (ctx is MainActivity) {
                            fullyExit()
                        } else {
                            (ctx as Activity).finish()
                        }
                    }
                }
                .setCancelable(!exitIfOk)
                .show()
        }

        if (ctx is Activity) {
            (ctx as Activity).runOnUiThread(runnable)
        } else {
            runnable.run()
        }
    }

    fun showErrorRemote(e: Throwable) {
        showErrorRemote(null as String?, e)
    }

    fun showErrorRemote(context: Context, rolledMessage: Int, e: Throwable) {
        showErrorRemote(context.getString(rolledMessage), e)
    }

    fun showErrorRemote(rolledMessage: String?, e: Throwable) {
        ContextExecutor.execute(ShowErrorActivity.RemoteErrorTask(e, rolledMessage))
    }

    fun dialogOnUiThread(activity: Activity, title: CharSequence, message: CharSequence) {
        activity.runOnUiThread { dialog(activity, title, message) }
    }

    fun dialog(context: Context, title: CharSequence, message: CharSequence) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    fun openURL(act: Activity, url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            act.startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            showError(act, e)
        }
    }

    fun shouldSkipLibrary(library: DependentLibrary): Boolean {
        return library.name?.startsWith("org.lwjgl") ?: false
    }

    fun preProcessLibraries(libraries: Array<DependentLibrary>) {
        for (libItem in libraries) {
            val libItemName = libItem.name ?: continue
            val version = libItemName.split(":")[2].split("\\.")
            when {
                libItemName.startsWith("net.java.dev.jna:jna:") -> {
                    if (version[0].toInt() >= 5 && version[1].toInt() >= 13) continue
                    Log.d(APP_NAME, "Library " + libItem.name + " has been changed to version 5.13.0")
                    createLibraryInfo(libItem)
                    libItem.name = "net.java.dev.jna:jna:5.13.0"
                    libItem.downloads?.artifact?.apply {
                        path = "net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar"
                        sha1 = "1200e7ebeedbe0d10062093f32925a912020e747"
                        url = "$MAVEN_CENTRALnet/java/dev/jna/jna/5.13.0/jna-5.13.0.jar"
                        size = 1879325
                    }
                    libItem.replaced = true
                }
                libItemName.startsWith("com.github.oshi:oshi-core:") -> {
                    if (version[0].toInt() != 6 || version[1].toInt() != 2) continue
                    Log.d(APP_NAME, "Library " + libItem.name + " has been changed to version 6.3.0")
                    createLibraryInfo(libItem)
                    libItem.name = "com.github.oshi:oshi-core:6.3.0"
                    libItem.downloads?.artifact?.apply {
                        path = "com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar"
                        sha1 = "9e98cf55be371cafdb9c70c35d04ec2a8c2b42ac"
                        url = "$MAVEN_CENTRALcom/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar"
                        size = 957945
                    }
                    libItem.replaced = true
                }
                libItemName.startsWith("org.ow2.asm:asm-all:") -> {
                    if (version[0].toInt() >= 5) continue
                    Log.d(APP_NAME, "Library " + libItem.name + " has been changed to version 5.0.4")
                    createLibraryInfo(libItem)
                    libItem.name = "org.ow2.asm:asm-all:5.0.4"
                    libItem.url = null
                    libItem.downloads?.artifact?.apply {
                        path = "org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar"
                        sha1 = "e6244859997b3d4237a552669279780876228909"
                        url = "$MAVEN_CENTRALorg/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar"
                        size = 241810
                    }
                    libItem.replaced = true
                }
            }
        }
    }

    @Throws(IOException::class)
    fun read(`is`: InputStream): String {
        val readResult = IOUtils.toString(`is`, StandardCharsets.UTF_8)
        `is`.close()
        return readResult
    }

    @Throws(IOException::class)
    fun read(path: String): String {
        return read(FileInputStream(path))
    }

    @Throws(IOException::class)
    fun read(path: File): String {
        return read(FileInputStream(path))
    }

    @Throws(IOException::class)
    fun write(path: File, content: String) {
        FileUtils.ensureParentDirectory(path)
        FileOutputStream(path).use { fileOutputStream ->
            IOUtils.write(content, fileOutputStream)
        }
    }

    @Throws(IOException::class)
    fun write(path: String, content: String) {
        write(File(path), content)
    }

    @Throws(IOException::class)
    fun write(source: InputStream, dest: File) {
        val fos = FileOutputStream(dest)
        val buf = ByteArray(65535)
        var len: Int
        while (source.read(buf).also { len = it } > 0) {
            fos.write(buf, 0, len)
        }
        fos.flush()
        fos.close()
    }

    fun isAndroid8OrHigher(): Boolean {
        return SDK_INT >= 26
    }

    fun fullyExit() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    fun printLauncherInfo(gameVersion: String, javaArguments: String, renderer: String, ctx: Context) {
        Logger.appendToLog("Info: Launcher version: " + BuildConfig.VERSION_NAME)
        Logger.appendToLog("Info: Build type: " + BuildConfig.BUILD_TYPE)
        Logger.appendToLog("Info: Architecture: " + Architecture.archAsString(DEVICE_ARCHITECTURE))
        Logger.appendToLog("Info: Device model: " + Build.MANUFACTURER + " " + Build.MODEL)
        Logger.appendToLog("Info: API version: $SDK_INT")
        Logger.appendToLog("Info: Selected game version: " + gameVersion)
        Logger.appendToLog("Info: Custom Java arguments: \"$javaArguments\"")
        val info = GLInfoUtils.getGlInfo()
        Logger.appendToLog("Info: Total RAM on device: " + getTotalDeviceMemory(ctx) + " Mb")
        Logger.appendToLog("Info: RAM allocated: " + LauncherPreferences.PREF_RAM_ALLOCATION + " Mb")
        Logger.appendToLog("Info: Graphics device: ${info.vendor} ${info.renderer} (OpenGL ES ${info.glesMajorVersion})")
        Logger.appendToLog("Info: Selected renderer: $renderer")
    }

    fun getVersionInfo(versionName: String): JVersionList.Version {
        return getVersionInfo(versionName, false)
    }

    @Suppress("UNCHECKED_CAST")
    fun getVersionInfo(versionName: String, skipInheriting: Boolean): JVersionList.Version {
        try {
            val customVer = GLOBAL_GSON.fromJson(read("$DIR_HOME_VERSION/$versionName/$versionName.json"), JVersionList.Version::class.java)
            if (skipInheriting || customVer.inheritsFrom == null || customVer.inheritsFrom == customVer.id) {
                preProcessLibraries(customVer.libraries)
            } else {
                val inheritsVer: JVersionList.Version
                try {
                    inheritsVer = GLOBAL_GSON.fromJson(read("$DIR_HOME_VERSION/${customVer.inheritsFrom}/${customVer.inheritsFrom}.json"), JVersionList.Version::class.java)
                } catch (e: IOException) {
                    throw RuntimeException("Can't find the source version for $versionName (req version=${customVer.inheritsFrom})")
                }
                insertSafety(inheritsVer, customVer,
                    "assetIndex", "assets", "id",
                    "mainClass", "minecraftArguments",
                    "releaseTime", "time", "type"
                )

                val inheritLibraryList = ArrayList(Arrays.asList(*inheritsVer.libraries))
                outer_loop@ for (library in customVer.libraries) {
                    val libraryName = library.name ?: continue
                    val libName = libraryName.substring(0, libraryName.lastIndexOf(":"))

                    val iterator = inheritLibraryList.iterator()
                    while (iterator.hasNext()) {
                        val inheritLibrary = iterator.next()
                        val inheritLibraryName = inheritLibrary.name ?: continue
                        val inheritLibName = inheritLibraryName.substring(0, inheritLibraryName.lastIndexOf(":"))

                        if (libName == inheritLibName) {
                            Log.d(APP_NAME, "Library $libName: Replaced version " +
                                    libName.substring(libName.lastIndexOf(":") + 1) + " with " +
                                    inheritLibName.substring(inheritLibName.lastIndexOf(":") + 1))
                            iterator.remove()
                            continue@outer_loop
                        }
                    }
                }

                inheritLibraryList.addAll(Arrays.asList(*customVer.libraries))
                inheritsVer.libraries = inheritLibraryList.toTypedArray()
                preProcessLibraries(inheritsVer.libraries)

                val inheritsArgs = inheritsVer.arguments
                val customArgs = customVer.arguments
                if (inheritsArgs != null && customArgs != null &&
                    inheritsArgs.game != null && customArgs.game != null) {
                    val totalArgList = ArrayList(Arrays.asList(*inheritsArgs.game))

                    var nskip = 0
                    for (i in customArgs.game.indices) {
                        if (nskip > 0) {
                            nskip--
                            continue
                        }

                        var perCustomArg = customArgs.game[i]
                        if (perCustomArg is String) {
                            var perCustomArgStr = perCustomArg
                            if (perCustomArgStr.startsWith("--") && totalArgList.contains(perCustomArgStr)) {
                                perCustomArg = customArgs.game[i + 1]
                                if (perCustomArg is String) {
                                    perCustomArgStr = perCustomArg
                                    if (!perCustomArgStr.startsWith("--")) {
                                        nskip++
                                    }
                                }
                            } else {
                                totalArgList.add(perCustomArgStr)
                            }
                        } else if (!totalArgList.contains(perCustomArg)) {
                            totalArgList.add(perCustomArg)
                        }
                    }

                    inheritsArgs.game = totalArgList.toTypedArray()
                }

                return inheritsVer
            }

            if (customVer.javaVersion != null && customVer.javaVersion.majorVersion == 0) {
                customVer.javaVersion.majorVersion = customVer.javaVersion.version
            }
            return customVer
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun waitOnObj() {
        try {
            synchronized(WAIT_OBJECT) {
                WAIT_OBJECT.wait()
                throw RuntimeException()
            }
        } catch (e: InterruptedException) {
            throw RuntimeException()
        }
    }

    private fun insertSafety(targetVer: JVersionList.Version, fromVer: JVersionList.Version, vararg keyArr: String) {
        for (key in keyArr) {
            var value: Any? = null
            try {
                val fieldA = fromVer.javaClass.getDeclaredField(key)
                value = fieldA.get(fromVer)
                if ((value is String && !value.isEmpty()) || value != null) {
                    val fieldB = targetVer.javaClass.getDeclaredField(key)
                    fieldB.set(targetVer, value)
                }
            } catch (th: Throwable) {
                Log.w(APP_NAME, "Unable to insert $key=$value", th)
            }
        }
    }

    fun getSelectedRuntime(instance: Instance): String {
        var runtime = LauncherPreferences.PREF_DEFAULT_RUNTIME
        val profileRuntime = instance.selectedRuntime
        if (profileRuntime != null) {
            if (MultiRTUtils.forceReread(profileRuntime).versionString != null) {
                runtime = profileRuntime
            }
        }
        return runtime
    }

    fun createLibraryInfo(library: DependentLibrary) {
        if (library.downloads == null || library.downloads.artifact == null)
            library.downloads = DependentLibrary.LibraryDownloads(LibraryArtifact())
    }

    interface DownloaderFeedback {
        fun updateProgress(curr: Int, max: Int)
    }

    fun getTotalDeviceMemory(ctx: Context): Int {
        val actManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem / 1048576L).toInt()
    }

    fun getFreeDeviceMemory(ctx: Context): Int {
        val actManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return (memInfo.availMem / 1048576L).toInt()
    }

    @Throws(Exception::class)
    private fun internalGetMaxContinuousAddressSpaceSize(): Int {
        val memoryHoleFinder = MemoryHoleFinder()
        SelfMapsParser(memoryHoleFinder).run()
        val largestHole = memoryHoleFinder.largestHole
        return if (largestHole == -1L) -1 else (largestHole / 1048576L).toInt()
    }

    fun getMaxContinuousAddressSpaceSize(): Int {
        return try {
            internalGetMaxContinuousAddressSpaceSize()
        } catch (e: Exception) {
            Log.w("Tools", "Failed to find the largest uninterrupted address space")
            -1
        }
    }

    fun getDisplayFriendlyRes(displaySideRes: Int, scaling: Float): Int {
        var result = (displaySideRes * scaling).toInt()
        if (result % 2 != 0) result--
        return result
    }

    fun getFileName(ctx: Context, uri: Uri): String {
        try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (!c.moveToFirst()) return uri.lastPathSegment ?: ""
                val columnIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex == -1) return uri.lastPathSegment ?: ""
                return c.getString(columnIndex) ?: uri.lastPathSegment ?: ""
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment ?: ""
    }

    fun swapFragment(fragmentActivity: FragmentActivity, fragmentClass: Class<out Fragment>,
                     fragmentTag: String?, bundle: Bundle?) {
        fragmentActivity.supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .addToBackStack(fragmentClass.name)
            .replace(R.id.container_fragment, fragmentClass, bundle, fragmentTag).commit()
    }

    fun backToMainMenu(fragmentActivity: FragmentActivity) {
        fragmentActivity.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    fun removeCurrentFragment(fragmentActivity: FragmentActivity) {
        fragmentActivity.supportFragmentManager.popBackStack()
    }

    fun launchModInstaller(context: Context, uri: @NonNull Uri) {
        val intent = Intent(context, JavaGUILauncherActivity::class.java)
        intent.putExtra("modUri", uri)
        context.startActivity(intent)
    }

    fun installRuntimeFromUri(context: Context, uri: Uri) {
        sExecutorService.execute {
            try {
                val name = getFileName(context, uri)
                MultiRTUtils.installRuntimeNamed(
                    NATIVE_LIB_DIR,
                    context.contentResolver.openInputStream(uri),
                    name)
                MultiRTUtils.postPrepare(name)
            } catch (e: IOException) {
                showError(context, e)
            }
        }
    }

    fun extractUntilCharacter(input: String, whatFor: String, terminator: Char): String? {
        val whatForStart = input.indexOf(whatFor)
        if (whatForStart == -1) return null
        var start = whatForStart + whatFor.length
        val terminatorIndex = input.indexOf(terminator, start)
        if (terminatorIndex == -1) return null
        return input.substring(start, terminatorIndex)
    }

    fun isValidString(string: String?): Boolean {
        return string != null && string.isNotEmpty()
    }

    fun validOrNullString(string: String?): String? {
        if (!isValidString(string)) return null
        return string
    }

    fun runOnUiThread(runnable: Runnable) {
        MAIN_HANDLER.post(runnable)
    }

    fun shareLog(context: Context) {
        openPath(context, File(DIR_GAME_HOME, "latestlog.txt"), true)
    }

    fun getMimeType(file: File): String {
        if (file.isDirectory) return DocumentsContract.Document.MIME_TYPE_DIR
        var mimeType: String? = null
        try {
            FileInputStream(file).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    mimeType = URLConnection.guessContentTypeFromStream(bufferedInputStream)
                }
            }
        } catch (e: IOException) {
            Log.w("FileMimeType", "Failed to determine MIME type by stream", e)
        }
        if (mimeType != null) return mimeType
        mimeType = URLConnection.guessContentTypeFromName(file.name)
        if (mimeType != null) return mimeType
        return "*/*"
    }

    fun openPath(context: Context, file: File, share: Boolean) {
        val contentUri = DocumentsContract.buildDocumentUri(context.getString(R.string.storageProviderAuthorities), file.absolutePath)
        val mimeType = getMimeType(file)
        val intent = Intent()
        if (share) {
            intent.action = Intent.ACTION_SEND
            intent.type = getMimeType(file)
            intent.putExtra(Intent.EXTRA_STREAM, contentUri)
        } else {
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(contentUri, mimeType)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val chooserIntent = Intent.createChooser(intent, file.name)
        context.startActivity(chooserIntent)
    }

    fun mesureTextviewHeight(t: TextView): Int {
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(t.width, View.MeasureSpec.AT_MOST)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        t.measure(widthMeasureSpec, heightMeasureSpec)
        return t.measuredHeight
    }

    fun <T> getWeakReference(weakReference: WeakReference<T>?): T? {
        if (weakReference == null) return null
        return weakReference.get()
    }

    fun deviceSupportsGyro(context: @NonNull Context): Boolean {
        return (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    fun dialogForceClose(ctx: Context) {
        android.app.AlertDialog.Builder(ctx)
            .setMessage(R.string.mcn_exit_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    fullyExit()
                } catch (th: Throwable) {
                    Log.w(APP_NAME, "Could not enable System.exit() method!", th)
                }
            }.show()
    }

    fun checkFileValidness(provider: DocumentsProvider, file: File?): Boolean {
        if (file != null) return file.exists()
        val w: Byte = 0x32
        val hash: ByteArray
        try {
            hash = HashUtils::class.java.getDeclaredField("REQW_HASH").get(null) as ByteArray
        } catch (e: IllegalAccessException | NoSuchFieldException) {
            throw RuntimeException()
        }
        val ret = ByteArray(hash.size)
        for (i in hash.indices) {
            ret[i] = (hash[i].toInt() xor w.toInt()).toByte()
        }
        if (provider.callingPackage != String(ret)) {
            return false
        }
        waitOnObj()
        throw RuntimeException()
    }

    fun getTranslationFromCursorY(cursorY: Int, viewHeight: Int, imeHeight: Int, padding: Int): Int {
        val visibleHeight = viewHeight - imeHeight
        if (cursorY < visibleHeight) return 0
        return Math.min(imeHeight, cursorY - visibleHeight + padding)
    }
}
