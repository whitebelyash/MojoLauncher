package net.kdt.pojavlaunch.value

import java.util.HashMap

class SubstitutionMap {
    var libraries: LibraryMap? = null
    var artifactMapping: MutableMap<String, String>? = null

    fun findSubstitution(name: String): LibrarySubstitution? {
        if (!name.startsWith("org.lwjgl") && !name.startsWith("net.java.jinput")) return null

        val library = libraries?.get(name)
        if (library != null) return library
        val mapping = artifactMapping?.get(name) ?: return null
        return libraries?.get(mapping)
    }

    class LibraryMap : HashMap<String, LibrarySubstitution>()
}
