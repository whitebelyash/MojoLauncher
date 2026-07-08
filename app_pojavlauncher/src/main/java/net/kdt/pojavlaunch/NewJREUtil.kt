package net.kdt.pojavlaunch

import net.kdt.pojavlaunch.Architecture.archAsString

import android.content.res.AssetManager
import android.util.Log

import com.kdt.mcgui.ProgressLayout

import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper
import net.kdt.pojavlaunch.utils.DownloadUtils
import net.kdt.pojavlaunch.utils.MathUtils
import net.kdt.pojavlaunch.utils.SignatureCheckUtil
import net.kdt.pojavlaunch.utils.jre.RuntimeSelectionException

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Arrays
import java.util.List
import java.util.Map

import git.artdeell.mojo.R

object NewJREUtil {
    private const val DOWNLOAD_URL = "https://mojolauncher.github.io/jre-download/"

    private fun getRemoteRuntimeVersion(internalRuntime: InternalRuntime): String {
        return DownloadUtils.downloadString("$DOWNLOAD_URL${internalRuntime.path}/version")
    }

    private fun checkLastUpdateTime(internalRuntime: InternalRuntime): Boolean {
        val lastUpdateTime = MultiRTUtils.readLastUpdateTime(internalRuntime.name)
        val currentTime = System.currentTimeMillis() / 1000L
        return lastUpdateTime != -1L && currentTime - lastUpdateTime < 259200
    }

    private fun writeLastUpdateTime(internalRuntime: InternalRuntime) {
        MultiRTUtils.writeLastUpdateTime(internalRuntime.name, System.currentTimeMillis() / 1000L)
    }

    @Throws(RuntimeSelectionException::class)
    private fun checkInternalRuntime(assetManager: AssetManager, internalRuntime: InternalRuntime) {
        val installedRuntimeVersion = MultiRTUtils.readInternalRuntimeVersion(internalRuntime.name)
        if (installedRuntimeVersion != null && checkLastUpdateTime(internalRuntime)) return
        val remoteRuntimeVersion: String = try {
            getRemoteRuntimeVersion(internalRuntime)
        } catch (exc: IOException) {
            Log.i("NewJreUtil", "Failed to get remote runtime version", exc)
            if (installedRuntimeVersion == null)
                throw RuntimeSelectionException(RuntimeSelectionException.RUNTIME_STATE_INTERNAL_RUNTIME_MISSING, internalRuntime.majorVersion)
            return
        }
        if (remoteRuntimeVersion != installedRuntimeVersion) unpackInternalRuntime(assetManager, internalRuntime, remoteRuntimeVersion)
        writeLastUpdateTime(internalRuntime)
    }

    private class RuntimeDownloaderVerifier(
        private val mSignatures: Map<String, ByteArray>,
        internalRuntime: InternalRuntime,
        private val mSignatureCheckUtil: SignatureCheckUtil
    ) {
        private val mRuntimePath: String = "$DOWNLOAD_URL${internalRuntime.path}/"
        private val mDownloadBuffer = ByteArray(8192)

        fun downloadAndVerify(component: String, output: File, progressString: Int): Boolean {
            DownloadUtils.downloadFileMonitored(
                "$mRuntimePath$component", output, mDownloadBuffer,
                DownloaderProgressWrapper(progressString, ProgressLayout.UNPACK_RUNTIME)
            )
            val signature = mSignatures[component] ?: return false
            FileInputStream(output).use { fileInputStream ->
                return mSignatureCheckUtil.verify(fileInputStream, signature)
            }
        }
    }

    private fun throwInstallFail(internalRuntime: InternalRuntime, cause: Throwable): Nothing {
        val e = RuntimeSelectionException(RuntimeSelectionException.RUNTIME_STATE_INSTALLATION_FAILED, internalRuntime.majorVersion)
        e.initCause(cause)
        throw e
    }

    private fun throwInstallFail(internalRuntime: InternalRuntime): Nothing {
        throw RuntimeSelectionException(RuntimeSelectionException.RUNTIME_STATE_INSTALLATION_FAILED, internalRuntime.majorVersion)
    }

