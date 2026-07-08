package net.kdt.pojavlaunch.modloaders

import android.util.Log
import androidx.annotation.Keep
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.IOException
import java.util.ArrayList

class BTAUtils {
    companion object {
        private const val BASE_DOWNLOADS_URL = "https://downloads.betterthanadventure.net/bta-client/"
        private const val CLIENT_JAR_URL = BASE_DOWNLOADS_URL + "%s/%s/client.jar"
        private const val ICON_URL = BASE_DOWNLOADS_URL + "%s/%s/auto/%s.png"
        private const val MANIFEST_URL = BASE_DOWNLOADS_URL + "%s/versions.json"
        private const val BUILD_TYPE_RELEASE = "release"
        private const val BUILD_TYPE_NIGHTLY = "nightly"
        private val BTA_TESTED_VERSIONS = listOf("v7.3", "v7.2_01", "v7.2", "v7.1_01", "v7.1")

        private fun getIconUrl(version: String, buildType: String): String {
            var iconName = version.replace('.', '_')
            if (buildType == "nightly") iconName = "v$iconName"
            return String.format(ICON_URL, buildType, version, iconName)
        }

        private fun getClientJarUrl(version: String, buildType: String) =
            String.format(CLIENT_JAR_URL, buildType, version)

        private fun getManifestUrl(buildType: String) =
            String.format(MANIFEST_URL, buildType)

        private fun <T> getManifest(buildType: String, parser: DownloadUtils.ParseCallback<T>): T {
            val manifestUrl = getManifestUrl(buildType)
            return DownloadUtils.downloadStringCached(manifestUrl, "bta_$manifestUrl", parser)
        }

        private fun createVersionList(versionStrings: List<String?>, buildType: String): List<BTAVersion> {
            val btaVersions = ArrayList<BTAVersion>(versionStrings.size)
            for (i in versionStrings.indices.reversed()) {
                val version = versionStrings[i] ?: continue
                btaVersions.add(BTAVersion(version, getClientJarUrl(version, buildType), getIconUrl(version, buildType)))
            }
            btaVersions.trimToSize()
            return btaVersions
        }

        @Throws(JsonParseException::class)
        private fun processNightliesJson(nightliesInfo: String): List<BTAVersion> {
            val manifest = Tools.GLOBAL_GSON.fromJson(nightliesInfo, BTAVersionsManifest::class.java)
            return createVersionList(manifest.versions, BUILD_TYPE_NIGHTLY)
        }

        @Throws(JsonParseException::class)
        private fun processReleasesJson(releasesInfo: String): BTAVersionList {
            val manifest = Tools.GLOBAL_GSON.fromJson(releasesInfo, BTAVersionsManifest::class.java)
            val stringVersions = manifest.versions
            val testedVersions = ArrayList<String>()
            val untestedVersions = ArrayList<String>()
            for (version in stringVersions) {
                if (version == null) break
                if (BTA_TESTED_VERSIONS.contains(version)) {
                    testedVersions.add(version)
                } else {
                    untestedVersions.add(version)
                }
            }
            return BTAVersionList(
                createVersionList(testedVersions, BUILD_TYPE_RELEASE),
                createVersionList(untestedVersions, BUILD_TYPE_RELEASE),
                null
            )
        }

        @JvmStatic
        @Throws(IOException::class)
        fun downloadVersionList(): BTAVersionList? {
            return try {
                val releases = getManifest(BUILD_TYPE_RELEASE) { processReleasesJson(it) }
                val nightlies = getManifest<String, List<BTAVersion>>(BUILD_TYPE_NIGHTLY) { processNightliesJson(it) }
                BTAVersionList(releases.testedVersions, releases.untestedVersions, nightlies)
            } catch (e: DownloadUtils.ParseException) {
                Log.e("BTAUtils", "Failed to process json", e)
                null
            }
        }
    }

    @Keep
    private class BTAVersionsManifest {
        @Keep
        var versions: List<String?>? = null
        @Keep
        @SerializedName("default")
        var defaultVersion: String? = null
    }

    class BTAVersion(
        val versionName: String,
        val downloadUrl: String,
        val iconUrl: String
    )

    class BTAVersionList(
        val testedVersions: List<BTAVersion>,
        val untestedVersions: List<BTAVersion>,
        val nightlyVersions: List<BTAVersion>?
    )
}
