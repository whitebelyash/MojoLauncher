package net.kdt.pojavlaunch

import android.os.Build

object Architecture {
    const val UNSUPPORTED_ARCH = -1
    const val ARCH_ARM64 = 0x1
    const val ARCH_ARM = 0x2
    const val ARCH_X86 = 0x4
    const val ARCH_X86_64 = 0x8

    const val ADDRESS_SPACE_LIMIT_32_BIT = 0xbfffffffL
    const val ADDRESS_SPACE_LIMIT_64_BIT = 0x7fffffffffL

    fun getAddressSpaceLimit(): Long {
        return if (is64BitsDevice()) ADDRESS_SPACE_LIMIT_64_BIT else ADDRESS_SPACE_LIMIT_32_BIT
    }

    fun is64BitsDevice(): Boolean {
        return Build.SUPPORTED_64_BIT_ABIS.size != 0
    }

    fun is32BitsDevice(): Boolean {
        return !is64BitsDevice()
    }

    fun getDeviceArchitecture(): Int {
        if (isx86Device()) {
            return if (is64BitsDevice()) ARCH_X86_64 else ARCH_X86
        }
        return if (is64BitsDevice()) ARCH_ARM64 else ARCH_ARM
    }

    fun isx86Device(): Boolean {
        val ABI = if (is64BitsDevice()) Build.SUPPORTED_64_BIT_ABIS else Build.SUPPORTED_32_BIT_ABIS
        val comparedArch = if (is64BitsDevice()) ARCH_X86_64 else ARCH_X86
        for (str in ABI) {
            if (archAsInt(str) == comparedArch) return true
        }
        return false
    }

    fun archAsInt(arch: String): Int {
        val lower = arch.lowercase().trim { it <= ' ' }.replace(" ", "")
        return when {
            lower.contains("arm64") || lower == "aarch64" -> ARCH_ARM64
            lower.contains("arm") || lower == "aarch32" -> ARCH_ARM
            lower.contains("x86_64") || lower.contains("amd64") -> ARCH_X86_64
            lower.contains("x86") || (lower.startsWith("i") && lower.endsWith("86")) -> ARCH_X86
            else -> UNSUPPORTED_ARCH
        }
    }

    fun archAsString(arch: Int): String {
        return when (arch) {
            ARCH_ARM64 -> "arm64"
            ARCH_ARM -> "arm"
            ARCH_X86_64 -> "x86_64"
            ARCH_X86 -> "x86"
            else -> "UNSUPPORTED_ARCH"
        }
    }
}