    @Throws(RuntimeSelectionException::class)
    private fun unpackInternalRuntime(assetManager: AssetManager, internalRuntime: InternalRuntime, versionSignatures: String) {
        val signatures = SignatureCheckUtil.decodeSignatureBundle(versionSignatures)
        val platformBinFile = "bin-${archAsString(Tools.DEVICE_ARCHITECTURE)}.tar.xz"
        if (!signatures.containsKey("universal.tar.xz") || !signatures.containsKey(platformBinFile)) {
            throwInstallFail(internalRuntime)
        }

        var universalCache: File? = null
        var platformCache: File? = null
        try {
            val signatureCheckUtil = SignatureCheckUtil.create(assetManager)
            universalCache = File.createTempFile("jre-install-", "-universal", Tools.DIR_CACHE)
            platformCache = File.createTempFile("jre-install-", "-platform", Tools.DIR_CACHE)
            val runtimeDownloaderVerifier = RuntimeDownloaderVerifier(signatures, internalRuntime, signatureCheckUtil)
            if (!runtimeDownloaderVerifier.downloadAndVerify("universal.tar.xz", universalCache, R.string.downloading_java_runtime_uni) ||
                !runtimeDownloaderVerifier.downloadAndVerify(platformBinFile, platformCache, R.string.downloading_java_runtime_platform)) {
                throwInstallFail(internalRuntime)
            }

            FileInputStream(universalCache).use { universal ->
                FileInputStream(platformCache).use { platform ->
                    MultiRTUtils.installRuntimeNamedBinpack(universal, platform, internalRuntime.name, versionSignatures)
                    MultiRTUtils.postPrepare(internalRuntime.name)
                    MultiRTUtils.forceReread(internalRuntime.name)
                }
            }
        } catch (e: IOException) {
            throwInstallFail(internalRuntime, e)
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME)
            if (universalCache != null && universalCache.isFile) universalCache.delete()
            if (platformCache != null && platformCache.isFile) platformCache.delete()
        }
    }

    private fun getInternalRuntime(runtime: Runtime): InternalRuntime? {
        for (internalRuntime in InternalRuntime.entries) {
            if (internalRuntime.name == runtime.name) return internalRuntime
        }
        return null
    }

    private fun getNearestInstalledRuntime(targetVersion: Int): MathUtils.RankedValue<Runtime> {
        val runtimes = MultiRTUtils.getRuntimes()
        return MathUtils.findNearestPositive(targetVersion, runtimes) { runtime: Runtime -> runtime.javaVersion }
    }

    private fun getNearestInternalRuntime(targetVersion: Int): MathUtils.RankedValue<InternalRuntime> {
        val runtimeList = InternalRuntime.entries.toList()
        return MathUtils.findNearestPositive(targetVersion, runtimeList) { runtime: InternalRuntime -> runtime.majorVersion }
    }

    @Throws(IOException::class, RuntimeSelectionException::class)
    fun installNewJreIfNeeded(assetManager: AssetManager, versionInfo: JVersionList.Version) {
        if (versionInfo.javaVersion == null || versionInfo.javaVersion.component.equals("jre-legacy", ignoreCase = true)) return

        val gameRequiredVersion = versionInfo.javaVersion.majorVersion

        val instance = Instances.loadSelectedInstance()
        val profileRuntime = Tools.getSelectedRuntime(instance!!)
        val runtime = MultiRTUtils.read(profileRuntime)
        if (runtime.javaVersion >= gameRequiredVersion) {
            val internalRuntime = getInternalRuntime(runtime)
            if (internalRuntime != null) {
                checkInternalRuntime(assetManager, internalRuntime)
            }
            return
        }

        val nearestInstalledRuntime = getNearestInstalledRuntime(gameRequiredVersion)
        val nearestInternalRuntime = getNearestInternalRuntime(gameRequiredVersion)

        val selectedRankedRuntime = MathUtils.objectMin(
            nearestInternalRuntime, nearestInstalledRuntime
        ) { value: MathUtils.RankedValue<*> -> value.rank }

        if (selectedRankedRuntime == null) {
            throw RuntimeSelectionException(RuntimeSelectionException.RUNTIME_STATE_SELECTION_FAILED, gameRequiredVersion)
        }

        val selected = selectedRankedRuntime.value
        val appropriateRuntime: String
        val internalRuntime: InternalRuntime?

        if (selected is Runtime) {
            appropriateRuntime = selected.name
            internalRuntime = getInternalRuntime(selected)
        } else if (selected is InternalRuntime) {
            internalRuntime = selected
            appropriateRuntime = internalRuntime.name
        } else {
            throw RuntimeException("Unexpected type of selected: ${selected!!.javaClass.name}")
        }

        if (internalRuntime != null) {
            checkInternalRuntime(assetManager, internalRuntime)
        }

        instance.selectedRuntime = appropriateRuntime
        instance.write()
    }

    enum class InternalRuntime(
        val majorVersion: Int,
        val name: String,
        val path: String
    ) {
        JRE_17(17, "Internal-17", "components/jre-new"),
        JRE_21(21, "Internal-21", "components/jre-21"),
        JRE_25(25, "Internal-25", "components/jre-25")
    }
}
