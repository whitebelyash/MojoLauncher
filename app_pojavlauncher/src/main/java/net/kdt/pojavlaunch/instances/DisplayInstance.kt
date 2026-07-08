package net.kdt.pojavlaunch.instances

import java.io.File

open class DisplayInstance {
    @Transient
    @JvmField var mInstanceRoot: File? = null
    @JvmField var name: String? = null
    @JvmField var versionId: String? = null
    @JvmField var icon: String? = null

    protected open fun sanitize() {
        sanitizeIcon()
    }

    protected constructor()

    protected fun getInstanceIconLocation(): File {
        return File(mInstanceRoot, "icon.webp")
    }

    private fun sanitizeIcon() {
        if (!InstanceIconProvider.hasStaticIcon(icon)) {
            icon = InstanceIconProvider.FALLBACK_ICON_NAME
        }
    }
}
