package net.kdt.pojavlaunch.modloaders.modpacks.api

import android.content.Context
import com.kdt.mcgui.ProgressLayout
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult
import java.io.File
import java.io.IOException

interface ModpackApi {
    fun searchMod(searchFilters: SearchFilters, previousPageResult: SearchResult?): SearchResult?

    fun searchMod(searchFilters: SearchFilters): SearchResult? {
        return searchMod(searchFilters, null)
    }

    fun getModDetails(item: ModItem): ModDetail?

    fun handleModpackInstallation(context: Context, modDetail: ModDetail, selectedVersion: Int) {
        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.global_waiting)
        PojavApplication.sExecutorService.execute {
            try {
                installModpack(modDetail, selectedVersion)
            } catch (e: IOException) {
                Tools.showErrorRemote(context, R.string.modpack_install_download_failed, e)
            }
        }
    }

    @Throws(IOException::class)
    fun installLocalModpack(modpackName: String, modpackFile: File, icon: String): ModLoader?

    @Throws(IOException::class)
    fun installModpack(modDetail: ModDetail, selectedVersion: Int): ModLoader?
}
