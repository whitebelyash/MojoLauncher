package net.kdt.pojavlaunch.fragments

import net.kdt.pojavlaunch.modloaders.FabriclikeUtils
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy

class FabricInstallFragment : FabriclikeInstallFragment(FabriclikeUtils.FABRIC_UTILS, TAG) {
    companion object {
        const val TAG = "FabricInstallFragment"
    }
}
