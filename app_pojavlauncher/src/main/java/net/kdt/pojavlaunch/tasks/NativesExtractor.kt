package net.kdt.pojavlaunch.tasks

import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.value.ExtractSettings
import java.io.File
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class NativesExtractor(private val mDestinationDir: File) {
    private val mLibraryLocation = "jni/${getAarArchitectureName()}/"

    @Throws(IOException::class)
    private fun extract(source: File, extractFilter: ExtractFilter) {
        val buffer = ByteArray(8192)
        FileInputStream(source).use { fileInputStream ->
            ZipInputStream(fileInputStream).use { zipInputStream ->
                val entryCopyStream = NonCloseableInputStream(zipInputStream)
                var entry: ZipEntry?
                while (zipInputStream.nextEntry.also { entry = it } != null) {
                    val entryName = entry!!.name
                    if (!extractFilter.shouldExtract(entryName) || entry!!.isDirectory) continue
                    var extractedName = FileUtils.getFileName(entryName)
                    if (extractedName == null || LIBRARY_BLACKLIST.contains(extractedName)) continue
                    processEntry(entryCopyStream, entry!!, File(mDestinationDir, extractedName), buffer)
                }
            }
        }
    }

    @Throws(IOException::class)
    fun extractFromAar(source: File) = extract(source) { it.startsWith(mLibraryLocation) }

    @Throws(IOException::class)
    fun extractMoJson(source: File, settings: ExtractSettings) = extract(source) { name ->
        if (settings.exclude == null) return@extract true
        for (exclude in settings.exclude) {
            if (name.startsWith(exclude)) return@extract false
        }
        true
    }

    @Throws(IOException::class)
    private fun processEntry(sourceStream: InputStream, zipEntry: ZipEntry, entryDestination: File, buffer: ByteArray) {
        if (entryDestination.exists()) {
            val expectedSize = zipEntry.size
            val expectedCrc32 = zipEntry.crc
            val realSize = entryDestination.length()
            val realCrc32 = fileCrc32(entryDestination, buffer)
            if (realSize == expectedSize && realCrc32 == expectedCrc32) return
        }
        org.apache.commons.io.FileUtils.copyInputStreamToFile(sourceStream, entryDestination)
    }

    private fun interface ExtractFilter {
        fun shouldExtract(entry: String): Boolean
    }

    private class NonCloseableInputStream(`in`: InputStream) : FilterInputStream(`in`) {
        override fun close() {
            // Do nothing
        }
    }

    companion object {
        private val LIBRARY_BLACKLIST = createLibraryBlacklist()

        private fun createLibraryBlacklist(): ArrayList<String> {
            val includedLibraryNames = File(Tools.NATIVE_LIB_DIR).list()
            val blacklist = ArrayList<String>(includedLibraryNames.size)
            for (libraryName in includedLibraryNames) {
                if (libraryName == "libjnidispatch.so") continue
                blacklist.add(libraryName)
            }
            blacklist.trimToSize()
            return blacklist
        }

        private fun getAarArchitectureName(): String {
            return when (Architecture.getDeviceArchitecture()) {
                Architecture.ARCH_ARM -> "armeabi-v7a"
                Architecture.ARCH_ARM64 -> "arm64-v8a"
                Architecture.ARCH_X86 -> "x86"
                Architecture.ARCH_X86_64 -> "x86_64"
                else -> throw RuntimeException("Unknown CPU architecture: ${Architecture.getDeviceArchitecture()}")
            }
        }

        @Throws(IOException::class)
        private fun fileCrc32(target: File, buffer: ByteArray): Long {
            FileInputStream(target).use { fileInputStream ->
                val crc32 = java.util.zip.CRC32()
                var len: Int
                while (fileInputStream.read(buffer).also { len = it } != -1) {
                    crc32.update(buffer, 0, len)
                }
                return crc32.value
            }
        }
    }
}
