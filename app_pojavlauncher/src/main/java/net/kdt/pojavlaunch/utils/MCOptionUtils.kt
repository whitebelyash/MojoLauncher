package net.kdt.pojavlaunch.utils

import android.os.Build
import android.os.FileObserver
import android.util.Log
import androidx.annotation.NonNull
import net.kdt.pojavlaunch.CallbackBridge
import net.kdt.pojavlaunch.Tools
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*

object MCOptionUtils {
    private val sParameterMap = HashMap<String, String>()
    private val sOptionListeners = ArrayList<WeakReference<MCOptionListener>>()
    private var sFileObserver: FileObserver? = null
    private var sOptionFolderPath: String? = null

    interface MCOptionListener {
        fun onOptionChanged()
    }

    fun load() {
        load(sOptionFolderPath ?: Tools.DIR_GAME_NEW)
    }

    fun load(@NonNull folderPath: String) {
        val optionFile = File("$folderPath/options.txt")
        if (!optionFile.exists()) {
            try {
                optionFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        if (sFileObserver == null || sOptionFolderPath != folderPath) {
            sOptionFolderPath = folderPath
            setupFileObserver()
        }
        sOptionFolderPath = folderPath

        sParameterMap.clear()

        try {
            val reader = BufferedReader(FileReader(optionFile))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val firstColonIndex = line!!.indexOf(':')
                if (firstColonIndex < 0) {
                    Log.w(Tools.APP_NAME, "No colon on line \"$line\", skipping")
                    continue
                }
                sParameterMap[line!!.substring(0, firstColonIndex)] = line!!.substring(firstColonIndex + 1)
            }
            reader.close()
        } catch (e: IOException) {
            Log.w(Tools.APP_NAME, "Could not load options.txt", e)
        }
    }

    fun set(key: String, value: String) {
        sParameterMap[key] = value
    }

    fun set(key: String, values: List<String>) {
        sParameterMap[key] = values.toString()
    }

    fun get(key: String): String? = sParameterMap[key]

    fun getAsList(key: String): List<String> {
        var value = get(key) ?: return ArrayList()
        value = value.replace("[", "").replace("]", "")
        return if (value.isEmpty()) ArrayList() else Arrays.asList(*value.split(",").toTypedArray())
    }

    fun save() {
        val result = StringBuilder()
        for (key in sParameterMap.keys) {
            result.append(key)
                .append(':')
                .append(sParameterMap[key])
                .append('\n')
        }

        try {
            sFileObserver?.stopWatching()
            Tools.write("$sOptionFolderPath/options.txt", result.toString())
            sFileObserver?.startWatching()
        } catch (e: IOException) {
            Log.w(Tools.APP_NAME, "Could not save options.txt", e)
        }
    }

    fun getMcScale(): Int {
        val str = MCOptionUtils.get("guiScale")
        var guiScale = if (str == null) 0 else Integer.parseInt(str)
        val scale = Math.max(Math.min(CallbackBridge.windowWidth / 320, CallbackBridge.windowHeight / 240), 1)
        if (scale < guiScale || guiScale == 0) {
            guiScale = scale
        }
        return guiScale
    }

    private fun setupFileObserver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sFileObserver = object : FileObserver(File("$sOptionFolderPath/options.txt"), FileObserver.MODIFY) {
                override fun onEvent(i: Int, s: String?) {
                    MCOptionUtils.load()
                    notifyListeners()
                }
            }
        } else {
            sFileObserver = object : FileObserver("$sOptionFolderPath/options.txt", FileObserver.MODIFY) {
                override fun onEvent(i: Int, s: String?) {
                    MCOptionUtils.load()
                    notifyListeners()
                }
            }
        }
        sFileObserver?.startWatching()
    }

    fun notifyListeners() {
        for (weakReference in sOptionListeners) {
            val optionListener = weakReference.get() ?: continue
            optionListener.onOptionChanged()
        }
    }

    fun addMCOptionListener(listener: MCOptionListener) {
        sOptionListeners.add(WeakReference(listener))
    }

    fun removeMCOptionListener(listener: MCOptionListener) {
        val iterator = sOptionListeners.iterator()
        while (iterator.hasNext()) {
            val weakReference = iterator.next()
            val optionListener = weakReference.get() ?: continue
            if (optionListener == listener) {
                iterator.remove()
                return
            }
        }
    }
}
