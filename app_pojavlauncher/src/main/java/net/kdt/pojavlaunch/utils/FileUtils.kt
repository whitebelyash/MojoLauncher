package net.kdt.pojavlaunch.utils

import java.io.File
import java.io.IOException

object FileUtils {
    fun exists(filePath: String): Boolean = File(filePath).exists()

    fun getFileName(pathOrUrl: String): String {
        val lastSlashIndex = pathOrUrl.lastIndexOf('/')
        return if (lastSlashIndex == -1) pathOrUrl else pathOrUrl.substring(lastSlashIndex)
    }

    fun removeExtension(pathOrUrl: String): String {
        val lastDotIndex = pathOrUrl.lastIndexOf('.')
        return if (lastDotIndex == -1) pathOrUrl else pathOrUrl.substring(0, lastDotIndex)
    }

    fun ensureDirectorySilently(targetFile: File): Boolean {
        if (targetFile.isFile) return false
        return if (targetFile.exists()) targetFile.canWrite() else targetFile.mkdirs()
    }

    fun ensureParentDirectorySilently(targetFile: File): Boolean {
        val parentFile = targetFile.parentFile ?: return false
        return ensureDirectorySilently(parentFile)
    }

    @Throws(IOException::class)
    fun ensureDirectory(targetFile: File) {
        if (targetFile.isFile) throw IOException("Target directory is a file")
        if (targetFile.exists()) {
            if (!targetFile.canWrite()) throw IOException("Target directory is not writable")
        } else if (!targetFile.mkdirs()) {
            if (!targetFile.isDirectory) throw IOException("Unable to create target directory")
        }
    }

    @Throws(IOException::class)
    fun ensureParentDirectory(targetFile: File) {
        val parentFile = targetFile.parentFile
            ?: throw IOException("targetFile does not have a parent")
        ensureDirectory(parentFile)
    }
}
