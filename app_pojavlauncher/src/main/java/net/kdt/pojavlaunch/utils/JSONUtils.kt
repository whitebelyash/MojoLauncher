package net.kdt.pojavlaunch.utils

import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

object JSONUtils {
    fun insertJSONValueList(args: MutableList<String>, keyValueMap: Map<String, String>): List<String> {
        for (i in args.indices) {
            args[i] = insertSingleJSONValue(args[i], keyValueMap)
        }
        return args
    }

    fun insertSingleJSONValue(value: String, keyValueMap: Map<String, String>): String {
        var valueInserted = value
        for ((key, value1) in keyValueMap) {
            valueInserted = valueInserted.replace("\${$key}", value1 ?: "")
        }
        return valueInserted
    }

    @Throws(IOException::class)
    fun <T> readFromStream(file: InputStream, clazs: Class<T>): T {
        InputStreamReader(file).use { streamReader ->
            return Tools.GLOBAL_GSON.fromJson(streamReader, clazs)
        }
    }

    @Throws(IOException::class)
    fun writeToFile(file: File, target: Any) {
        FileWriter(file).use { fileWriter ->
            Tools.GLOBAL_GSON.toJson(target, fileWriter)
        }
    }

    @Throws(IOException::class)
    fun <T> readFromFile(file: File, clazs: Class<T>): T {
        FileReader(file).use { fileReader ->
            return Tools.GLOBAL_GSON.fromJson(fileReader, clazs)
        }
    }
}
