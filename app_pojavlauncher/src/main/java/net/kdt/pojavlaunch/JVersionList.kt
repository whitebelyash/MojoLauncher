package net.kdt.pojavlaunch

import androidx.annotation.Keep
import net.kdt.pojavlaunch.value.*

@Keep
@Suppress("unused")
class JVersionList {
    var latest: Map<String, String>? = null
    var versions: Array<Version>? = null

    @Keep
    open class FileProperties {
        var id: String? = null
        var sha1: String? = null
        var url: String? = null
        var size: Long = 0
    }

    @Keep
    class Version : FileProperties() {
        var arguments: Arguments? = null
        var assetIndex: AssetIndex? = null
        var assets: String? = null
        var downloads: Map<String, ClientInfo>? = null
        var inheritsFrom: String? = null
        var javaVersion: JavaVersionInfo? = null
        var libraries: Array<DependentLibrary> = arrayOf()
        var logging: LoggingConfig? = null
        var mainClass: String? = null
        var minecraftArguments: String? = null
        var minimumLauncherVersion: Int = 0
        var releaseTime: String? = null
        var time: String? = null
        var type: String? = null
    }

    @Keep
    class JavaVersionInfo {
        var component: String? = null
        var majorVersion: Int = 0
        var version: Int = 0
    }

    @Keep
    class LoggingConfig {
        var client: LoggingClientConfig? = null

        @Keep
        class LoggingClientConfig {
            var argument: String? = null
            var file: FileProperties? = null
            var type: String? = null
        }
    }

    @Keep
    class Arguments {
        var game: Array<Any>? = null
        var jvm: Array<Any>? = null

        @Keep
        class ArgValue {
            var rules: Array<MoJsonRule>? = null
            var value: String? = null
            var values: Array<String>? = null
        }
    }

    @Keep
    class AssetIndex : FileProperties() {
        var totalSize: Long = 0
    }
}
