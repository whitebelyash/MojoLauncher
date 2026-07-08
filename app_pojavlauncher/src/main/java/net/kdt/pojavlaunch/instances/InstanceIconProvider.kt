package net.kdt.pojavlaunch.instances

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.NonNull
import androidx.core.content.res.ResourcesCompat
import git.artdeell.mojo.R
import java.io.File
import java.util.*

object InstanceIconProvider {
    const val FALLBACK_ICON_NAME = "default"

    private val sIconCache = HashMap<Int, Drawable>()
    private val sStaticIconCache = HashMap<String, Drawable>()
    private val sStaticIcons = HashMap<String, Int>()

    init {
        sStaticIcons["default"] = R.drawable.ic_mojo_full
        sStaticIcons["fabric"] = R.drawable.ic_fabric
        sStaticIcons["quilt"] = R.drawable.ic_quilt
        sStaticIcons["forge"] = R.drawable.ic_forge
        sStaticIcons["neoforge"] = R.drawable.ic_neoforge
    }

    @NonNull
    fun fetchIcon(resources: Resources, @NonNull instance: DisplayInstance): Drawable {
        val identityHashCode = System.identityHashCode(instance)
        val cachedIcon = sIconCache[identityHashCode]
        if (cachedIcon != null) return cachedIcon

        val instanceIcon = fetchInstanceFileIcon(resources, identityHashCode, instance.getInstanceIconLocation())
        if (instanceIcon != null) return instanceIcon

        return fetchStaticIcon(resources, identityHashCode, instance.icon)
    }

    fun dropIcon(@NonNull key: Instance) {
        sIconCache.remove(System.identityHashCode(key))
    }

    private fun fetchInstanceFileIcon(
        resources: Resources,
        identityHash: Int,
        iconLocation: File
    ): Drawable? {
        if (!iconLocation.isFile || !iconLocation.canRead()) return null
        val iconBitmap = BitmapFactory.decodeFile(iconLocation.absolutePath) ?: return null
        val iconDrawable = BitmapDrawable(resources, iconBitmap)
        sIconCache[identityHash] = iconDrawable
        return iconDrawable
    }

    private fun fetchStaticIcon(
        resources: Resources,
        identityHash: Int,
        icon: String?
    ): Drawable {
        var staticIcon = sStaticIconCache[icon]
        if (staticIcon == null) {
            if (icon != null) staticIcon = getStaticIcon(resources, icon)
            if (staticIcon == null) staticIcon = fetchFallbackIcon(resources)
            sStaticIconCache[icon] = staticIcon
        }
        sIconCache[identityHash] = staticIcon
        return staticIcon
    }

    @NonNull
    private fun fetchFallbackIcon(resources: Resources): Drawable {
        var fallbackIcon = sStaticIconCache[FALLBACK_ICON_NAME]
        if (fallbackIcon == null) {
            fallbackIcon = Objects.requireNonNull(getStaticIcon(resources, FALLBACK_ICON_NAME))
            sStaticIconCache[FALLBACK_ICON_NAME] = fallbackIcon
        }
        return fallbackIcon
    }

    private fun getStaticIcon(resources: Resources, @NonNull icon: String): Drawable? {
        val staticIconResource = getStaticIconResource(icon)
        if (staticIconResource == -1) return null
        return ResourcesCompat.getDrawable(resources, staticIconResource, null)
    }

    private fun getStaticIconResource(icon: String): Int {
        return sStaticIcons[icon] ?: -1
    }

    fun hasStaticIcon(name: String?): Boolean {
        return sStaticIcons.containsKey(name)
    }
}
