package net.kdt.pojavlaunch.customcontrols

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.NonNull
import org.apache.commons.io.IOUtils
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.HashMap
import java.util.Random
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LayoutBitmaps private constructor() {
    private val mBitmaps = HashMap<String, Bitmap>()

    private fun pickKey(): String {
        var key: String
        do {
            key = mKeyPicker.nextInt().toString()
        } while (mBitmaps.containsKey(key))
        return key
    }

    fun getBitmap(key: String): Bitmap? = mBitmaps[key]

    fun putBitmap(bitmap: Bitmap?, oldKey: String): String {
        val newKey = pickKey()
        mBitmaps.remove(oldKey)
        if (bitmap != null) mBitmaps[newKey] = bitmap
        return newKey
    }

    companion object {
        private val mKeyPicker = Random(System.nanoTime())

        fun createEmpty(): LayoutBitmaps = LayoutBitmaps()

        private fun createEmpty(controlsJson: String): ControlsContainer =
            ControlsContainer(controlsJson, LayoutBitmaps())

        private fun loadFromZip(zipIn: ZipInputStream): ControlsContainer {
            val layoutBitmaps = LayoutBitmaps()
            var layoutContent: String? = null
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryName = entry.name
                    if (entryName == "layout.json") {
                        layoutContent = IOUtils.toString(zipIn, StandardCharsets.UTF_8)
                    } else {
                        layoutBitmaps.mBitmaps[entryName] = BitmapFactory.decodeStream(zipIn)
                        zipIn.closeEntry()
                    }
                }
                entry = zipIn.nextEntry
            }
            if (layoutContent == null) throw ZipException("Incorrect ZIP file structure")
            return ControlsContainer(layoutContent, layoutBitmaps)
        }

        @Throws(IOException::class)
        private fun load(fileInputStream: FileInputStream, fileSize: Long): ControlsContainer {
            BufferedInputStream(fileInputStream).use { bufferedIn ->
                var isZip: Boolean
                bufferedIn.mark(4096)
                try {
                    val zipIn = ZipInputStream(bufferedIn)
                    isZip = zipIn.nextEntry != null
                } catch (e: ZipException) {
                    isZip = false
                } catch (e: EOFException) {
                    isZip = false
                } catch (e: Exception) {
                    isZip = false
                }
                bufferedIn.reset()
                return if (isZip) {
                    ZipInputStream(bufferedIn).use { zipIn -> loadFromZip(zipIn) }
                } else {
                    val meg = 1024L * 1024L
                    if (fileSize > 25L * meg) throw IOException("Raw JSON control data size too large")
                    createEmpty(IOUtils.toString(bufferedIn, StandardCharsets.UTF_8))
                }
            }
        }

        @Throws(IOException::class)
        private fun storeZip(fileOutputStream: FileOutputStream, controlsContainer: ControlsContainer) {
            val bitmaps = controlsContainer.mLayoutZip
            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                zipOutputStream.putNextEntry(ZipEntry("layout.json"))
                IOUtils.write(controlsContainer.mControlsJson, zipOutputStream, StandardCharsets.UTF_8)
                zipOutputStream.closeEntry()
                for ((key, outBitmap) in bitmaps.mBitmaps) {
                    if (outBitmap == null) continue
                    zipOutputStream.putNextEntry(ZipEntry(key))
                    outBitmap.compress(Bitmap.CompressFormat.WEBP, 100, zipOutputStream)
                    zipOutputStream.closeEntry()
                }
            }
        }

        @Throws(IOException::class)
        fun store(fileOutputStream: FileOutputStream, controlsContainer: ControlsContainer) {
            val bitmaps = controlsContainer.mLayoutZip
            val controlsContent = controlsContainer.mControlsJson
            if (bitmaps.mBitmaps.isEmpty()) {
                IOUtils.write(controlsContent, fileOutputStream, StandardCharsets.UTF_8)
                return
            }
            storeZip(fileOutputStream, controlsContainer)
        }

        @Throws(IOException::class)
        @NonNull
        fun load(jsonLocation: File): ControlsContainer {
            FileInputStream(jsonLocation).use { fileInputStream ->
                return load(fileInputStream, jsonLocation.length())
            }
        }
    }

    class ControlsContainer(
        val mControlsJson: String,
        val mLayoutZip: LayoutBitmaps
    )
}
