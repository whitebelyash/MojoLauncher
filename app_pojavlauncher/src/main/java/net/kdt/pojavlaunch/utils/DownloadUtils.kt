package net.kdt.pojavlaunch.utils

import android.util.Log
import net.kdt.pojavlaunch.Tools
import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable

@Suppress("IOStreamConstructor")
object DownloadUtils {
    val USER_AGENT: String = Tools.APP_NAME

    @Throws(IOException::class)
    fun download(url: String, os: OutputStream) {
        download(URL(url), os)
    }

    @Throws(IOException::class)
    fun download(url: URL, os: OutputStream) {
        var `is`: InputStream? = null
        try {
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 10000
            conn.doInput = true
            conn.connect()
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP " + conn.responseCode
                        + ": " + conn.responseMessage)
            }
            `is` = conn.inputStream
            IOUtils.copy(`is`, os)
        } catch (e: IOException) {
            throw IOException("Unable to download from $url", e)
        } finally {
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(IOException::class)
    fun downloadString(url: String): String {
        val bos = ByteArrayOutputStream()
        download(url, bos)
        bos.close()
        return String(bos.toByteArray(), StandardCharsets.UTF_8)
    }

    @Throws(IOException::class)
    fun downloadFile(url: String, out: File) {
        FileUtils.ensureParentDirectory(out)
        FileOutputStream(out).use { fileOutputStream ->
            download(url, fileOutputStream)
        }
    }

    @Throws(IOException::class)
    fun downloadFileMonitored(urlInput: String, outputFile: File, buffer: ByteArray?,
                              monitor: Tools.DownloaderFeedback) {
        FileUtils.ensureParentDirectory(outputFile)

        val conn = URL(urlInput).openConnection() as HttpURLConnection
        val readStr: InputStream = conn.inputStream
        FileOutputStream(outputFile).use { fos ->
            var current: Int
            var overall = 0
            val length = conn.contentLength

            var buf = buffer
            if (buf == null) buf = ByteArray(65535)

            while (readStr.read(buf).also { current = it } != -1) {
                overall += current
                fos.write(buf, 0, current)
                monitor.updateProgress(overall, length)
            }
            conn.disconnect()
        }
    }

    @Throws(IOException::class, ParseException::class)
    fun <T> downloadStringCached(url: String, cacheName: String, parseCallback: ParseCallback<T>): T {
        val cacheDestination = File(Tools.DIR_CACHE, "string_cache/$cacheName")
        if (cacheDestination.isFile &&
                cacheDestination.canRead() &&
                System.currentTimeMillis() < (cacheDestination.lastModified() + 86400000)) {
            try {
                val cachedString = Tools.read(cacheDestination.inputStream())
                return parseCallback.process(cachedString)
            } catch (e: IOException) {
                Log.i("DownloadUtils", "Failed to read the cached file", e)
            } catch (e: ParseException) {
                Log.i("DownloadUtils", "Failed to parse the cached file", e)
            }
        }
        val urlContent = DownloadUtils.downloadString(url)
        val parseResult = parseCallback.process(urlContent)

        val tryWriteCache = if (cacheDestination.exists()) {
            cacheDestination.canWrite()
        } else {
            FileUtils.ensureParentDirectorySilently(cacheDestination)
        }

        if (tryWriteCache) try {
            Tools.write(cacheDestination, urlContent)
        } catch (e: IOException) {
            Log.i("DownloadUtils", "Failed to cache the string", e)
        }
        return parseResult
    }

    private fun <T> downloadFile(downloadFunction: Callable<T>): T {
        try {
            return downloadFunction.call()
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun verifyFile(file: File, sha1: String): Boolean {
        return file.exists() && HashUtils.compareSHA1(file, sha1)
    }

    @Throws(IOException::class)
    fun <T> ensureSha1(outputFile: File, sha1: String?, downloadFunction: Callable<T>): T? {
        if (sha1 == null) {
            if (outputFile.exists()) return null
            else return downloadFile(downloadFunction)
        }

        var attempts = 0
        var fileOkay = verifyFile(outputFile, sha1)
        var result: T? = null
        while (attempts < 5 && !fileOkay) {
            attempts++
            downloadFile(downloadFunction)
            fileOkay = verifyFile(outputFile, sha1)
        }
        if (!fileOkay) throw SHA1VerificationException("SHA1 verifcation failed after 5 download attempts")
        return result
    }

    fun getContentLength(url: String): Long {
        return try {
            val urlConnection = URL(url).openConnection() as HttpURLConnection
            urlConnection.requestMethod = "HEAD"
            urlConnection.doInput = false
            urlConnection.doOutput = false
            urlConnection.connect()
            val responseCode = urlConnection.responseCode
            if (responseCode in 200..299) urlConnection.contentLength.toLong() else -1
        } catch (e: IOException) {
            Log.w("DownloadUtils", "Failed to get content length", e)
            -1
        }
    }

    interface ParseCallback<T> {
        @Throws(ParseException::class)
        fun process(input: String): T
    }

    class ParseException(e: Exception) : Exception(e)

    class SHA1VerificationException(message: String) : IOException(message)
}
