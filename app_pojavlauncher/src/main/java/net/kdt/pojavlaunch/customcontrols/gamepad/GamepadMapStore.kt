package net.kdt.pojavlaunch.customcontrols.gamepad

import android.util.Log
import com.google.gson.JsonParseException
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.FileUtils
import java.io.File
import java.io.IOException

object GamepadMapStore {
    private val STORE_FILE = File(Tools.DIR_DATA, "gamepad_map.json")
    private var sMapStore: GamepadMapStoreData? = null

    private fun createDefault(): GamepadMapStoreData {
        val mapStore = GamepadMapStoreData()
        mapStore.mInGameMap = GamepadMap.getDefaultGameMap()
        mapStore.mInMenuMap = GamepadMap.getDefaultMenuMap()
        return mapStore
    }

    private fun loadIfNecessary() {
        if (sMapStore == null) return
        load()
    }

    fun load() {
        var mapStore: GamepadMapStoreData? = null
        if (STORE_FILE.exists() && STORE_FILE.canRead()) {
            try {
                val storeFileContent = Tools.read(STORE_FILE)
                mapStore = Tools.GLOBAL_GSON.fromJson(storeFileContent, GamepadMapStoreData::class.java)
            } catch (e: JsonParseException) {
                Log.w("GamepadMapStore", "Map store failed to load!", e)
            } catch (e: IOException) {
                Log.w("GamepadMapStore", "Map store failed to load!", e)
            }
        }
        if (mapStore == null) mapStore = createDefault()
        sMapStore = mapStore
    }

    @Throws(IOException::class)
    fun save() {
        if (sMapStore == null) throw RuntimeException("Must load map store first!")
        FileUtils.ensureParentDirectory(STORE_FILE)
        val jsonData = Tools.GLOBAL_GSON.toJson(sMapStore)
        Tools.write(STORE_FILE, jsonData)
    }

    fun getGameMap(): GamepadMap {
        loadIfNecessary()
        return sMapStore!!.mInGameMap
    }

    fun getMenuMap(): GamepadMap {
        loadIfNecessary()
        return sMapStore!!.mInMenuMap
    }

    private class GamepadMapStoreData {
        var mInMenuMap: GamepadMap = GamepadMap()
        var mInGameMap: GamepadMap = GamepadMap()
    }
}
