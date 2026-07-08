package net.kdt.pojavlaunch.modloaders.modpacks.api

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.kdt.mcgui.ProgressLayout
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.downloader.AcquireableTaskMetadata
import net.kdt.pojavlaunch.downloader.Downloader
import net.kdt.pojavlaunch.mirrors.DownloadMirror
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants
import net.kdt.pojavlaunch.modloaders.modpacks.models.CurseManifest
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.GsonJsonUtils
import net.kdt.pojavlaunch.utils.ZipUtils
import java.io.File
import java.io.IOException
import java.net.URL
import java.net.URLDecoder
import java.util.ArrayList
import java.util.HashMap
import java.util.regex.Pattern
import java.util.zip.ZipFile

class CurseforgeApi(apiKey: String) : ModpackApi {
    private val mApiHandler: ApiHandler = ApiHandler("https://api.curseforge.com/v1", apiKey)

    override fun searchMod(searchFilters: SearchFilters, previousPageResult: SearchResult?): SearchResult? {
        val curseforgeSearchResult = previousPageResult as? CurseforgeSearchResult
        val params = HashMap<String, Any>()
        params["gameId"] = CURSEFORGE_MC_GAME_ID
        params["classId"] = if (searchFilters.isModpack) CURSEFORGE_MODPACK_CLASS_ID else CURSEFORGE_MOD_CLASS_ID
        params["searchFilter"] = searchFilters.name
        params["sortField"] = CURSEFORGE_SORT_RELEVANCY
        params["sortOrder"] = "desc"
        if (!searchFilters.mcVersion.isNullOrEmpty()) params["gameVersion"] = searchFilters.mcVersion
        if (previousPageResult != null) params["index"] = curseforgeSearchResult!!.previousOffset
        val response = mApiHandler.get("mods/search", params, JsonObject::class.java) ?: return null
        val dataArray = response.getAsJsonArray("data") ?: return null
        val paginationInfo = response.getAsJsonObject("pagination")
        val modItemList = ArrayList<ModItem>(dataArray.size())
        for (i in 0 until dataArray.size()) {
            val dataElement = dataArray[i].asJsonObject
            val allowModDistribution = dataElement.get("allowModDistribution")
            if (!allowModDistribution.isJsonNull && !allowModDistribution.asBoolean) {
                Log.i("CurseforgeApi", "Skipping modpack " + dataElement["name"].asString + " because curseforge sucks")
                continue
            }
            modItemList.add(
                ModItem(
                    Constants.SOURCE_CURSEFORGE,
                    searchFilters.isModpack,
                    dataElement["id"].asString,
                    dataElement["name"].asString,
                    dataElement["summary"].asString,
                    dataElement.getAsJsonObject("logo")["thumbnailUrl"].asString
                )
            )
        }
        val csr = curseforgeSearchResult ?: CurseforgeSearchResult()
        csr.results = modItemList.toTypedArray()
        csr.totalResultCount = paginationInfo["totalCount"].asInt
        csr.previousOffset += dataArray.size()
        return csr
    }

    override fun getModDetails(item: ModItem): ModDetail? {
        val allModDetails = ArrayList<JsonObject>()
        var index = 0
        while (index != CURSEFORGE_PAGINATION_END_REACHED && index != CURSEFORGE_PAGINATION_ERROR) {
            index = getPaginatedDetails(allModDetails, index, item.id)
        }
        if (index == CURSEFORGE_PAGINATION_ERROR) return null
        val length = allModDetails.size
        val versionNames = arrayOfNulls<String>(length)
        val mcVersionNames = arrayOfNulls<String>(length)
        val versionUrls = arrayOfNulls<String>(length)
        val hashes = arrayOfNulls<String>(length)
        for (i in allModDetails.indices) {
            val modDetail = allModDetails[i]
            versionNames[i] = modDetail["displayName"].asString
            val downloadUrl = modDetail["downloadUrl"]
            versionUrls[i] = downloadUrl.asString
            val gameVersions = modDetail.getAsJsonArray("gameVersions")
            for (jsonElement in gameVersions) {
                val gameVersion = jsonElement.asString
                if (sMcVersionPattern.matcher(gameVersion).matches()) {
                    mcVersionNames[i] = gameVersion
                    break
                }
            }
            hashes[i] = getSha1FromModData(modDetail)
        }
        return ModDetail(item, versionNames, mcVersionNames, versionUrls, hashes)
    }

