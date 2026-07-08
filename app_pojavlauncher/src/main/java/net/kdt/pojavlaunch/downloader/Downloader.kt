package net.kdt.pojavlaunch.downloader

import com.kdt.mcgui.ProgressLayout
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.tasks.SpeedCalculator
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

open class Downloader(private val mProgressKey: String) {
    private val mThreadException = AtomicReference<IOException>()
    private val mDownloadedFileCounter = AtomicInteger()
    private val mDownloadedSizeCounter = AtomicLong()
    private val mInternetUsageCounter = AtomicLong()
    private val mUseSizeProgress = AtomicBoolean(true)
    private val mSpeedCalculator = SpeedCalculator()
    private var mDownloadService: ExecutorService? = null
    private var mVerifyService: ExecutorService? = null

    @Throws(IOException::class, InterruptedException::class)
    protected fun runDownloads(downloads: ArrayList<out TaskMetadata>) {
        insertMetadata(downloads)
        performDownloads(downloads)
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun performDownloads(metadata: ArrayList<out TaskMetadata>) {
        mThreadException.set(null)
        mDownloadedFileCounter.set(0)
        mDownloadedSizeCounter.set(0)
        mDownloadService = Executors.newFixedThreadPool(3)
        val verifyThreads = maxOf(2, Runtime.getRuntime().availableProcessors() - 2)
        mVerifyService = Executors.newFixedThreadPool(verifyThreads) { r ->
            Thread(r).apply {
                priority = 10
                name = "verify thread"
            }
        }
        var totalSize = 0L
        val totalCount = metadata.size
        val sizeCounter = mUseSizeProgress.get()
        for (element in metadata) {
            totalSize += element.size
            mVerifyService!!.submit(CheckFileOnDiskTask(element, this))
        }
        val totalMegabytes = totalSize / ONE_MEGABYTE
        while (mDownloadedFileCounter.get() < totalCount) {
            val exception = mThreadException.get()
            if (exception != null) throw exception
            if (sizeCounter) reportSizeProgress(totalMegabytes)
            else reportCountProgress(R.string.newerdl_downloading_files_count, totalCount)
            Thread.sleep(33)
        }
        mDownloadService!!.shutdown()
        mVerifyService!!.shutdown()
        if (!mDownloadService!!.awaitTermination(100, TimeUnit.MILLISECONDS) ||
            !mVerifyService!!.awaitTermination(100, TimeUnit.MILLISECONDS)
        ) {
            throw RuntimeException("BUG! The file counter is wrong. Maybe. Send this to artDev.")
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun insertMetadata(metadata: ArrayList<out TaskMetadata>) {
        mThreadException.set(null)
        mDownloadedFileCounter.set(0)
        val reducedList = ArrayList<TaskMetadata>()
        for (element in metadata) {
            if (!CompleteMetadataTask.shouldCompleteMetadata(element)) continue
            reducedList.add(element)
        }
        if (reducedList.isEmpty()) return
        val executorService = Executors.newFixedThreadPool(4)
        try {
            for (element in reducedList) executorService.submit(CompleteMetadataTask(element, this))
            executorService.shutdown()
            while (!executorService.awaitTermination(33, TimeUnit.MILLISECONDS)) {
                val exception = mThreadException.get()
                if (exception != null) throw exception
                reportCountProgress(R.string.newerdl_inserting_metadata_count, reducedList.size)
            }
        } finally {
            executorService.shutdown()
        }
    }

    private fun getSpeed() = mSpeedCalculator.feed(mInternetUsageCounter.get()) / ONE_MEGABYTE

    private fun reportCountProgress(resource: Int, total: Int) {
        val downloadedCount = mDownloadedFileCounter.get()
        val progress = (downloadedCount.toFloat() / total * 100f).toInt()
        ProgressLayout.setProgress(mProgressKey, progress, resource, downloadedCount, total, getSpeed())
    }

    private fun reportSizeProgress(totalMegabytes: Double) {
        val downloadedMegabytes = mDownloadedSizeCounter.get() / ONE_MEGABYTE
        val progress = (downloadedMegabytes / totalMegabytes * 100.0).toInt()
        ProgressLayout.setProgress(mProgressKey, progress, R.string.newerdl_downloading_files_size, downloadedMegabytes, totalMegabytes, getSpeed())
    }

    protected fun taskException(e: IOException) = mThreadException.set(e)
    protected fun disableSizeCounter() = mUseSizeProgress.lazySet(false)

    protected fun submitFileForDownload(taskMetadata: TaskMetadata) {
        mDownloadService!!.submit(DownloadFileTask(taskMetadata, this))
    }

    protected fun submitFileForRecheck(taskMetadata: TaskMetadata) {
        mVerifyService!!.submit(CheckFileOnDiskTask(taskMetadata, this, true))
    }

    protected fun fileComplete() = mDownloadedFileCounter.getAndIncrement()
    protected fun addSize(bytes: Long) = mDownloadedSizeCounter.getAndAdd(bytes)

    private fun copy(inputStream: InputStream, outputStream: OutputStream, listener: BytesCopiedListener?) {
        val buffer = getBuffer()
        var readLen: Int
        while (inputStream.read(buffer).also { readLen = it } != -1) {
            outputStream.write(buffer, 0, readLen)
            listener?.onBytesCopied(readLen)
            mInternetUsageCounter.getAndAdd(readLen.toLong())
        }
    }

    private fun openConnection(url: URL): HttpURLConnection {
        return (url.openConnection() as HttpURLConnection).apply {
            readTimeout = 10000
            setRequestProperty("User-Agent", DownloadUtils.USER_AGENT)
            doInput = true
            doOutput = false
        }
    }

    protected fun downloadToStream(connection: HttpURLConnection, outputStream: OutputStream, listener: BytesCopiedListener?) {
        val inputStream = connection.inputStream
        copy(inputStream, outputStream, listener)
    }

    @Throws(IOException::class)
    protected fun downloadString(url: URL): String {
        val connection = openConnection(url)
        val length = connection.contentLength
        return try {
            ByteArrayOutputStream(if (length < 0) 32 else length).use { outputStream ->
                downloadToStream(connection, outputStream, null)
                String(outputStream.toByteArray(), StandardCharsets.UTF_8)
            }
        } finally {
            connection.disconnect()
        }
    }

    @Throws(IOException::class)
    protected fun downloadFile(file: File, url: URL, listener: BytesCopiedListener?) {
        val connection = openConnection(url)
        try {
            FileOutputStream(file).use { outputStream -> downloadToStream(connection, outputStream, listener) }
        } finally {
            connection.disconnect()
        }
    }

    @Throws(IOException::class)
    protected fun tryContinueDownload(file: File, wantedLength: Long, url: URL, listener: BytesCopiedListener?): Boolean {
        val connection = openConnection(url)
        val range = String.format(Locale.ENGLISH, "bytes %d-%d/%d", file.length(), wantedLength - 1, wantedLength)
        connection.setRequestProperty("Content-Range", range)
        return try {
            connection.connect()
            if (connection.responseCode != 206) return false
            FileOutputStream(file, true).use { outputStream ->
                downloadToStream(connection, outputStream, listener)
                true
            }
        } finally {
            connection.disconnect()
        }
    }

    @Throws(IOException::class)
    protected fun getFileContentLength(url: URL): Long {
        val connection = openConnection(url)
        connection.connectTimeout = 2000
        connection.readTimeout = 2000
        connection.requestMethod = "HEAD"
        connection.connect()
        return if (connection.responseCode >= 400) -1 else connection.contentLength.toLong()
    }

    companion object {
        private const val ONE_MEGABYTE = 1024.0 * 1024.0
        private val sThreadLocalBuffer = ThreadLocal<ByteArray>()

        fun getBuffer(): ByteArray {
            var buffer = sThreadLocalBuffer.get()
            if (buffer == null) {
                buffer = ByteArray(8192)
                sThreadLocalBuffer.set(buffer)
            }
            return buffer
        }
    }
}
