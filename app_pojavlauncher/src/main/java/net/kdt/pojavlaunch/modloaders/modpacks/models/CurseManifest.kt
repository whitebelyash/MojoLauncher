package net.kdt.pojavlaunch.modloaders.modpacks.models

class CurseManifest {
    var name: String? = null
    var version: String? = null
    var author: String? = null
    var manifestType: String? = null
    var manifestVersion = 0
    var files: Array<CurseFile> = emptyArray()
    var minecraft: CurseMinecraft? = null
    var overrides: String? = null

    class CurseFile {
        var projectID: Long = 0
        var fileID: Long = 0
        var required: Boolean = false
    }

    class CurseMinecraft {
        var version: String? = null
        var modLoaders: Array<CurseModLoader> = emptyArray()
    }

    class CurseModLoader {
        var id: String? = null
        var primary: Boolean = false
    }
}