    @Throws(IOException::class)
    override fun installModpack(modDetail: ModDetail, selectedVersion: Int): ModLoader? {
        return ModpackInstaller.downloadModpack(modDetail, selectedVersion, this::installCurseforgeZip)
    }

    @Throws(IOException::class)
    override fun installLocalModpack(modpackName: String, modpackFile: File, icon: String): ModLoader? {
        return ModpackInstaller.installModpack(modpackName, modpackName, modpackFile, icon, this::installCurseforgeZip)
    }

    private fun getPaginatedDetails(objectList: MutableList<JsonObject>, index: Int, modId: String): Int {
        val params = HashMap<String, Any>()
        params["index"] = index
        params["pageSize"] = CURSEFORGE_PAGINATION_SIZE
        val response = mApiHandler.get("mods/$modId/files", params, JsonObject::class.java)
        val data = GsonJsonUtils.getJsonArraySafe(response, "data")
        Log.i("CurseforgeApi", "data...")
        if (data == null) return CURSEFORGE_PAGINATION_ERROR
        Log.i("CurseforgeApi", "filtering...")
        for (i in 0 until data.size()) {
            val fileInfo = data[i].asJsonObject
            if (fileInfo["isServerPack"].asBoolean) continue
            objectList.add(fileInfo)
        }
        Log.i("CurseforgeApi", "pag_end")
        return if (data.size() < CURSEFORGE_PAGINATION_SIZE) CURSEFORGE_PAGINATION_END_REACHED
        else index + data.size()
    }

    @Throws(IOException::class)
    private fun installCurseforgeZip(zipFile: File, instanceDestination: File): ModLoader? {
        ZipFile(zipFile).use { modpackZipFile ->
            val curseManifest = Tools.GLOBAL_GSON.fromJson(
                Tools.read(ZipUtils.getEntryStream(modpackZipFile, "manifest.json")),
                CurseManifest::class.java
            )
            if (!verifyManifest(curseManifest)) {
                Log.i("CurseforgeApi", "manifest verification failed")
                return null
            }
            try {
                CurseDownloader().start(curseManifest, instanceDestination)
            } catch (e: InterruptedException) {
                throw IOException("NIY: InterruptedException", e)
            }
            val overridesDir = curseManifest.overrides ?: "overrides"
            ZipUtils.zipExtract(modpackZipFile, overridesDir, instanceDestination)
            return createInfo(curseManifest.minecraft!!)
        }
    }

    private fun createInfo(minecraft: CurseManifest.CurseMinecraft): ModLoader? {
        var primaryModLoader: CurseManifest.CurseModLoader? = null
        for (modLoader in minecraft.modLoaders) {
            if (modLoader.primary) {
                primaryModLoader = modLoader
                break
            }
        }
        val pml = primaryModLoader ?: minecraft.modLoaders[0]
        val modLoaderId = pml.id
        val dashIndex = modLoaderId.indexOf('-')
        val modLoaderName = modLoaderId.substring(0, dashIndex)
        val modLoaderVersion = modLoaderId.substring(dashIndex + 1)
        Log.i("CurseforgeApi", "$modLoaderId $modLoaderName $modLoaderVersion")
        val modLoaderTypeInt = when (modLoaderName) {
            "forge" -> ModLoader.MOD_LOADER_FORGE
            "fabric" -> ModLoader.MOD_LOADER_FABRIC
            "neoforge" -> ModLoader.MOD_LOADER_NEOFORGE
            else -> return null
        }
        return ModLoader(modLoaderTypeInt, modLoaderVersion, minecraft.version)
    }

    @Throws(IOException::class)
    private fun getDownloadUrl(fileMetadata: JsonObject): String {
        if (fileMetadata["modId"].isJsonNull || fileMetadata["id"].isJsonNull) throw IOException("Bad metadata schema!")
        val projectID = fileMetadata["modId"].asLong
        val fileID = fileMetadata["id"].asLong
        val response = mApiHandler.get("mods/$projectID/files/$fileID/download-url", JsonObject::class.java)
        if (response != null && !response["data"].isJsonNull) return response["data"].asString
        return String.format("https://edge.forgecdn.net/files/%s/%s/%s", fileID / 1000, fileID % 1000, fileMetadata["fileName"].asString)
    }

