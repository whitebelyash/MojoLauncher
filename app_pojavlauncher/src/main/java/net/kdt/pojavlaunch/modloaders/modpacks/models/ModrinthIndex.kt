package net.kdt.pojavlaunch.modloaders.modpacks.models

import java.util.Arrays
import java.util.Map

class ModrinthIndex {
    var formatVersion = 0
    var game: String? = null
    var versionId: String? = null
    var name: String? = null
    var summary: String? = null
    var files: Array<ModrinthIndexFile> = emptyArray()
    var dependencies: Map<String, String> = emptyMap()

    class ModrinthIndexFile {
        var path: String? = null
        var downloads: Array<String> = emptyArray()
        var fileSize = 0
        var hashes: ModrinthIndexFileHashes? = null
        var env: ModrinthIndexFileEnv? = null

        override fun toString(): String {
            return "ModrinthIndexFile{" +
                    "path='" + path + '\'' +
                    ", downloads=" + Arrays.toString(downloads) +
                    ", fileSize=" + fileSize +
                    ", hashes=" + hashes +
                    '}'
        }

        class ModrinthIndexFileHashes {
            var sha1: String? = null
            var sha512: String? = null

            override fun toString(): String {
                return "ModrinthIndexFileHashes{" +
                        "sha1='" + sha1 + '\'' +
                        ", sha512='" + sha512 + '\'' +
                        '}'
            }
        }

        class ModrinthIndexFileEnv {
            var client: String? = null
            var server: String? = null

            override fun toString(): String {
                return "ModrinthIndexFileEnv{" +
                        "client='" + client + '\'' +
                        ", server='" + server + '\'' +
                        '}'
            }
        }
    }

    override fun toString(): String {
        return "ModrinthIndex{" +
                "formatVersion=" + formatVersion +
                ", game='" + game + '\'' +
                ", versionId='" + versionId + '\'' +
                ", name='" + name + '\'' +
                ", summary='" + summary + '\'' +
                ", files=" + Arrays.toString(files) +
                '}'
    }
}
