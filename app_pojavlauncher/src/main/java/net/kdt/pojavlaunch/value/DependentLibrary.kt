package net.kdt.pojavlaunch.value

import androidx.annotation.Keep
import java.util.HashMap

@Keep
open class DependentLibrary {
    var rules: Array<MoJsonRule>? = null
    var name: String? = null
    var downloads: LibraryDownloads? = null
    var url: String? = null
    @Transient
    var replaced = false
    var natives: MutableMap<String, String>? = null
    var extract: ExtractSettings? = null

    @Keep
    class LibraryDownloads {
        var artifact: LibraryArtifact? = null
        var classifiers: LibraryClassifierMap? = null

        constructor(artifact: LibraryArtifact?) {
            this.artifact = artifact
        }

        @Keep
        constructor() {}
    }

    class LibraryClassifierMap : HashMap<String, LibraryArtifact>()
}