    @Throws(IOException::class)
    private fun checkRequiredFileFields(fileMetadata: JsonObject) {
        if (fileMetadata == null || fileMetadata.isJsonNull) throw IOException("File metadata is null!")
        val hasProjectId = fileMetadata.has("modId")
        val hasFileId = fileMetadata.has("id")
        val hasLength = fileMetadata.has("fileLength")
        if (!hasProjectId || !hasFileId || !hasLength) {
            val builder = StringBuilder("File metadata is mising the following fields:")
            if (!hasProjectId) builder.append(" modId")
            if (!hasFileId) builder.append(" id")
            if (!hasLength) builder.append(" fileLength")
            throw IOException(builder.toString())
        }
    }

    private fun getFile(projectID: Long, fileID: Long): JsonObject? {
        val response = mApiHandler.get("mods/$projectID/files/$fileID", JsonObject::class.java)
        return GsonJsonUtils.getJsonObjectSafe(response, "data")
    }

    private fun getSha1FromModData(`object`: JsonObject): String? {
        val hashes = GsonJsonUtils.getJsonArraySafe(`object`, "hashes") ?: return null
        for (jsonElement in hashes) {
            val jsonObject = GsonJsonUtils.getJsonObjectSafe(jsonElement)
            if (GsonJsonUtils.getIntSafe(jsonObject, "algo", -1) == ALGO_SHA_1) {
                return GsonJsonUtils.getStringSafe(jsonObject, "value")
            }
        }
        return null
    }

    private fun verifyManifest(manifest: CurseManifest): Boolean {
        if (manifest.manifestType != "minecraftModpack") return false
        if (manifest.manifestVersion != 1) return false
        if (manifest.minecraft == null) return false
        if (manifest.minecraft.version == null) return false
        if (manifest.minecraft.modLoaders == null) return false
        return manifest.minecraft.modLoaders.size >= 1
    }

    class CurseforgeSearchResult : SearchResult() {
        var previousOffset = 0
    }

    inner class CurseDownloader : Downloader(ProgressLayout.INSTALL_MODPACK) {
        @Throws(IOException::class, InterruptedException::class)
        fun start(curseManifest: CurseManifest, instanceDestination: File) {
            val taskMetadatas = ArrayList<AcquireableTaskMetadata>(curseManifest.files.size)
            for (file in curseManifest.files) {
                taskMetadatas.add(CurseTaskMetadata(file, instanceDestination))
            }
            runDownloads(taskMetadatas)
        }
    }

    inner class CurseTaskMetadata(
        private val mFile: CurseManifest.CurseFile,
        private val mInstanceDestination: File
    ) : AcquireableTaskMetadata(DownloadMirror.DOWNLOAD_CLASS_METADATA) {
        @Throws(IOException::class)
        override fun acquireMetadata() {
            val fileMetadata = getFile(mFile.projectID, mFile.fileID)!!
            checkRequiredFileFields(fileMetadata)
            val url = getDownloadUrl(fileMetadata)
            this.url = URL(url)
            this.path = File(mInstanceDestination, "mods/${URLDecoder.decode(FileUtils.getFileName(url), "UTF-8")}")
            FileUtils.ensureParentDirectorySilently(this.path)
            this.sha1Hash = getSha1FromModData(fileMetadata)
            this.size = fileMetadata["fileLength"].asLong
        }
    }

    companion object {
        private val sMcVersionPattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.?([0-9]+)?")
        private const val ALGO_SHA_1 = 1
        private const val CURSEFORGE_MC_GAME_ID = 432
        private const val CURSEFORGE_MODPACK_CLASS_ID = 4471
        private const val CURSEFORGE_MOD_CLASS_ID = 6
        private const val CURSEFORGE_SORT_RELEVANCY = 1
        private const val CURSEFORGE_PAGINATION_SIZE = 50
        private const val CURSEFORGE_PAGINATION_END_REACHED = -1
        private const val CURSEFORGE_PAGINATION_ERROR = -2
    }
}
