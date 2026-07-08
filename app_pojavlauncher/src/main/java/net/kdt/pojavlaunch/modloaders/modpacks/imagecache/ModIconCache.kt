package net.kdt.pojavlaunch.modloaders.modpacks.imagecache

import android.graphics.BitmapFactory
import android.util.Log
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.utils.FileUtils
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Iterator
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ModIconCache {
    val cacheLoaderPool = ThreadPoolExecutor(
        10, 10,
        1000, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue()
    )
    private val mCancelledReceivers = ArrayList<WeakReference<ImageReceiver>>()
    var cachePath: File

    init {
        cachePath = getImageCachePath()
        if (!FileUtils.ensureDirectorySilently(cachePath)) {
            throw RuntimeException("Failed to create icon cache directory")
        }
    }

    fun getImage(imageReceiver: ImageReceiver, imageTag: String, imageUrl: String) {
        cacheLoaderPool.execute(ReadFromDiskTask(this, imageReceiver, imageTag, imageUrl))
    }

    fun cancelImage(imageReceiver: ImageReceiver) {
        synchronized(mCancelledReceivers) {
            mCancelledReceivers.add(WeakReference(imageReceiver))
        }
    }

    fun checkCancelled(imageReceiver: ImageReceiver): Boolean {
        var isCanceled = false
        synchronized(mCancelledReceivers) {
            val iterator = mCancelledReceivers.iterator()
            while (iterator.hasNext()) {
                val reference = iterator.next()
                if (reference.get() == null) {
                    iterator.remove()
                    continue
                }
                if (reference.get() === imageReceiver) isCanceled = true
            }
        }
        if (isCanceled) Log.i("IconCache", "checkCancelled(${imageReceiver.hashCode()}) == true")
        return isCanceled
    }

    companion object {
        @JvmStatic
        fun getImageCachePath(): File = File(Tools.DIR_CACHE, "mod_icons")

        @JvmStatic
        fun writeInstanceImage(instance: Instance, imageTag: String?) {
            val imagePath = File(Tools.DIR_CACHE, "mod_icons/$imageTag.ca")
            Log.i("IconCache", "Creating base64 version of icon $imageTag")
            if (!imagePath.canRead() || !imagePath.isFile) {
                Log.i("IconCache", "Icon does not exist")
                return
            }
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath.absolutePath) ?: return
                instance.encodeNewIcon(bitmap)
            } catch (e: IOException) {
                Log.i("ModIconCache", "Failed to reencode icon for instance")
                e.printStackTrace()
            }
        }
    }
}
