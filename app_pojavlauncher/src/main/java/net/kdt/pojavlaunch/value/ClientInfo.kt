package net.kdt.pojavlaunch.value

import androidx.annotation.Keep

@Keep
open class ClientInfo {
    var sha1: String? = null
    var size = -1
    var url: String? = null
}
