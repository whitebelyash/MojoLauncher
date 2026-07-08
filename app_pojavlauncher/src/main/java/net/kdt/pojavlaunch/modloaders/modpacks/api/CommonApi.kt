package net.kdt.pojavlaunch.modloaders.modpacks.api

import android.util.Log
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.zip.ZipFile

class CommonApi(curseforgeApiKey: String) : ModpackApi {
    private val mCurseforgeApi: ModpackApi?
    private val mModrinthApi: ModpackApi = ModrinthApi()
    private val mModpackApis: Array<ModpackApi>

    init {
        mModpackApis = if ("DUMMY" == curseforgeApiKey) {
            mCurseforgeApi = null
            arrayOf(mModrinthApi)
        } else {
            mCurseforgeApi = CurseforgeApi(curseforgeApiKey)
            arrayOf(mModrinthApi, mCurseforgeApi)
        }
    }

    override fun searchMod(searchFilters: SearchFilters, previousPageResult: SearchResult?): SearchResult? {
        val commonApiSearchResult = previousPageResult as? CommonApiSearchResult
        val results = commonApiSearchResult?.searchResults ?: arrayOfNulls<SearchResult>(mModpackApis.size)
        var totalSize = 0
        val futures = arrayOfNulls<Future<*>?>(mModpackApis.size)
        for (i in mModpackApis.indices) {
            if (results[i] != null && results[i]!!.results.isEmpty()) continue
            if (previousPageResult != null && results[i] == null) continue
            futures[i] = PojavApplication.sExecutorService.submit(ApiDownloadTask(i, searchFilters, results[i]))
        }
        if (Thread.interrupted()) {
            cancelAllFutures(futures)
            return null
        }
        var hasSuccessful = false
        for (i in mModpackApis.indices) {
            val future = futures[i] ?: continue
            try {
                val searchResult = (future.get() as? SearchResult).also { results[i] = it }
                if (searchResult != null) hasSuccessful = true
                else continue
                totalSize += searchResult.totalResultCount
            } catch (e: Exception) {
                cancelAllFutures(futures)
                e.printStackTrace()
                return null
            }
        }
        if (!hasSuccessful) return null
        val filteredResults = ArrayList<Array<ModItem>>(results.size)
        for (result in results) {
            if (result == null) continue
            val searchResults = result.results
            if (searchResults.isEmpty()) continue
            filteredResults.add(searchResults)
        }
        filteredResults.trimToSize()
        if (Thread.interrupted()) return null
        val concatenatedItems = buildFusedResponse(filteredResults)
        if (Thread.interrupted()) return null
        val csr = commonApiSearchResult ?: CommonApiSearchResult()
        csr.searchResults = results
        csr.totalResultCount = totalSize
        csr.results = concatenatedItems
        return csr
    }

    override fun getModDetails(item: ModItem): ModDetail? {
        Log.i("CommonApi", "Invoking getModDetails on item.apiSource=${item.apiSource} item.title=${item.title}")
        return getModpackApi(item.apiSource)?.getModDetails(item)
    }

    @Throws(IOException::class)
    override fun installModpack(modDetail: ModDetail, selectedVersion: Int): ModLoader? {
        return getModpackApi(modDetail.apiSource)?.installModpack(modDetail, selectedVersion)
    }

    @Throws(IOException::class)
    fun installLocalModpack(modpackName: String, modpackFile: File, icon: String): ModLoader? {
        return when (checkModpack(modpackFile)) {
            PACK_MODRINTH -> mModrinthApi.installLocalModpack(modpackName, modpackFile, icon)
            PACK_CURSEFORGE -> mCurseforgeApi?.installLocalModpack(modpackName, modpackFile, icon)
            PACK_UNDEFINED -> {
                modpackFile.delete()
                null
            }
            else -> null
        }
    }

    private fun getModpackApi(apiSource: Int): ModpackApi? {
        return when (apiSource) {
            Constants.SOURCE_MODRINTH -> mModrinthApi
            Constants.SOURCE_CURSEFORGE -> mCurseforgeApi ?: return null
            else -> throw UnsupportedOperationException("Unknown API source: $apiSource")
        }
    }

    companion object {
        const val PACK_MODRINTH: Byte = 1
        const val PACK_CURSEFORGE: Byte = 2
        const val PACK_UNDEFINED: Byte = 0

        fun checkModpack(outFile: File): Short {
            return try {
                ZipFile(outFile).use { zipFile ->
                    val modrinth = zipFile.getEntry("modrinth.index.json")
                    val curseforge = zipFile.getEntry("manifest.json")
                    when {
                        modrinth != null -> PACK_MODRINTH.toShort()
                        curseforge != null -> PACK_CURSEFORGE.toShort()
                        else -> PACK_UNDEFINED.toShort()
                    }
                }
            } catch (e: Exception) {
                -1
            }
        }
    }

    private fun buildFusedResponse(modMatrix: List<Array<ModItem>>): Array<ModItem> {
        var totalSize = 0
        for (array in modMatrix) totalSize += array.size
        val fusedItems = arrayOfNulls<ModItem>(totalSize)
        var mergedIndex = 0
        var maxLength = 0
        for (array in modMatrix) if (array.size > maxLength) maxLength = array.size
        for (i in 0 until maxLength) {
            for (matrix in modMatrix) {
                if (i < matrix.size) {
                    fusedItems[mergedIndex] = matrix[i]
                    mergedIndex++
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return fusedItems as Array<ModItem>
    }

    private fun cancelAllFutures(futures: Array<Future<*>?>) {
        for (future in futures) future?.cancel(true)
    }

    private inner class ApiDownloadTask(
        private val mModApi: Int,
        private val mSearchFilters: SearchFilters,
        private val mPreviousPageResult: SearchResult?
    ) : Callable<SearchResult?> {
        override fun call(): SearchResult? = mModpackApis[mModApi].searchMod(mSearchFilters, mPreviousPageResult)
    }

    inner class CommonApiSearchResult : SearchResult() {
        var searchResults = arrayOfNulls<SearchResult>(mModpackApis.size)
    }
}
