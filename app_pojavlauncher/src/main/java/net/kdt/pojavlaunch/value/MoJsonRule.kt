package net.kdt.pojavlaunch.value

import net.kdt.pojavlaunch.Architecture

class MoJsonRule {
    var action: String? = null
    var os: OSDescriptor? = null

    fun getPrecedenceLevel(): Int {
        if (os == null) return 1
        return 1 + os!!.getPrecedenceLevel()
    }

    fun matches(): Boolean {
        if (os == null) return true
        return os!!.matches()
    }

    companion object {
        fun ruleSetCheck(rules: Array<MoJsonRule>): String {
            var precedenceLevel = 0
            var action = "disallow"
            for (rule in rules) {
                val ruleLevel = rule.getPrecedenceLevel()
                if (ruleLevel <= precedenceLevel) continue
                if (rule.matches()) action = rule.action ?: "disallow"
                precedenceLevel = ruleLevel
            }
            return action
        }
    }

    class OSDescriptor {
        var name: String? = null
        var version: String? = null
        var arch: String? = null

        fun getPrecedenceLevel(): Int {
            var precedence = 0
            if (name != null) precedence += 1
            if (version != null) precedence += 2
            if (arch != null) precedence += 3
            return precedence
        }

        fun matches(): Boolean {
            return propertyMatches(name, "linux") &&
                    propertyMatches(arch, Architecture.archAsString(Architecture.getDeviceArchitecture())) &&
                    version == null
        }

        companion object {
            private fun propertyMatches(value: String?, expected: String): Boolean {
                if (value == null) return true
                return value == expected
            }
        }
    }
}
