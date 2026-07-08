package net.kdt.pojavlaunch.modloaders.modpacks.imagecache

import android.graphics.Bitmap

fun interface ImageReceiver {
    fun onImageAvailable(image: Bitmap)
}
