package net.kdt.pojavlaunch.downloader

import java.io.File
import java.net.URL

open class TaskMetadata {
    var path: File? = null
    var url: URL? = null
    val mirrorType: Int
    var size: Long = -1
    var sha1Hash: String? = null

    constructor(path: File?, url: URL?, mirrorType: Int) {
        this.path = path
        this.url = url
        this.mirrorType = mirrorType
    }

    constructor(path: File?, url: URL?, size: Long, hash: String?, mirrorType: Int) : this(path, url, mirrorType) {
        this.sha1Hash = hash
        this.size = size
    }

    open fun acquireMetadata() {
        // no-op by default
    }

    override fun toString(): String {
        return "TaskMetadata{\nurl=$url;\npath=$path\nhash=$sha1Hash;\nsize=$size\n}"
    }
}
