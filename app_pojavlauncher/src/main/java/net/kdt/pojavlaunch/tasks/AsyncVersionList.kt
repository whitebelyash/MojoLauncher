package net.kdt.pojavlaunch.tasks

import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.PojavApplication.sExecutorService
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.IOException

object AsyncVersionList {
    private const val MAX_RETRIES = 5

    @Throws(DownloadUtils.ParseException::class)
    private fun parseList(input: String): JVersionList {
        return try {
            Tools.GLOBAL_GSON.fromJson(input, JVersionList::class.java)
        } catch (e: Exception) {
            throw DownloadUtils.ParseException(e)
        }
    }

    private fun getVersionListAsync(versionDoneListener: VersionDoneListener?, retries: Int) {
        try {
            val versionList = DownloadUtils.downloadStringCached(
                LauncherPreferences.PREF_VERSION_REPOS,
                "version_list",
                ::parseList
            )
            versionDoneListener?.onVersionDone(versionList)
        } catch (e: IOException) {
            if (retries < MAX_RETRIES) {
                getVersionListAsync(versionDoneListener, retries + 1)
            } else {
                versionDoneListener?.onVersionDone(null)
                Tools.showErrorRemote(e)
            }
        } catch (e: DownloadUtils.ParseException) {
            if (retries < MAX_RETRIES) {
                getVersionListAsync(versionDoneListener, retries + 1)
            } else {
                versionDoneListener?.onVersionDone(null)
                Tools.showErrorRemote(e)
            }
        }
    }

    fun getVersionList(listener: VersionDoneListener?) {
        sExecutorService.execute { getVersionListAsync(listener, 0) }
    }

    fun interface VersionDoneListener {
        fun onVersionDone(versions: JVersionList?)
    }
}
