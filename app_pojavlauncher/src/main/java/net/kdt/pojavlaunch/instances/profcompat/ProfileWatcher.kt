package net.kdt.pojavlaunch.instances.profcompat

import android.content.res.AssetManager
import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.FileReader
import java.io.IOException

object ProfileWatcher {
    private val sLauncherProfiles = File(Tools.DIR_GAME_NEW, "launcher_profiles.json")

    @Throws(IOException::class)
    fun consumePendingVersion(assetManager: AssetManager): String? {
        val store: Profiles
        FileReader(sLauncherProfiles).use { fileReader ->
            store = Tools.GLOBAL_GSON.fromJson(fileReader, Profiles::class.java)
        }
        val profiles = store.profiles ?: return null
        var versionId: String? = null
        for ((key, entry) in profiles) {
            if ("(Default)" == key) continue
            versionId = entry.lastVersionId
            if (versionId != null) break
        }
        installDefaultProfiles(assetManager)
        return versionId
    }

    @Throws(IOException::class)
    fun installDefaultProfiles(assetManager: AssetManager) {
        Tools.copyAssetFile(assetManager, "launcher_profiles.json", sLauncherProfiles, true)
    }
}
