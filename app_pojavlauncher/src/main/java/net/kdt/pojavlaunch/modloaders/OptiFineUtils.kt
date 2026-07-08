package net.kdt.pojavlaunch.modloaders

import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.InstanceInstaller
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.File
import java.io.IOException
import java.util.List
import java.util.Objects

class OptiFineUtils {
    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun downloadOptiFineVersions(): OptiFineVersions? {
            return try {
                DownloadUtils.downloadStringCached("https://optifine.net/downloads", "of_downloads_page", OptiFineScraper())
            } catch (e: DownloadUtils.ParseException) {
                e.printStackTrace()
                null
            }
        }

        @JvmStatic
        fun createInstaller(version: OptiFineVersion): InstanceInstaller {
            val installerHash = Objects.hash(version.versionName, version.gameVersion)
            val installerLocation = File(Tools.DIR_CACHE, "optifine-installer-$installerHash.jar")
            val instanceInstaller = InstanceInstaller()
            instanceInstaller.installerUrlTransformer = "optifine"
            instanceInstaller.installerDownloadUrl = version.downloadUrl
            instanceInstaller.installerJar = installerLocation.absolutePath
            instanceInstaller.commandLineArgs = List.of("-javaagent:${Tools.DIR_DATA}/forge_installer/forge_installer.jar=OF")
            return instanceInstaller
        }
    }

    class OptiFineVersions {
        var gameVersions: MutableList<String?> = ArrayList()
        var optifineVersions: MutableList<MutableList<OptiFineVersion>> = ArrayList()
    }

    class OptiFineVersion {
        var gameVersion: String? = null
        var versionName: String? = null
        var downloadUrl: String? = null
    }
}
