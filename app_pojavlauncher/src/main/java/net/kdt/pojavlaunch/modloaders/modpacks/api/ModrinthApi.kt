package net.kdt.pojavlaunch.modloaders.modpacks.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.kdt.mcgui.ProgressLayout
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.downloader.Downloader
import net.kdt.pojavlaunch.downloader.TaskMetadata
import net.kdt.pojavlaunch.mirrors.DownloadMirror
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModrinthIndex
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.ZipUtils
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.ArrayList
import java.util.HashMap
import java.util.zip.ZipFile

class ModrinthApi : ModpackApi {
    private val mApiHandler = ApiHandler("https://api.modrinth.com/v2")

    override fun searchMod(searchFilters: SearchFilters, previousPageResult: SearchResult?): SearchResult? {
        val modrinthSearchResult = previousPageResult as? ModrinthSearchResult
        if (modrinthSearchResult != null && modrinthSearchResult.previousOffset >= modrinthSearchResult.totalResultCount) {
            val emptyResult = ModrinthSearchResult()
            emptyResult.results = arrayOf()
            emptyResult.totalResultCount = modrinthSearchResult.totalResultCount
            emptyResult.previousOffset = modrinthSearchResult.previousOffset
            return emptyResult
        }
        val params = HashMap<String, Any>()
        val facetString = StringBuilder()
        facetString.append("[")
        facetString.append(String.format("[\"project_type:%s\"]", if (searchFilters.isModpack) "modpack" else "mod"))
        if (!searchFilters.mcVersion.isNullOrEmpty()) facetString.append(",[\"versions:${searchFilters.mcVersion}\"]")
        facetString.append("]")
        params["facets"] = facetString.toString()
        params["query"] = searchFilters.name
        params["limit"] = 50
        params["index"] = "relevance"
        if (modrinthSearchResult != null) params["offset"] = modrinthSearchResult.previousOffset
        val response = mApiHandler.get("search", params, JsonObject::class.java) ?: return null
        val responseHits = response.getAsJsonArray("hits") ?: return null
        val items = arrayOfNulls<ModItem>(responseHits.size())
        for (i in 0 until responseHits.size()) {
            val hit = responseHits[i].asJsonObject
            items[i] = ModItem(
                Constants.SOURCE_MODRINTH,
                hit["project_type"].asString == "modpack",
                hit["project_id"].asString,
                hit["title"].asString,
                hit["description"].asString,
                hit["icon_url"].asString
            )
        }
        val msr = modrinthSearchResult ?: ModrinthSearchResult()
        msr.previousOffset += responseHits.size()
        @Suppress("UNCHECKED_CAST")
        msr.results = items as Array<ModItem>
        msr.totalResultCount = response["total_hits"].asInt
        return msr
    }

    override fun getModDetails(item: ModItem): ModDetail? {
        val response = mApiHandler.get("project/${item.id}/version", JsonArray::class.java) ?: return null
        println(response)
        val names = arrayOfNulls<String>(response.size())
        val mcNames = arrayOfNulls<String>(response.size())
        val urls = arrayOfNulls<String>(response.size())
        val hashes = arrayOfNulls<String>(response.size())
        for (i in 0 until response.size()) {
            val version = response[i].asJsonObject
            names[i] = version["name"].asString
            mcNames[i] = version["game_versions"].asJsonArray[0].asString
            urls[i] = version["files"].asJsonArray[0].asJsonObject["url"].asString
            val hashesMap = version.getAsJsonArray("files")[0].asJsonObject["hashes"].asJsonObject
            hashes[i] = if (hashesMap == null || hashesMap["sha1"] == null) null else hashesMap["sha1"].asString
        }
        return ModDetail(item, names, mcNames, urls, hashes)
    }

    @Throws(IOException::class)
    override fun installModpack(modDetail: ModDetail, selectedVersion: Int): ModLoader? {
        return ModpackInstaller.downloadModpack(modDetail, selectedVersion, this::installMrpack)
    }

    @Throws(IOException::class)
    override fun installLocalModpack(modpackName: String, modpackFile: File, icon: String): ModLoader? {
        return ModpackInstaller.installModpack(modpackName, modpackName, modpackFile, icon, this::installMrpack)
    }

    private fun installMrpack(mrpackFile: File, instanceDestination: File): ModLoader? {
        ZipFile(mrpackFile).use { modpackZipFile ->
            val modrinthIndex = Tools.GLOBAL_GSON.fromJson(
                Tools.read(ZipUtils.getEntryStream(modpackZipFile, "modrinth.index.json")),
                ModrinthIndex::class.java
            )
            try {
                ModrinthDownloader().startDownloads(modrinthIndex.files, instanceDestination)
            } catch (e: InterruptedException) {
                throw IOException("NIY: InterruptedException", e)
            }
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.modpack_download_applying_overrides, 1, 2)
            ZipUtils.zipExtract(modpackZipFile, "overrides/", instanceDestination)
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 50, R.string.modpack_download_applying_overrides, 2, 2)
            ZipUtils.zipExtract(modpackZipFile, "client-overrides/", instanceDestination)
            return createInfo(modrinthIndex)
        }
    }

    companion object {
        private fun createInfo(modrinthIndex: ModrinthIndex?): ModLoader? {
            if (modrinthIndex == null) return null
            val dependencies = modrinthIndex.dependencies
            val mcVersion = dependencies["minecraft"] ?: return null
            var modLoaderVersion: String?
            modLoaderVersion = dependencies["forge"]
            if (modLoaderVersion != null) return ModLoader(ModLoader.MOD_LOADER_FORGE, modLoaderVersion, mcVersion)
            modLoaderVersion = dependencies["fabric-loader"]
            if (modLoaderVersion != null) return ModLoader(ModLoader.MOD_LOADER_FABRIC, modLoaderVersion, mcVersion)
            modLoaderVersion = dependencies["quilt-loader"]
            if (modLoaderVersion != null) return ModLoader(ModLoader.MOD_LOADER_QUILT, modLoaderVersion, mcVersion)
            modLoaderVersion = dependencies["neoforge"]
            if (modLoaderVersion != null) return ModLoader(ModLoader.MOD_LOADER_NEOFORGE, modLoaderVersion, mcVersion)
            return null
        }
    }

    class ModrinthSearchResult : SearchResult() {
        var previousOffset = 0
    }

    class ModrinthDownloader : Downloader(ProgressLayout.INSTALL_MODPACK) {
        @Throws(IOException::class, InterruptedException::class)
        fun startDownloads(indexFiles: Array<ModrinthIndex.ModrinthIndexFile>, instanceDestination: File) {
            val absoluteInstancePath = instanceDestination.absolutePath
            val taskMetadatas = ArrayList<TaskMetadata>(indexFiles.size)
            for (file in indexFiles) {
                val targetPath = File(instanceDestination, file.path)
                if (!targetPath.absolutePath.startsWith(absoluteInstancePath)) throw IOException("Bad path!")
                FileUtils.ensureParentDirectory(targetPath)
                taskMetadatas.add(
                    TaskMetadata(
                        targetPath, URL(file.downloads[0]), file.fileSize.toLong(),
                        file.hashes.sha1, DownloadMirror.DOWNLOAD_CLASS_NONE
                    )
                )
            }
            runDownloads(taskMetadatas)
        }
    }
}
