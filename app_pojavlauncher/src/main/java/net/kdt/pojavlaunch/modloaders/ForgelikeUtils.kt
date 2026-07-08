package net.kdt.pojavlaunch.modloaders

import android.util.Log
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.InstanceInstaller
import net.kdt.pojavlaunch.utils.DownloadUtils
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.util.List
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

abstract class ForgelikeUtils private constructor(
    private val mName: String,
    private val mCachePrefix: String,
    private val mIconName: String,
    private val mVersionResolver: String,
    private val mMetadataUrl: String,
    private val mInstallerUrl: String,
    private val mVersionOrderInversed: Boolean
) {
    @Throws(IOException::class)
    fun downloadVersions(): List<String>? {
        val saxParser: SAXParser = try {
            val parserFactory = SAXParserFactory.newInstance()
            parserFactory.newSAXParser()
        } catch (e: SAXException) {
            e.printStackTrace()
            return null
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
            return null
        }
        return try {
            DownloadUtils.downloadStringCached(mMetadataUrl, "${mCachePrefix}_versions") { input ->
                try {
                    val handler = ForgelikeVersionListHandler()
                    saxParser.parse(InputSource(StringReader(input)), handler)
                    handler.versions
                } catch (e: SAXException) {
                    throw DownloadUtils.ParseException(e)
                } catch (e: IOException) {
                    throw DownloadUtils.ParseException(e)
                }
            }
        } catch (e: DownloadUtils.ParseException) {
            e.printStackTrace()
            null
        }
    }

    fun getInstallerUrl(version: String) = String.format(mInstallerUrl, version)

    @Throws(IOException::class)
    fun createInstaller(gameVersion: String, modLoaderVersion: String): InstanceInstaller? {
        val versions = downloadVersions() ?: return null
        val versionStart = String.format(mVersionResolver, gameVersion, modLoaderVersion)
        for (versionName in versions) {
            if (!versionName.startsWith(versionStart)) continue
            return createInstaller(versionName)
        }
        return null
    }

    @Throws(IOException::class)
    fun createInstaller(fullVersion: String): InstanceInstaller {
        val downloadUrl = getInstallerUrl(fullVersion)
        val hash = DownloadUtils.downloadString("$downloadUrl.sha1")
        val installerLocation = File(Tools.DIR_CACHE, "$mCachePrefix-installer-$fullVersion.jar")
        val instanceInstaller = InstanceInstaller()
        instanceInstaller.commandLineArgs = java.util.List.of(
            "-Duser.language=en", "-Duser.country=US",
            "-javaagent:${Tools.DIR_DATA}/forge_installer/forge_installer.jar"
        )
        instanceInstaller.installerJar = installerLocation.absolutePath
        instanceInstaller.installerSha1 = hash
        instanceInstaller.installerDownloadUrl = downloadUrl
        return instanceInstaller
    }

    fun getName() = mName
    fun getIconName() = mIconName
    abstract fun processVersionString(version: String): String
    abstract fun shouldSkipVersion(version: String): Boolean
    fun isVersionOrderInversed() = mVersionOrderInversed

    companion object {
        val FORGE_UTILS: ForgelikeUtils = ForgeUtils()
        val NEOFORGE_UTILS: ForgelikeUtils = NeoforgeUtils()

        private fun getMcVersionForNeoVersion(neoVersion: String): String {
            return try {
                val firstIndex = neoVersion.indexOf('.')
                val secondIndex = neoVersion.indexOf('.', firstIndex + 1)
                if (firstIndex == -1 || secondIndex == -1) {
                    Log.e("NeoforgeUtils", "Failed to parse neoforge version: $neoVersion; not enough '.' found")
                    return neoVersion
                }
                "1.${neoVersion.substring(0, secondIndex)}"
            } catch (e: StringIndexOutOfBoundsException) {
                Log.e("NeoforgeUtils", "Failed to parse neoforge version: $neoVersion", e)
                neoVersion
            }
        }
    }

    private class ForgeUtils : ForgelikeUtils(
        "Forge", "forge", "forge", "%1\$s-%2\$s",
        "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml",
        "https://maven.minecraftforge.net/net/minecraftforge/forge/%1\$s/forge-%1\$s-installer.jar",
        false
    ) {
        override fun processVersionString(version: String): String {
            val dashIndex = version.indexOf("-")
            return version.substring(0, dashIndex)
        }

        override fun shouldSkipVersion(version: String) = false
    }

    private class NeoforgeUtils : ForgelikeUtils(
        "NeoForge", "neoforge", "neoforge", "%2\$s",
        "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml",
        "https://maven.neoforged.net/releases/net/neoforged/neoforge/%1\$s/neoforge-%1\$s-installer.jar",
        true
    ) {
        override fun processVersionString(version: String) =
            ComparableVersionString.parse(getMcVersionForNeoVersion(version)).proper

        override fun shouldSkipVersion(version: String) = version.startsWith("0")
    }
}
