package net.kdt.pojavlaunch.utils

import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipFile

object ZipUtils {
    @Throws(IOException::class)
    fun getEntryStream(zipFile: ZipFile, entryPath: String): InputStream {
        val entry = zipFile.getEntry(entryPath)
            ?: throw IOException("No entry in ZIP file: $entryPath")
        return zipFile.getInputStream(entry)
    }

    @Throws(IOException::class)
    fun zipExtract(zipFile: ZipFile, dirName: String, destination: File) {
        val zipEntries = zipFile.entries()
        val dirNameLen = dirName.length
        while (zipEntries.hasMoreElements()) {
            val zipEntry = zipEntries.nextElement()
            val entryName = zipEntry.name
            if (!entryName.startsWith(dirName) || zipEntry.isDirectory) continue
            val zipDestination = File(destination, entryName.substring(dirNameLen))
            FileUtils.ensureParentDirectory(zipDestination)
            zipFile.getInputStream(zipEntry).use { inputStream ->
                FileOutputStream(zipDestination).use { outputStream ->
                    IOUtils.copy(inputStream, outputStream)
                }
            }
        }
    }
}
