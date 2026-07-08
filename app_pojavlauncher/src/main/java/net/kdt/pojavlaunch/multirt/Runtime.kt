package net.kdt.pojavlaunch.multirt

import java.util.Objects

data class Runtime(
    val name: String,
    val versionString: String?,
    val arch: String?,
    val javaVersion: Int
) {
    constructor(name: String) : this(name, null, null, 0)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val runtime = other as Runtime
        return name == runtime.name
    }

    override fun hashCode(): Int = Objects.hash(name)
}
