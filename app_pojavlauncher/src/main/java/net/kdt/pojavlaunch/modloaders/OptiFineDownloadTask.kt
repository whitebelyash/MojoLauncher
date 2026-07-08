package net.kdt.pojavlaunch.modloaders

import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.tasks.MoJsonExtras
import net.kdt.pojavlaunch.tasks.MoJsonDownloader
import java.io.File
import java.util.regex.Pattern

class OptiFineDownloadTask(private val mOptiFineVersion: OptiFineUtils.OptiFineVersion) : MoJsonExtras.DoneListener {
    private val mDownloadLock = Any()
    private var mDownloaderThrowable: Throwable? = null

    @Throws(Exception::class)
    fun prepareForInstall() {
        val gameVersion = determineGameVersion()
        if (gameVersion == null) return
        if (!downloadGame(gameVersion)) {
            throw (mDownloaderThrowable as? Exception) ?: Exception(mDownloaderThrowable)
        }
    }

    fun determineGameVersion(): String? {
        val matcher = sGameVersionPattern.matcher(mOptiFineVersion.gameVersion)
        if (matcher.find()) {
            val mcVersionBuilder = StringBuilder()
            mcVersionBuilder.append(matcher.group(1))
            mcVersionBuilder.append('.')
            mcVersionBuilder.append(matcher.group(2))
            val thirdGroup = matcher.group(3)
            if (thirdGroup != null && thirdGroup.isNotEmpty() && thirdGroup != "0") {
                mcVersionBuilder.append('.')
                mcVersionBuilder.append(thirdGroup)
            }
            return mcVersionBuilder.toString()
        }
        return null
    }

    fun downloadGame(gameVersion: String): Boolean {
        val versionMeta = MoJsonExtras.getListedVersion(gameVersion) ?: return false
        return try {
            synchronized(mDownloadLock) {
                MoJsonDownloader().start(null, versionMeta, gameVersion, this)
                mDownloadLock.wait()
            }
            mDownloaderThrowable == null
        } catch (e: InterruptedException) {
            e.printStackTrace()
            false
        }
    }

    override fun onDownloadDone(classpath: Array<File>) {
        synchronized(mDownloadLock) {
            mDownloaderThrowable = null
            mDownloadLock.notifyAll()
        }
    }

    override fun onDownloadFailed(throwable: Throwable) {
        synchronized(mDownloadLock) {
            mDownloaderThrowable = throwable
            mDownloadLock.notifyAll()
        }
    }

    companion object {
        private val sGameVersionPattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.?([0-9]+)?")
    }
}
