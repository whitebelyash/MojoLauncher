package net.kdt.pojavlaunch.utils.memory

import java.io.FileInputStream
import java.io.IOException
import java.util.*

class SelfMapsParser(private val mCallback: Callback) {

    @Throws(IOException::class, NumberFormatException::class)
    fun run() {
        FileInputStream("/proc/self/maps").use { fileInputStream ->
            val scanner = Scanner(fileInputStream)
            while (scanner.hasNextLine()) {
                if (!forEachLine(scanner.nextLine())) break
            }
        }
    }

    @Throws(NumberFormatException::class)
    private fun forEachLine(line: String): Boolean {
        val firstSpaceIndex = line.indexOf(' ')
        val addresses = line.substring(0, firstSpaceIndex)
        val addressArray = addresses.split("-")
        if (addressArray.size < 2) return true
        val begin = java.lang.Long.parseLong(addressArray[0], 16)
        val end = java.lang.Long.parseLong(addressArray[1], 16)
        return mCallback.process(begin, end, line)
    }

    interface Callback {
        fun process(startAddress: Long, endAddress: Long, wholeLine: String): Boolean
    }
}
