package net.kdt.pojavlaunch.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import net.kdt.pojavlaunch.Tools
import git.artdeell.mojo.R
import java.io.File

object RendererCompatUtil {
    private var sCompatibleRenderers: RenderersList? = null

    fun checkVulkanSupport(packageManager: PackageManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) &&
                    packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
        } else false
    }

    fun getCompatibleRenderers(context: Context): RenderersList {
        if (sCompatibleRenderers != null) return sCompatibleRenderers!!
        val resources = context.resources
        val defaultRenderers = resources.getStringArray(R.array.renderer_values)
        val defaultRendererNames = resources.getStringArray(R.array.renderer)
        val deviceHasVulkan = checkVulkanSupport(context.packageManager)
        val deviceCompatibleMesa = Build.VERSION.SDK_INT >= 29
        val deviceHasOpenGLES3 = JREUtils.getDetectedVersion() >= 3
        val appHasLtw = File(Tools.NATIVE_LIB_DIR, "libltw.so").exists()
        val rendererIds = ArrayList<String>(defaultRenderers.size)
        val rendererNames = ArrayList<String>(defaultRendererNames.size)
        for (i in defaultRenderers.indices) {
            val rendererId = defaultRenderers[i]
            if (rendererId.contains("vulkan") && !deviceHasVulkan) continue
            if (rendererId.contains("zink") && !deviceCompatibleMesa) continue
            if (rendererId.contains("freedreno") && (!GLInfoUtils.getGlInfo().isAdreno() || !deviceCompatibleMesa)) continue
            if (rendererId.contains("ltw") && (!deviceHasOpenGLES3 || !appHasLtw)) continue
            rendererIds.add(rendererId)
            rendererNames.add(defaultRendererNames[i])
        }
        sCompatibleRenderers = RenderersList(rendererIds, rendererNames.toTypedArray())
        return sCompatibleRenderers!!
    }

    fun checkRendererCompatible(context: Context, rendererName: String): Boolean {
        return getCompatibleRenderers(context).rendererIds.contains(rendererName)
    }

    fun releaseRenderersCache() {
        sCompatibleRenderers = null
        System.gc()
    }

    class RenderersList(
        val rendererIds: List<String>,
        val rendererDisplayNames: Array<String>
    )
}
