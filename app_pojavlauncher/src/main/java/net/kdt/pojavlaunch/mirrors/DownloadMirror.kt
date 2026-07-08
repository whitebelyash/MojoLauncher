package net.kdt.pojavlaunch.mirrors

import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.File
import java.io.IOException
import java.net.MalformedURLException

object DownloadMirror {
    const val DOWNLOAD_CLASS_NONE = -1
    const val DOWNLOAD_CLASS_LIBRARIES = 0
    const val DOWNLOAD_CLASS_METADATA = 1
    const val DOWNLOAD_CLASS_ASSETS = 2

    private const val URL_PROTOCOL_TAIL = "://"
    private val MIRROR_BMCLAPI = arrayOf(
        "https://bmclapi2.bangbang93.com/maven",
        "https://bmclapi2.bangbang93.com",
        "https://bmclapi2.bangbang93.com/assets"
    )

    @Throws(IOException::class)
    fun downloadFileMirrored(downloadClass: Int, urlInput: String, outputFile: File) {
        DownloadUtils.downloadFile(getMirrorMapping(downloadClass, urlInput), outputFile)
    }

    fun isMirrored(): Boolean {
        return LauncherPreferences.PREF_DOWNLOAD_SOURCE != "default"
    }

    private fun getMirrorSettings(): Array<String>? {
        return when (LauncherPreferences.PREF_DOWNLOAD_SOURCE) {
            "bmclapi" -> MIRROR_BMCLAPI
            else -> null
        }
    }

    @Throws(MalformedURLException::class)
    fun getMirrorMapping(downloadClass: Int, mojangUrl: String): String {
        if (downloadClass == DOWNLOAD_CLASS_NONE) return mojangUrl
        val mirrorSettings = getMirrorSettings() ?: return mojangUrl
        val urlTail = getBaseUrlTail(mojangUrl)
        val baseUrl = mojangUrl.substring(0, urlTail)
        val path = mojangUrl.substring(urlTail)
        var newBase = baseUrl
        when (downloadClass) {
            DOWNLOAD_CLASS_ASSETS, DOWNLOAD_CLASS_METADATA -> {
                newBase = mirrorSettings[downloadClass]
            }

            DOWNLOAD_CLASS_LIBRARIES -> {
                if (baseUrl.endsWith("libraries.minecraft.net")) {
                    newBase = mirrorSettings[downloadClass]
                }
            }
        }
        return newBase + path
    }

    @Throws(MalformedURLException::class)
    private fun getBaseUrlTail(wholeUrl: String): Int {
        var protocolNameEnd = wholeUrl.indexOf(URL_PROTOCOL_TAIL)
        if (protocolNameEnd == -1) throw MalformedURLException("No protocol, or non path-based URL")
        protocolNameEnd += URL_PROTOCOL_TAIL.length
        val hostnameEnd = wholeUrl.indexOf('/', protocolNameEnd)
        if (protocolNameEnd >= wholeUrl.length || hostnameEnd == protocolNameEnd)
            throw MalformedURLException("No hostname")
        return if (hostnameEnd == -1) wholeUrl.length else hostnameEnd
    }
}
