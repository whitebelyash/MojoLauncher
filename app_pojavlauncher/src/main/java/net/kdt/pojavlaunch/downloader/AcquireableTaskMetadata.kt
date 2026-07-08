package net.kdt.pojavlaunch.downloader

import java.io.IOException

abstract class AcquireableTaskMetadata(mirrorType: Int) : TaskMetadata(null, null, mirrorType) {
    @Throws(IOException::class)
    abstract override fun acquireMetadata()
}
