package net.kdt.pojavlaunch.modloaders

class ComparableVersionString : Comparable<ComparableVersionString> {
    private var major = 0
    private var minor = 0
    private var patch = 0
    private val original: String
    private val isValid: Boolean

    private constructor(str: String) {
        this.original = str
        this.isValid = false
    }

    constructor(original: String, major: Int, minor: Int, patch: Int) {
        this.major = major
        this.minor = minor
        this.patch = patch
        this.original = original
        this.isValid = true
    }

    override fun compareTo(str: ComparableVersionString): Int {
        if (!isValid) return str.proper.compareTo(original)
        if (major != str.major) return major.compareTo(str.major)
        if (minor != str.minor) return minor.compareTo(str.minor)
        if (patch != str.patch) return patch.compareTo(str.patch)
        return 0
    }

    fun getOriginal() = original

    val proper: String
        get() {
            if (!isValid) return original
            val sb = StringBuilder()
            sb.append(major)
            sb.append('.')
            sb.append(minor)
            if (patch != 0) {
                sb.append('.')
                sb.append(patch)
            }
            return sb.toString()
        }

    fun isValid() = isValid

    companion object {
        fun parse(str: String): ComparableVersionString {
            val split = str.split("\\.".toRegex())
            if (split.size < 2) return ComparableVersionString(str)
            return try {
                val major = split[0].toInt()
                val minor = split[1].toInt()
                val patch = if (split.size >= 3) split[2].toInt() else 0
                ComparableVersionString(str, major, minor, patch)
            } catch (e: NumberFormatException) {
                ComparableVersionString(str)
            }
        }
    }
}
