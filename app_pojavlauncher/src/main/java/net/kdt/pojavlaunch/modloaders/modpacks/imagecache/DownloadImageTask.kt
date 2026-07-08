package net.kdt.pojavlaunch.modloaders.modpacks.imagecache

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.FileOutputStream
import java.io.IOException

class DownloadImageTask(private val mParentTask: ReadFromDiskTask) : Runnable {
    private var mRetryCount = 0

    override fun run() {
        var wasSuccessful = false
        while (mRetryCount < 5 && !runCatching().also { wasSuccessful = it }) {
            mRetryCount++
        }
        if (wasSuccessful && !mParentTask.taskCancelled()) {
            mParentTask.iconCache.cacheLoaderPool.execute(mParentTask)
        }
    }

    fun runCatching(): Boolean {
        return try {
            IconCacheJanitor.waitForJanitorToFinish()
            DownloadUtils.downloadFile(mParentTask.imageUrl, mParentTask.cacheFile)
            val bitmap = BitmapFactory.decodeFile(mParentTask.cacheFile.absolutePath) ?: return false
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            if (bitmapWidth <= BITMAP_FINAL_DIMENSION && bitmapHeight <= BITMAP_FINAL_DIMENSION) {
                bitmap.recycle()
                return true
            }
            val imageRescaleRatio = minOf(BITMAP_FINAL_DIMENSION / bitmapWidth, BITMAP_FINAL_DIMENSION / bitmapHeight)
            val resizedBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmapWidth * imageRescaleRatio).toInt(),
                (bitmapHeight * imageRescaleRatio).toInt(),
                true
            )
            bitmap.recycle()
            if (resizedBitmap == bitmap) return true
            try {
                FileOutputStream(mParentTask.cacheFile).use { fileOutputStream ->
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream)
                }
            } finally {
                resizedBitmap.recycle()
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        private const val BITMAP_FINAL_DIMENSION = 256f
    }
}
