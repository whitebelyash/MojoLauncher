package net.kdt.pojavlaunch.modloaders.modpacks.api

import net.kdt.pojavlaunch.instances.InstanceInstaller
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils
import net.kdt.pojavlaunch.modloaders.ForgelikeUtils
import java.io.IOException

class ModLoader(
    val modLoaderType: Int,
    val modLoaderVersion: String,
    val gameVersion: String
) {
    fun getVersionId(): String? {
        return when (modLoaderType) {
            MOD_LOADER_FORGE -> "$gameVersion-forge-$modLoaderVersion"
            MOD_LOADER_FABRIC -> "fabric-loader-$modLoaderVersion-$gameVersion"
            MOD_LOADER_QUILT -> "quilt-loader-$modLoaderVersion-$gameVersion"
            MOD_LOADER_NEOFORGE -> "neoforge-$modLoaderVersion"
            MOD_LOADER_LEGACY_FABRIC -> "legacy-fabric-loader-$modLoaderVersion-$gameVersion"
            else -> null
        }
    }

    @Throws(IOException::class)
    fun installHeadlessly(): String? {
        return when (modLoaderType) {
            MOD_LOADER_FABRIC -> FabriclikeUtils.FABRIC_UTILS.install(gameVersion, modLoaderVersion)
            MOD_LOADER_QUILT -> FabriclikeUtils.QUILT_UTILS.install(gameVersion, modLoaderVersion)
            MOD_LOADER_LEGACY_FABRIC -> FabriclikeUtils.LEGACY_FABRIC_UTILS.install(gameVersion, modLoaderVersion)
            MOD_LOADER_FORGE, MOD_LOADER_NEOFORGE -> null
            else -> null
        }
    }

    @Throws(IOException::class)
    fun createInstaller(): InstanceInstaller? {
        return when (modLoaderType) {
            MOD_LOADER_NEOFORGE -> ForgelikeUtils.NEOFORGE_UTILS.createInstaller(gameVersion, modLoaderVersion)
            MOD_LOADER_FORGE -> ForgelikeUtils.FORGE_UTILS.createInstaller(gameVersion, modLoaderVersion)
            MOD_LOADER_QUILT, MOD_LOADER_FABRIC, MOD_LOADER_LEGACY_FABRIC -> null
            else -> null
        }
    }

    fun requiresGuiInstallation(): Boolean {
        return when (modLoaderType) {
            MOD_LOADER_NEOFORGE, MOD_LOADER_FORGE -> true
            MOD_LOADER_FABRIC, MOD_LOADER_QUILT, MOD_LOADER_LEGACY_FABRIC -> false
            else -> false
        }
    }

    companion object {
        const val MOD_LOADER_FORGE = 0
        const val MOD_LOADER_FABRIC = 1
        const val MOD_LOADER_QUILT = 2
        const val MOD_LOADER_NEOFORGE = 3
        const val MOD_LOADER_LEGACY_FABRIC = 4
    }
}
