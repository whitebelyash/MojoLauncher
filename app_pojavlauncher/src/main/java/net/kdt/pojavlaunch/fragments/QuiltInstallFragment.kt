package net.kdt.pojavlaunch.fragments

import net.kdt.pojavlaunch.modloaders.FabriclikeUtils
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy

class QuiltInstallFragment : FabriclikeInstallFragment(FabriclikeUtils.QUILT_UTILS, TAG) {
    companion object {
        const val TAG = "QuiltInstallFragment"
    }

    private var sTaskProxy: ModloaderListenerProxy? = null
}
