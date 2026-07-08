package net.kdt.pojavlaunch.downloader

import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.HashUtils
import java.io.File
import java.io.IOException

class CheckFileOnDiskTask(
    mMetadata: TaskMetadata,
    mHostDownloader: Downloader,
    private val mAfterDownload: Boolean = false
) : DownloaderTask(mMetadata, mHostDownloader) {
    @Throws(IOException::class)
    override fun performTask() {
        val checkResult = checkFile()
        if (checkResult) {
            if (!mAfterDownload) mDownloader.addSize(mMetadata.size)
            mDownloader.fileComplete()
        } else {
            if (!mAfterDownload) mDownloader.submitFileForDownload(mMetadata)
            else throw IOException("Failed to verify " + mMetadata.toString())
        }
    }

    @Throws(IOException::class)
    private fun checkFile(): Boolean {
        val localFile = mMetadata.path
        if (!localFile.exists()) return false
        if (!LauncherPreferences.PREF_VERIFY_FILES) return true
        if (mMetadata.size != -1L) {
            if (mMetadata.size != localFile.length()) return false
            if (LauncherPreferences.PREF_RAPID_START && !mAfterDownload) return true
        }
        return mMetadata.sha1Hash == null || HashUtils.compareSHA1(localFile, mMetadata.sha1Hash)
    }
}
