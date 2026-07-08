package net.kdt.pojavlaunch.tasks

import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.Instance
import java.io.File

object MoJsonExtras {
    fun normalizeVersionId(versionString: String): String {
        var vs = versionString
        val versionList = ExtraCore.getValue(ExtraConstants.RELEASE_TABLE) as? JVersionList
        if (versionList == null || versionList.versions == null) return vs
        if (Instance.VERSION_LATEST_RELEASE == vs) vs = versionList.latest["release"]
        if (Instance.VERSION_LATEST_SNAPSHOT == vs) vs = versionList.latest["snapshot"]
        return vs
    }

    fun getListedVersion(normalizedVersionString: String): JVersionList.Version? {
        val versionList = ExtraCore.getValue(ExtraConstants.RELEASE_TABLE) as? JVersionList ?: return null
        if (versionList.versions == null) return null
        for (version in versionList.versions) {
            if (version.id == normalizedVersionString) return version
        }
        return null
    }

    fun interface DoneListener {
        fun onDownloadDone(classpath: Array<File>)
        fun onDownloadFailed(throwable: Throwable)
    }
}
