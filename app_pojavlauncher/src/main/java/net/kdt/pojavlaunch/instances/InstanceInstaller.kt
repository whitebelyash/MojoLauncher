package net.kdt.pojavlaunch.instances

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import com.kdt.mcgui.ProgressLayout
import net.kdt.pojavlaunch.JavaGUILauncherActivity
import net.kdt.pojavlaunch.LauncherActivity
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.profcompat.ProfileWatcher
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask
import net.kdt.pojavlaunch.modloaders.OFDownloadPageScraper
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper
import net.kdt.pojavlaunch.utils.DownloadUtils
import net.kdt.pojavlaunch.utils.JSONUtils
import net.kdt.pojavlaunch.utils.NotificationUtils
import java.io.File
import java.io.IOException
import java.util.*

class InstanceInstaller : ContextExecutorTask {
    companion object {
        private val sLastInstallInfo = File(Tools.DIR_CACHE, "last_installer.json")
        private val TRUSTED_URLS = arrayOf(
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/",
            "https://maven.minecraftforge.net/net/minecraftforge/forge/",
            "https://optifine.net/adloadx"
        )

        fun postInstallCheck(assetManager: AssetManager) {
            if (!sLastInstallInfo.exists() || !sLastInstallInfo.isFile) return
            val lastInstaller = JSONUtils.readFromFile(sLastInstallInfo, InstanceInstaller::class.java) ?: return
            lastInstaller.installerJar()?.delete()
            if (!sLastInstallInfo.delete()) throw IOException("Failed to delete mod installer info")
            val targetVersionId = ProfileWatcher.consumePendingVersion(assetManager) ?: return
            for (instance in Instances.loadAllInstances()) {
                if (!lastInstaller.equals(instance.installer)) continue
                instance.installer = null
                instance.versionId = targetVersionId
                instance.write()
            }
            ExtraCore.setValue(ExtraConstants.REFRESH_VERSION_SPINNER, null)
        }

        fun postInstallCheck(context: Context) {
            try {
                postInstallCheck(context.assets)
            } catch (e: Exception) {
                Tools.showError(context, e)
                if (sLastInstallInfo.isFile) {
                    sLastInstallInfo.delete()
                }
            }
        }
    }

    @JvmField var installerJar: String? = null
    @Transient
    private var installerJarFile: File? = null
    @Transient
    private var mTransformedUrl: String? = null
    @JvmField var commandLineArgs: MutableList<String>? = null
    @JvmField var installerUrlTransformer: String? = null
    @JvmField var installerDownloadUrl: String? = null
    @JvmField var installerSha1: String? = null

    private fun installerJar(): File? {
        if (installerJarFile == null) {
            installerJarFile = installerJar?.let { File(it) }
        }
        return installerJarFile
    }

    @Throws(IOException::class)
    private fun installerDownloadUrl(): String {
        if (mTransformedUrl != null) return mTransformedUrl!!
        mTransformedUrl = if ("optifine" == installerUrlTransformer) {
            OFDownloadPageScraper.run(installerDownloadUrl!!)
        } else {
            installerDownloadUrl!!
        }
        return mTransformedUrl!!
    }

    @Throws(IOException::class)
    private fun writeLastInstaller() {
        JSONUtils.writeToFile(sLastInstallInfo, this)
    }

    @Throws(IOException::class)
    fun threadedStart() {
        try {
            val buffer = ByteArray(8192)
            val wrapper = DownloaderProgressWrapper(
                R.string.mcl_launch_downloading_progress, ProgressLayout.INSTANCE_INSTALL
            )
            wrapper.extraString = installerJar()?.name
            DownloadUtils.ensureSha1(installerJar(), installerSha1) {
                DownloadUtils.downloadFileMonitored(installerDownloadUrl(), installerJar(), buffer, wrapper)
                null
            }
            ContextExecutor.execute(this)
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.INSTANCE_INSTALL)
        }
    }

    fun start() {
        ProgressLayout.setProgress(ProgressLayout.INSTANCE_INSTALL, 0)
        PojavApplication.sExecutorService.execute {
            try {
                threadedStart()
            } catch (e: Exception) {
                Tools.showErrorRemote(e)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InstanceInstaller) return false
        return installerJar == other.installerJar &&
                commandLineArgs == other.commandLineArgs &&
                installerDownloadUrl == other.installerDownloadUrl &&
                installerUrlTransformer == other.installerUrlTransformer &&
                installerSha1 == other.installerSha1
    }

    override fun hashCode(): Int {
        return Objects.hash(installerJar, commandLineArgs, installerDownloadUrl, installerUrlTransformer, installerSha1)
    }

    private fun isTrustedInstaller(): Boolean {
        for (frontTrusted in TRUSTED_URLS) {
            if (installerDownloadUrl?.startsWith(frontTrusted) == true) return true
        }
        return false
    }

    override fun executeWithActivity(activity: Activity) {
        try {
            ProfileWatcher.installDefaultProfiles(activity.assets)
            writeLastInstaller()
        } catch (e: Exception) {
            Tools.showError(activity, e)
            return
        }
        val intent = Intent(activity, JavaGUILauncherActivity::class.java)
        val extras = Bundle()
        extras.putStringArrayList("javaArgs", ArrayList(commandLineArgs))
        extras.putString("modPath", installerJar)
        extras.putBoolean("trusted", isTrustedInstaller())
        intent.putExtras(extras)
        activity.startActivity(intent)
    }

    override fun executeWithApplication(context: Context) {
        Tools.runOnUiThread {
            NotificationUtils.sendBasicNotification(
                context,
                R.string.modpack_install_notification_title,
                R.string.modpack_install_notification_success,
                Intent(context, LauncherActivity::class.java),
                NotificationUtils.PENDINGINTENT_CODE_DOWNLOAD_SERVICE,
                NotificationUtils.NOTIFICATION_ID_DOWNLOAD_LISTENER
            )
        }
    }
}
