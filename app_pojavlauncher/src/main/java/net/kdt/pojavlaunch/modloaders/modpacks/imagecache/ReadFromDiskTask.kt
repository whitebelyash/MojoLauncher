package net.kdt.pojavlaunch.modloaders.modpacks.imagecache

import android.graphics.BitmapFactory
import net.kdt.pojavlaunch.Tools
import java.io.File

class ReadFromDiskTask(
    val iconCache: ModIconCache,
    private val imageReceiver: ImageReceiver,
    cacheTag: String,
    val imageUrl: String
) : Runnable {
    val cacheFile: File = File(iconCache.cachePath, "$cacheTag.ca")

    fun runDownloadTask() {
        iconCache.cacheLoaderPool.execute(DownloadImageTask(this))
    }

    override fun run() {
        if (cacheFile.isDirectory) return
        if (cacheFile.canRead()) {
            IconCacheJanitor.waitForJanitorToFinish()
            val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (bitmap != null) {
                Tools.runOnUiThread {
                    if (taskCancelled()) {
                        bitmap.recycle()
                        return@runOnUiThread
                    }
                    imageReceiver.onImageAvailable(bitmap)
                }
                return
            }
        }
        if (iconCache.cachePath.canWrite() && !taskCancelled()) {
            runDownloadTask()
        }
    }

    fun taskCancelled(): Boolean = iconCache.checkCancelled(imageReceiver)
}
