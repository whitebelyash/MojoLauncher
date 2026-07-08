package net.kdt.pojavlaunch.downloader

import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

class DownloadFileTask(
    mMetadata: TaskMetadata,
    mHostDownloader: Downloader
) : DownloaderTask(mMetadata, mHostDownloader), BytesCopiedListener {
    private val mBytesDownloaded = AtomicLong()

    @Throws(IOException::class)
    override fun performTask() {
        tryDownload(0, true)
        mDownloader.submitFileForRecheck(mMetadata)
    }

    @Throws(IOException::class)
    private fun performRetry(attempt: Int, rangeAllowed: Boolean) {
        mDownloader.addSize(-mBytesDownloaded.get())
        tryDownload(attempt + 1, rangeAllowed)
    }

    @Throws(IOException::class)
    private fun tryDownload(attempt: Int, rangeAllowed: Boolean) {
        try {
            if (!mMetadata.path.exists() || !rangeAllowed) {
                mBytesDownloaded.set(0)
                mDownloader.downloadFile(mMetadata.path, mMetadata.url, this)
            } else {
                val alreadyDownloaded = mMetadata.path.length()
                mBytesDownloaded.set(alreadyDownloaded)
                mDownloader.addSize(alreadyDownloaded)
                val rangeOk = mDownloader.tryContinueDownload(mMetadata.path, mMetadata.size, mMetadata.url, this)
                if (!rangeOk) performRetry(attempt, false)
            }
        } catch (e: IOException) {
            if (attempt == 5) throw e
            performRetry(attempt, rangeAllowed)
        }
    }

    override fun onBytesCopied(nbytes: Int) {
        mBytesDownloaded.getAndAdd(nbytes.toLong())
        mDownloader.addSize(nbytes.toLong())
    }
}
