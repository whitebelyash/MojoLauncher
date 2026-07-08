package net.kdt.pojavlaunch

import android.content.Intent.FLAG_ACTIVITY_NEW_TASK

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Log

import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat

import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.tasks.AsyncAssetManager
import net.kdt.pojavlaunch.tasks.MoJsonDownloader
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.LocaleUtils

import java.io.File
import java.io.PrintStream
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import git.artdeell.mojo.BuildConfig

class PojavApplication : Application() {
    companion object {
        const val CRASH_REPORT_TAG = "PojavCrashReport"
        val sExecutorService: ExecutorService = ThreadPoolExecutor(4, 4, 500, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
    }

    private fun installFatalErrorHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, th ->
            val storagePermAllowed = (Build.VERSION.SDK_INT < 23 || Build.VERSION.SDK_INT >= 29 ||
                    ActivityCompat.checkSelfPermission(this@PojavApplication, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) && Tools.checkStorageRoot(this@PojavApplication)
            val crashFile = File(if (storagePermAllowed) Tools.DIR_GAME_HOME else Tools.DIR_DATA, "latestcrash.txt")
            try {
                FileUtils.ensureParentDirectory(crashFile)
                val crashStream = PrintStream(crashFile)
                crashStream.append("PojavLauncher crash report\n")
                crashStream.append(" - Time: ").append(DateFormat.getDateTimeInstance().format(Date())).append("\n")
                crashStream.append(" - Device: ").append(Build.PRODUCT).append(" ").append(Build.MODEL).append("\n")
                crashStream.append(" - Android version: ").append(Build.VERSION.RELEASE).append("\n")
                crashStream.append(" - Crash stack trace:\n")
                crashStream.append(" - Launcher version: " + BuildConfig.VERSION_NAME + "\n")
                crashStream.append(Log.getStackTraceString(th))
                crashStream.close()
            } catch (throwable: Throwable) {
                Log.e(CRASH_REPORT_TAG, " - Exception attempt saving crash stack trace:", throwable)
                Log.e(CRASH_REPORT_TAG, " - The crash stack trace was:", th)
            }

            FatalErrorActivity.showError(this@PojavApplication, crashFile.absolutePath, storagePermAllowed, th)
            Tools.fullyExit()
        }
    }

    override fun onCreate() {
        ContextExecutor.setApplication(this)
        if (BuildConfig.BUILD_TYPE != "gplay") installFatalErrorHandler()

        try {
            super.onCreate()
            if (Tools.checkStorageRoot(this)) {
                LauncherPreferences.loadPreferences(this)
            } else {
                Tools.initEarlyConstants(this)
            }
            Tools.DEVICE_ARCHITECTURE = Architecture.getDeviceArchitecture()
            if (Architecture.isx86Device() && Architecture.is32BitsDevice()) {
                val originalJNIDirectory = applicationInfo.nativeLibraryDir
                applicationInfo.nativeLibraryDir = originalJNIDirectory.substring(0,
                    originalJNIDirectory.lastIndexOf("/")) + "/x86"
            }
            MoJsonDownloader.prepareSubstitutionMap(assets)
            AsyncAssetManager.unpackRuntime(assets)
        } catch (throwable: Throwable) {
            val ferrorIntent = Intent(this, FatalErrorActivity::class.java)
            ferrorIntent.putExtra("throwable", throwable)
            ferrorIntent.flags = FLAG_ACTIVITY_NEW_TASK
            startActivity(ferrorIntent)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        ContextExecutor.clearApplication()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleUtils.setLocale(base))
    }

    override fun onConfigurationChanged(@NonNull newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleUtils.setLocale(this)
    }
}
