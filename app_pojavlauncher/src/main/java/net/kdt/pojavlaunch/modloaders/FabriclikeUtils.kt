package net.kdt.pojavlaunch.modloaders

import com.google.gson.JsonSyntaxException
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.DownloadUtils
import net.kdt.pojavlaunch.utils.FileUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder

class FabriclikeUtils private constructor(
    private val mApiUrl: String,
    private val mCachePrefix: String,
    private val mName: String,
    private val mIconName: String
) {
    @Throws(IOException::class)
    fun downloadGameVersions(): Array<FabricVersion>? {
        return try {
            DownloadUtils.downloadStringCached(
                String.format(GAME_METADATA_URL, mApiUrl), "${mCachePrefix}_game_versions",
                Companion::deserializeRawVersions
            )
        } catch (_: DownloadUtils.ParseException) {
            null
        }
    }

    @Throws(IOException::class)
    fun downloadLoaderVersions(gameVersion: String): Array<FabricVersion>? {
        return try {
            val urlEncodedGameVersion = URLEncoder.encode(gameVersion, "UTF-8")
            DownloadUtils.downloadStringCached(
                String.format(LOADER_METADATA_URL, mApiUrl, urlEncodedGameVersion),
                "${mCachePrefix}_loader_versions.$urlEncodedGameVersion"
            ) { input ->
                try {
                    deserializeLoaderVersions(input)
                } catch (e: JSONException) {
                    throw DownloadUtils.ParseException(e)
                }
            }
        } catch (e: DownloadUtils.ParseException) {
            e.printStackTrace()
            null
        }
    }

    fun createJsonDownloadUrl(gameVersion: String, loaderVersion: String): String {
        val encodedGame = URLEncoder.encode(gameVersion, "UTF-8")
        val encodedLoader = URLEncoder.encode(loaderVersion, "UTF-8")
        return String.format(JSON_DOWNLOAD_URL, mApiUrl, encodedGame, encodedLoader)
    }

    fun getName() = mName
    fun getIconName() = mIconName

    @Throws(IOException::class)
    fun install(gameVersion: String, loaderVersion: String): String? {
        val fabricJson = DownloadUtils.downloadString(createJsonDownloadUrl(gameVersion, loaderVersion))
        val versionId = try {
            JSONObject(fabricJson).getString("id")
        } catch (e: JSONException) {
            e.printStackTrace()
            return null
        }
        val versionJsonDir = File(Tools.DIR_HOME_VERSION, versionId)
        val versionJsonFile = File(versionJsonDir, "$versionId.json")
        FileUtils.ensureDirectory(versionJsonDir)
        Tools.write(versionJsonFile, fabricJson)
        return versionId
    }

    companion object {
        val FABRIC_UTILS = FabriclikeUtils("https://meta.fabricmc.net/v2", "fabric", "Fabric", "fabric")
        val QUILT_UTILS = FabriclikeUtils("https://meta.quiltmc.org/v3", "quilt", "Quilt", "quilt")
        val LEGACY_FABRIC_UTILS = FabriclikeUtils("https://meta.legacyfabric.net/v2", "legacy_fabric", "Legacy Fabric", "fabric")

        private const val LOADER_METADATA_URL = "%s/versions/loader/%s"
        private const val GAME_METADATA_URL = "%s/versions/game"
        private const val JSON_DOWNLOAD_URL = "%s/versions/loader/%s/%s/profile/json"

        @Throws(JSONException::class)
        private fun deserializeLoaderVersions(input: String): Array<FabricVersion> {
            val jsonArray = JSONArray(input)
            val fabricVersions = arrayOfNulls<FabricVersion>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i).getJSONObject("loader")
                val fabricVersion = FabricVersion()
                fabricVersion.version = jsonObject.getString("version")
                fabricVersion.stable = if (jsonObject.has("stable")) jsonObject.getBoolean("stable")
                else !fabricVersion.version!!.contains("beta")
                fabricVersions[i] = fabricVersion
            }
            @Suppress("UNCHECKED_CAST")
            return fabricVersions as Array<FabricVersion>
        }

        @Throws(DownloadUtils.ParseException::class)
        private fun deserializeRawVersions(jsonArrayIn: String): Array<FabricVersion> {
            return try {
                Tools.GLOBAL_GSON.fromJson(jsonArrayIn, Array<FabricVersion>::class.java)
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
                throw DownloadUtils.ParseException(null)
            }
        }
    }
}
