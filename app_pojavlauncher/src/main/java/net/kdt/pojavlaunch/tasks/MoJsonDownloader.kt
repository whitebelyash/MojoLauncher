package net.kdt.pojavlaunch.tasks

import android.content.res.AssetManager
import android.util.Log
import com.google.gson.JsonParseException
import com.kdt.mcgui.ProgressLayout
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.JAssetInfo
import net.kdt.pojavlaunch.JAssets
import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.NewJREUtil
import net.kdt.pojavlaunch.PojavApplication.sExecutorService
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.downloader.Downloader
import net.kdt.pojavlaunch.downloader.TaskMetadata
import net.kdt.pojavlaunch.mirrors.DownloadMirror
import net.kdt.pojavlaunch.mirrors.MirrorTamperedException
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.DownloadUtils
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.JSONUtils
import net.kdt.pojavlaunch.utils.MavenNameUtils
import net.kdt.pojavlaunch.utils.jre.RuntimeSelectionException
import net.kdt.pojavlaunch.value.DependentLibrary
import net.kdt.pojavlaunch.value.LibraryArtifact
import net.kdt.pojavlaunch.value.LibrarySubstitution
import net.kdt.pojavlaunch.value.MoJsonRule
import net.kdt.pojavlaunch.value.NativeLibraryExtractable
import net.kdt.pojavlaunch.value.SubstitutionMap
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.concurrent.Future

class MoJsonDownloader : Downloader(ProgressLayout.DOWNLOAD_GAME) {
    private val mNativeName = "android-${Architecture.archAsString(Architecture.getDeviceArchitecture())}"
    private var mScheduledDownloadTasks: ArrayList<TaskMetadata>? = null
    private var mDeclaredNatives: MutableList<NativeLibraryExtractable>? = null
    private var mAllLibraries: LinkedHashMap<String, DependentLibrary>? = null
    private var mClassPath: LinkedHashSet<File>? = null
    private var mSubstitutionMap: SubstitutionMap? = null
    private var mSourceJarFile: File? = null
    private var mTargetJarFile: File? = null
    private var mVersionName: String? = null

