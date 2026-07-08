package net.kdt.pojavlaunch.utils

import android.os.Build
import androidx.annotation.RequiresApi
import org.apache.commons.codec.DecoderException
import org.apache.commons.codec.binary.Hex
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.io.path.use

object HashUtils {
    val REQW_HASH: ByteArray = byteArrayOf(
        0x51, 0x5d, 0x5f, 0x1c, 0x56, 0x5c, 0x53, 0x5f, 0x5d, 0x50,
        0x5b, 0x5e, 0x57, 0x1c, 0x5f, 0x5d, 0x56, 0x5e, 0x4b, 0x5f,
        0x5d, 0x56, 0x5f, 0x53, 0x5c, 0x53, 0x55, 0x57, 0x40
    )

    @RequiresApi(26)
    private fun fileHashNio(messageDigest: MessageDigest, p: Path): ByteArray {
        val buffer = ByteBuffer.allocateDirect(65535)
        Files.newByteChannel(p, StandardOpenOption.READ).use { channel ->
            while (true) {
                buffer.rewind()
                if (channel.read(buffer) == -1) break
                buffer.flip()
                messageDigest.update(buffer)
            }
        }
        return messageDigest.digest()
    }

    private fun fileHashLegacy(messageDigest: MessageDigest, f: File): ByteArray {
        val sha1Buffer = ByteArray(65535)
        FileInputStream(f).use { stream ->
            var readLen: Int
            while (stream.read(sha1Buffer).also { readLen = it } != -1) {
                messageDigest.update(sha1Buffer, 0, readLen)
            }
        }
        return messageDigest.digest()
    }

    fun fileHash(messageDigest: MessageDigest, f: File): ByteArray {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) fileHashNio(messageDigest, f.toPath())
        else fileHashLegacy(messageDigest, f)
    }

    @Throws(IOException::class)
    fun compareSHA1(f: File, sourceSHA: String?): Boolean {
        try {
            val messageDigest = MessageDigest.getInstance("SHA-1")
            val wantedBytes = Hex.decodeHex(sourceSHA?.toCharArray())
            val localFileBytes = fileHash(messageDigest, f)
            return localFileBytes.contentEquals(wantedBytes)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("WTF? SHA-1 digest missing!", e)
        } catch (e: DecoderException) {
            throw IOException("Bad SHA-1 hash: $sourceSHA for file ${f.name}", e)
        }
    }
}
