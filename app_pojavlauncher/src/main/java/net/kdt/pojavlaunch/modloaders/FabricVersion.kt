package net.kdt.pojavlaunch.modloaders

open class FabricVersion {
    var version: String? = null
    var stable: Boolean = false

    class LoaderDescriptor : FabricVersion() {
        var loader: FabricVersion? = null

        override fun toString(): String = loader?.toString() ?: "null"
    }

    override fun toString(): String = version ?: ""
}