    fun start(
        assetManager: AssetManager?,
        version: JVersionList.Version?,
        realVersion: String,
        listener: MoJsonExtras.DoneListener
    ) {
        sExecutorService.execute {
            try {
                downloadGame(assetManager, version, realVersion)
                listener.onDownloadDone(mClassPath!!.toTypedArray())
            } catch (e: JsonParseException) {
                listener.onDownloadFailed(e)
            } catch (e: RuntimeException) {
                throw e
            } catch (e: Exception) {
                listener.onDownloadFailed(e)
            }
            ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_GAME)
        }
    }

    @Throws(Exception::class)
    private fun downloadGame(assetManager: AssetManager?, verInfo: JVersionList.Version?, versionName: String) {
        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_GAME, 0, R.string.newdl_starting)
        mTargetJarFile = createGameJarPath(versionName)
        mScheduledDownloadTasks = ArrayList()
        mDeclaredNatives = ArrayList()
        mAllLibraries = LinkedHashMap()
        if (sSubstitutionMapFuture == null) throw RuntimeException("SubstitutionMap not prepared")
        mSubstitutionMap = sSubstitutionMapFuture!!.get()
        mVersionName = versionName
        downloadAndProcessMetadata(assetManager, verInfo, versionName)
        val downloadLibCount = mAllLibraries!!.size
        mClassPath = LinkedHashSet(downloadLibCount)
        growDownloadList(downloadLibCount)
        for (dependentLibrary in mAllLibraries!!.values) {
            if (dependentLibrary.name?.startsWith("net.java.dev.jna:jna:") == true && !dependentLibrary.replaced) {
                scheduleAarDownload(Tools.MAVEN_CENTRAL, dependentLibrary)
            }
            if (dependentLibrary.downloads != null) processLibraryWithDownloads(dependentLibrary)
            else processRawLibrary(dependentLibrary)
        }
        mAllLibraries!!.clear()
        mClassPath!!.add(mTargetJarFile)
        runDownloads(mScheduledDownloadTasks!!)
        ensureJarFileCopy()
        extractNatives(mVersionName!!)
    }

    private fun createGameJsonPath(versionId: String) = File(Tools.DIR_HOME_VERSION, "$versionId${File.separator}$versionId.json")
    private fun createGameJarPath(versionId: String) = File(Tools.DIR_HOME_VERSION, "$versionId${File.separator}$versionId.jar")

    @Throws(IOException::class)
    private fun ensureJarFileCopy() {
        if (mSourceJarFile == null) return
        if (mSourceJarFile == mTargetJarFile) return
        if (mTargetJarFile!!.exists()) return
        FileUtils.ensureParentDirectory(mTargetJarFile!!)
        Log.i("NewMCDownloader", "Copying ${mSourceJarFile!!.name} to ${mTargetJarFile!!.absolutePath}")
        org.apache.commons.io.FileUtils.copyFile(mSourceJarFile, mTargetJarFile, false)
    }

    @Throws(IOException::class)
    private fun extractNatives(versionName: String) {
        if (mDeclaredNatives!!.isEmpty()) return
        val totalCount = mDeclaredNatives!!.size
        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_GAME, 0, R.string.newdl_extracting_native_libraries, 0, totalCount)
        val targetDirectory = File(Tools.DIR_CACHE, "natives/$versionName")
        FileUtils.ensureDirectory(targetDirectory)
        val nativesExtractor = NativesExtractor(targetDirectory)
        var extractedCount = 0
        for (extractable in mDeclaredNatives!!) {
            if (extractable.extractInfo == null) nativesExtractor.extractFromAar(extractable.path)
            else nativesExtractor.extractMoJson(extractable.path, extractable.extractInfo)
            extractedCount++
            ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_GAME, extractedCount * 100 / totalCount, R.string.newdl_extracting_native_libraries, extractedCount, totalCount)
        }
    }

    @Throws(IOException::class, MirrorTamperedException::class)
    private fun downloadGameJson(verInfo: JVersionList.Version): File {
        val targetFile = createGameJsonPath(verInfo.id)
        if (verInfo.sha1 == null && targetFile.canRead() && targetFile.isFile) return targetFile
        FileUtils.ensureParentDirectory(targetFile)
        try {
            DownloadUtils.ensureSha1(targetFile, if (LauncherPreferences.PREF_VERIFY_MANIFEST) verInfo.sha1 else null) {
                ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_GAME, 0, R.string.newdl_downloading_metadata, targetFile.name)
                DownloadMirror.downloadFileMirrored(DownloadMirror.DOWNLOAD_CLASS_METADATA, verInfo.url!!, targetFile)
                null
            }
        } catch (e: DownloadUtils.SHA1VerificationException) {
            throw if (DownloadMirror.isMirrored()) MirrorTamperedException() else e
        }
        return targetFile
    }

    @Throws(IOException::class)
    private fun downloadAssetsIndex(verInfo: JVersionList.Version): JAssets? {
        val assetIndex = verInfo.assetIndex ?: return null
        if (verInfo.assets == null) return null
        val targetFile = File(Tools.ASSETS_PATH, "indexes${File.separator}${verInfo.assets}.json")
        FileUtils.ensureParentDirectory(targetFile)
        DownloadUtils.ensureSha1(targetFile, assetIndex.sha1) {
            ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_GAME, 0, R.string.newdl_downloading_metadata, targetFile.name)
            DownloadMirror.downloadFileMirrored(DownloadMirror.DOWNLOAD_CLASS_METADATA, assetIndex.url!!, targetFile)
            null
        }
        return Tools.GLOBAL_GSON.fromJson(Tools.read(targetFile), JAssets::class.java)
    }

    private fun getClientInfo(verInfo: JVersionList.Version): ClientInfo? = verInfo.downloads?.get("client")

    @Throws(IOException::class, MirrorTamperedException::class, RuntimeSelectionException::class, JsonParseException::class)
    private fun downloadAndProcessMetadata(assetManager: AssetManager?, verInfo: JVersionList.Version?, versionName: String) {
        var vi = verInfo
        val versionJsonFile = if (vi != null) downloadGameJson(vi) else createGameJsonPath(versionName)
        if (versionJsonFile.canRead()) {
            vi = JSONUtils.readFromFile(versionJsonFile, JVersionList.Version::class.java)
                ?: throw IOException("Deserialized json is null. Contact developer.")
        } else {
            throw IOException("Unable to read Version JSON for version $versionName")
        }
        if (assetManager != null) NewJREUtil.installNewJreIfNeeded(assetManager, vi)
        if (Tools.isValidString(vi.inheritsFrom)) {
            val inheritedVersion = MoJsonExtras.getListedVersion(vi.inheritsFrom)
            downloadAndProcessMetadata(assetManager, inheritedVersion, vi.inheritsFrom)
        }
        val assets = downloadAssetsIndex(vi)
        if (assets != null) scheduleAssetDownloads(assets)
        val clientInfo = getClientInfo(vi)
        if (clientInfo != null) scheduleGameJarDownload(clientInfo, versionName)
        if (vi.libraries != null) scheduleLibraryDownloads(vi.libraries)
        if (vi.logging != null) scheduleLoggingAssetDownloadIfNeeded(vi.logging)
    }

    private fun growDownloadList(addedElementCount: Int) {
        mScheduledDownloadTasks!!.ensureCapacity(mScheduledDownloadTasks!!.size + addedElementCount)
    }

    @Throws(IOException::class)
    private fun scheduleDownload(targetFile: File, downloadClass: Int, url: String?, sha1: String?,
                                  size: Long) {
        FileUtils.ensureParentDirectory(targetFile)
        var sha1Hash = sha1
        if (!Tools.isValidString(sha1Hash)) sha1Hash = null
        val urlObject = if (Tools.isValidString(url)) URL(url!!) else null
        val taskMetadata = TaskMetadata(targetFile, urlObject, size, sha1Hash, downloadClass)
        mScheduledDownloadTasks!!.add(taskMetadata)
    }

    @Throws(IOException::class)
    private fun scheduleAarDownload(baseRepository: String, dependentLibrary: DependentLibrary) {
        val path = MavenNameUtils.mavenNameToAarPath(dependentLibrary.name!!)
        val downloadUrl = baseRepository + path
        val targetPath = File(Tools.DIR_HOME_LIBRARY, path)
        mDeclaredNatives!!.add(NativeLibraryExtractable(targetPath, null))
        scheduleDownload(targetPath, DownloadMirror.DOWNLOAD_CLASS_LIBRARIES, downloadUrl, null, -1L)
    }

    @Throws(IOException::class)
    private fun submitBareLibrary(path: String, baseUrl: String) {
        val artifactPath = File(Tools.DIR_HOME_LIBRARY, path)
        if (!mClassPath!!.add(artifactPath)) {
            Log.w("MoJsonDownloader", "Repeated classpath entry $path skipped")
            return
        }
        scheduleDownload(artifactPath, DownloadMirror.DOWNLOAD_CLASS_LIBRARIES, baseUrl + path, null, -1L)
    }

    @Throws(IOException::class)
    private fun submitArtifact(artifact: LibraryArtifact, subPath: String): File? {
        val artifactPath = File(Tools.DIR_HOME_LIBRARY, subPath)
        if (!mClassPath!!.add(artifactPath)) {
            Log.w("MoJsonDownloader", "Repeated classpath entry ${artifact.path} skipped")
            return null
        }
        scheduleDownload(artifactPath, DownloadMirror.DOWNLOAD_CLASS_LIBRARIES, artifact.url, artifact.sha1, artifact.size.toLong())
        return artifactPath
    }

    private fun canIgnoreNatives(libName: String) = libName.startsWith("com.mojang:text2speech")

    @Throws(IOException::class)
    private fun processNatives(library: DependentLibrary) {
        val libraryClassifier = library.natives?.get(mNativeName) ?: run {
            if (!canIgnoreNatives(library.name)) throw IOException("library ${library.name} does not include native $mNativeName")
            Log.i("MoJsonDownloader", "Library ${library.name} doesn't have an $mNativeName natives-classifier (skipped)")
            return
        }
        val artifact = library.downloads?.classifiers?.get(libraryClassifier)
            ?: throw IOException("library ${library.name} is missing required classifier $libraryClassifier")
        var subPath = artifact.path
        if (subPath == null) subPath = MavenNameUtils.mavenNameToPath(library.name!!, libraryClassifier)
        val artifactPath = submitArtifact(artifact, subPath)
        if (library.extract != null && artifactPath != null) {
            mDeclaredNatives!!.add(NativeLibraryExtractable(artifactPath, library.extract))
        }
    }

    @Throws(IOException::class)
    private fun processLibraryWithDownloads(library: DependentLibrary) {
        val downloads = library.downloads ?: return
        val artifact = downloads.artifact
        if (artifact != null) {
            var subPath = artifact.path
            if (subPath == null) subPath = MavenNameUtils.mavenNameToPath(library.name!!)
            submitArtifact(artifact, subPath)
        }
        if (library.natives != null && downloads.classifiers != null) processNatives(library)
    }

    @Throws(IOException::class)
    private fun processRawLibrary(library: DependentLibrary) {
        val path = MavenNameUtils.mavenNameToPath(library.name!!)
        var baseUrl = library.url
        if (baseUrl != null) baseUrl = baseUrl.replace("http://", "https://")
        else baseUrl = "https://libraries.minecraft.net/"
        submitBareLibrary(path, baseUrl)
    }

    @Throws(IOException::class)
    private fun scheduleLibraryDownloads(dependentLibraries: Array<DependentLibrary>) {
        Tools.preProcessLibraries(dependentLibraries)
        for (dl in dependentLibraries) {
            var dependentLibrary = dl
            val rules = dependentLibrary.rules
            if (rules != null) {
                val ruleSetAction = MoJsonRule.ruleSetCheck(rules)
                if (ruleSetAction != "allow") continue
            }
            val substitution = mSubstitutionMap!!.findSubstitution(dependentLibrary.name)
            if (substitution != null) {
                if (substitution.skip) continue
                dependentLibrary = substitution
            }
            val libraryTrimmedName = MavenNameUtils.mavenBaseName(dependentLibrary.name!!)
            if (mAllLibraries!!.containsKey(libraryTrimmedName)) {
                mAllLibraries!!.remove(libraryTrimmedName)
            }
            mAllLibraries!![libraryTrimmedName] = dependentLibrary
        }
    }

    @Throws(IOException::class)
    private fun scheduleAssetDownloads(assets: JAssets) {
        val assetObjects = assets.objects ?: return
        val assetNames = assetObjects.keys
        growDownloadList(assetNames.size)
        for (asset in assetNames) {
            val assetInfo = assetObjects[asset] ?: continue
            val hash = assetInfo.hash ?: continue
            val hashedPath = "${hash.substring(0, 2)}${File.separator}$hash"
            val basePath = if (assets.mapToResources) Tools.OBSOLETE_RESOURCES_PATH else Tools.ASSETS_PATH
            val targetFile = if (assets.virtual || assets.mapToResources) File(basePath, asset)
            else File(basePath, "objects${File.separator}$hashedPath")
            scheduleDownload(targetFile, DownloadMirror.DOWNLOAD_CLASS_ASSETS, "$MC_RES$hashedPath", assetInfo.hash, assetInfo.size.toLong())
        }
    }

    @Throws(IOException::class)
    private fun scheduleLoggingAssetDownloadIfNeeded(loggingConfig: JVersionList.LoggingConfig) {
        val loggingFileProperties = loggingConfig.client?.file ?: return
        val id = loggingFileProperties.id ?: return
        val internalLoggingConfig = File("${Tools.DIR_DATA}${File.separator}security", id.replace("client", "log4j-rce-patch"))
        if (internalLoggingConfig.exists()) return
        val destination = File(Tools.DIR_GAME_NEW, id)
        scheduleDownload(destination, DownloadMirror.DOWNLOAD_CLASS_LIBRARIES, loggingFileProperties.url, loggingFileProperties.sha1, loggingFileProperties.size)
    }

    @Throws(IOException::class)
    private fun scheduleGameJarDownload(clientInfo: ClientInfo, versionName: String) {
        val clientJar = createGameJarPath(versionName)
        growDownloadList(1)
        scheduleDownload(clientJar, DownloadMirror.DOWNLOAD_CLASS_LIBRARIES, clientInfo.url, clientInfo.sha1, clientInfo.size.toLong())
        mSourceJarFile = clientJar
    }

    companion object {
        const val MC_RES = "https://resources.download.minecraft.net/"
        private var sSubstitutionMapFuture: Future<SubstitutionMap>? = null

        fun prepareSubstitutionMap(assetManager: AssetManager) {
            sSubstitutionMapFuture = sExecutorService.submit {
                assetManager.open("substitutions.json").use { stream ->
                    JSONUtils.readFromStream(stream, SubstitutionMap::class.java)
                }
            }
        }
    }
}
