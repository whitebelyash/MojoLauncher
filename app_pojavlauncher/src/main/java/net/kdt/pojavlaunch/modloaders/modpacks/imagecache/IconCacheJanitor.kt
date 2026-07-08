package net.kdt.pojavlaunch.modloaders.modpacks.imagecache

import android.util.Log
import net.kdt.pojavlaunch.PojavApplication
import java.io.File
import java.util.ArrayList
import java.util.Arrays
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

class IconCacheJanitor private constructor() : Runnable {
    override fun run() {
        val modIconCachePath = ModIconCache.getImageCachePath()
        if (!modIconCachePath.isDirectory || !modIconCachePath.canRead()) return
        val modIconFiles = modIconCachePath.listFiles() ?: return
        val writableModIconFiles = ArrayList<File>(modIconFiles.size)
        var directoryFileSize = 0L
        for (modIconFile in modIconFiles) {
            if (!modIconFile.isFile || !modIconFile.canRead()) continue
            directoryFileSize += modIconFile.length()
            if (!modIconFile.canWrite()) continue
            writableModIconFiles.add(modIconFile)
        }
        if (directoryFileSize < CACHE_SIZE_LIMIT) {
            Log.i("IconCacheJanitor", "Skipping cleanup because there's not enough to clean up")
            return
        }
        Arrays.sort(modIconFiles) { x, y -> java.lang.Long.compare(y.lastModified(), x.lastModified()) }
        var filesCleanedUp = 0
        for (modFile in writableModIconFiles) {
            if (directoryFileSize < CACHE_BRINGDOWN) break
            val modFileSize = modFile.length()
            if (modFile.delete()) {
                directoryFileSize -= modFileSize
                filesCleanedUp++
            }
        }
        Log.i("IconCacheJanitor", "Cleaned up $filesCleanedUp files")
        synchronized(IconCacheJanitor::class.java) {
            sJanitorFuture = null
            sJanitorRan = true
        }
    }

    companion object {
        const val CACHE_SIZE_LIMIT: Long = 104857600
        const val CACHE_BRINGDOWN: Long = 52428800
        private var sJanitorFuture: Future<*>? = null
        private var sJanitorRan = false

        fun runJanitor() {
            synchronized(IconCacheJanitor::class.java) {
                if (sJanitorFuture != null || sJanitorRan) return
                sJanitorFuture = PojavApplication.sExecutorService.submit(IconCacheJanitor())
            }
        }

        fun waitForJanitorToFinish() {
            synchronized(IconCacheJanitor::class.java) {
                if (sJanitorFuture == null) return
                try {
                    sJanitorFuture!!.get()
                } catch (e: ExecutionException) {
                    throw RuntimeException("Should not happen!", e)
                } catch (e: InterruptedException) {
                    throw RuntimeException("Should not happen!", e)
                }
            }
        }
    }
}
