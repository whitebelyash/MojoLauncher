package net.kdt.pojavlaunch.downloader

import android.util.Log
import net.kdt.pojavlaunch.mirrors.DownloadMirror
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import java.io.IOException
import java.net.URL

class CompleteMetadataTask(
    mMetadata: TaskMetadata,
    mHostDownloader: Downloader
) : DownloaderTask(mMetadata, mHostDownloader) {
    @Throws(IOException::class)
    override fun performTask() {
        if (mMetadata is AcquireableTaskMetadata) {
            (mMetadata as AcquireableTaskMetadata).acquireMetadata()
            if (mMetadata.url == null) throw IOException("Metadata acquisition did not supply the URL!")
        }
        if (mMetadata.url != null) {
            getFileSize()
            getLibrarySha1Hash()
        }
        if (mMetadata.size == -1L) {
            mDownloader.disableSizeCounter()
        }
        mDownloader.fileComplete()
    }

    private fun getLibrarySha1Hash() {
        if (mMetadata.sha1Hash != null) return
        if (mMetadata.mirrorType != DownloadMirror.DOWNLOAD_CLASS_LIBRARIES) return
        if (!LauncherPreferences.PREF_VERIFY_FILES) return
        if (LauncherPreferences.PREF_RAPID_START && mMetadata.size != -1L && mMetadata.path.length() == mMetadata.size) return
        try {
            mMetadata.sha1Hash = mDownloader.downloadString(URL("${mMetadata.url}.sha1"))
        } catch (e: IOException) {
            Log.i("CompleteMetadataTask", "Failed to get server hash for " + mMetadata.path.name, e)
        }
    }

    private fun getFileSize() {
        if (mMetadata.size != -1L) return
        try {
            mMetadata.size = mDownloader.getFileContentLength(mMetadata.url)
            Log.i("CompleteMetadataTask", "Got size: " + mMetadata.size + " for " + mMetadata.path.name)
        } catch (e: IOException) {
            Log.i("CompleteMetadataTask", "Failed to get size for " + mMetadata.path.name, e)
        }
    }

    companion object {
        fun shouldCompleteMetadata(metadata: TaskMetadata): Boolean {
            return metadata is AcquireableTaskMetadata ||
                    (metadata.sha1Hash == null && metadata.mirrorType == DownloadMirror.DOWNLOAD_CLASS_LIBRARIES) ||
                    metadata.size == -1L
        }
    }
}
