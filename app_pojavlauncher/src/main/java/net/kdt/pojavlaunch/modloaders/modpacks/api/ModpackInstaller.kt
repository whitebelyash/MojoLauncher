package net.kdt.pojavlaunch.modloaders.modpacks.api

import com.kdt.mcgui.ProgressLayout
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceInstaller
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Callable

class ModpackInstaller {
    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun installModpack(
            modpackName: String,
            title: String,
            modpackFile: File,
            icon: String,
            installFunction: InstallFunction
        ): ModLoader {
            val instance = Instances.createInstance({ i -> i.name = title }, modpackName.substring(0, minOf(16, modpackName.length)))
            val modLoaderInfo: ModLoader
            try {
                modLoaderInfo = installFunction.installModpack(modpackFile, instance.gameDirectory)
                    ?: throw IOException("Unknown modpack mod loader information")
                if (modLoaderInfo.requiresGuiInstallation()) {
                    val instanceInstaller = modLoaderInfo.createInstaller()
                        ?: throw IOException("Failed to prepare data for instance installation")
                    instance.installer = instanceInstaller
                } else {
                    val versionId = modLoaderInfo.installHeadlessly()
                        ?: throw IOException("Unknown mod loader version")
                    instance.versionId = versionId
                }
                instance.write()
                ModIconCache.writeInstanceImage(instance, icon)
                Instances.setSelectedInstance(instance)
                if (modLoaderInfo.requiresGuiInstallation()) {
                    instance.installer.start()
                }
            } catch (e: IOException) {
                Instances.removeInstance(instance)
                throw e
            } finally {
                modpackFile.delete()
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
            }
            return modLoaderInfo
        }

        @JvmStatic
        @Throws(IOException::class)
        fun downloadModpack(
            modDetail: ModDetail,
            selectedVersion: Int,
            installFunction: InstallFunction
        ): ModLoader {
            val versionUrl = modDetail.versionUrls[selectedVersion]
            var versionHash = modDetail.versionHashes[selectedVersion]
            var modpackName = (modDetail.title.lowercase(Locale.ROOT) + " " + modDetail.versionNames[selectedVersion])
                .trim { it <= ' ' }.replace("[\\\\/:*?\"<>| \\t\\n]".toRegex(), "_")
            val name = modDetail.title
            val icon = modDetail.getIconCacheTag()
            if (versionHash != null) modpackName += "_$versionHash"
            if (modpackName.length > 255) modpackName = modpackName.substring(0, 255)
            val modpackFile = File(Tools.DIR_CACHE, "$modpackName.cf")
            val downloadBuffer = ByteArray(8192)
            try {
                DownloadUtils.ensureSha1(modpackFile, versionHash, Callable {
                    DownloadUtils.downloadFileMonitored(
                        versionUrl, modpackFile, downloadBuffer,
                        DownloaderProgressWrapper(R.string.modpack_download_downloading_metadata, ProgressLayout.INSTALL_MODPACK)
                    )
                    null
                })
            } catch (e: IOException) {
                modpackFile.delete()
                throw e
            }
            return installModpack(modpackName, name, modpackFile, icon, installFunction)
        }
    }

    fun interface InstallFunction {
        @Throws(IOException::class)
        fun installModpack(modpackFile: File, instanceDestination: File): ModLoader?
    }
}
