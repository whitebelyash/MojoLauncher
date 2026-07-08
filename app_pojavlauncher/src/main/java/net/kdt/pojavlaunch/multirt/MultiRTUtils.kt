package net.kdt.pojavlaunch.multirt

import net.kdt.pojavlaunch.Tools.NATIVE_LIB_DIR
import org.apache.commons.io.FileUtils.listFiles
import org.apache.commons.io.FileUtils.write

import android.system.Os
import android.util.Log
import com.kdt.mcgui.ProgressLayout
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.MathUtils
import net.kdt.pojavlaunch.utils.jre.JavaRunner
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*

object MultiRTUtils {
    private val sCache = HashMap<String, Runtime>()
    private val RUNTIME_FOLDER = File(Tools.MULTIRT_HOME)
    private const val JAVA_VERSION_STR = "JAVA_VERSION=\""
    private const val OS_ARCH_STR = "OS_ARCH=\""

    fun getRuntimes(): List<Runtime> {
        if (!RUNTIME_FOLDER.exists() && !RUNTIME_FOLDER.mkdirs()) {
            throw RuntimeException("Failed to create runtime directory")
        }
        val runtimes = ArrayList<Runtime>()
        val files = RUNTIME_FOLDER.listFiles()
            ?: throw RuntimeException("The runtime directory does not exist")
        for (f in files) {
            runtimes.add(read(f.name))
        }
        return runtimes
    }

    fun getExactJreName(majorVersion: Int): String? {
        val runtimes = getRuntimes()
        for (r in runtimes) {
            if (r.javaVersion == majorVersion) return r.name
        }
        return null
    }

    fun getNearestJreName(majorVersion: Int): String? {
        val runtimes = getRuntimes()
        val nearestRankedRuntime = MathUtils.findNearestPositive(majorVersion, runtimes) { runtime -> runtime.javaVersion }
            ?: return null
        return nearestRankedRuntime.value?.name
    }

    @Throws(IOException::class)
    fun installRuntimeNamed(nativeLibDir: String, runtimeInputStream: InputStream, name: String) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        try {
            uncompressTarXZ(runtimeInputStream, dest)
            runtimeInputStream.close()
            unpack200(nativeLibDir, "$RUNTIME_FOLDER/$name")
            read(name)
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME)
        }
    }

    @Throws(IOException::class)
    fun postPrepare(name: String) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (!dest.exists()) return
        val runtime = read(name)
        val vmPath = JavaRunner.findVmPath(dest, runtime.arch) ?: throw IOException("Could not find libjvm.so after extraction")
        val libDir = Objects.requireNonNull(vmPath.parentFile)!!.parentFile

        val ftIn = File(libDir, "libfreetype.so.6")
        val ftOut = File(libDir, "libfreetype.so")
        if (ftIn.exists() && (!ftOut.exists() || ftIn.length() != ftOut.length())) {
            if (!ftIn.renameTo(ftOut)) throw IOException("Failed to rename freetype")
        }
        copyDummyNativeLib("libawt_xawt.so", libDir)
    }

    @Throws(IOException::class)
    fun installRuntimeNamedBinpack(
        universalFileInputStream: InputStream,
        platformBinsInputStream: InputStream,
        name: String,
        binpackVersion: String
    ) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        try {
            installRuntimeNamedNoRemove(universalFileInputStream, dest)
            installRuntimeNamedNoRemove(platformBinsInputStream, dest)
            unpack200(NATIVE_LIB_DIR, "$RUNTIME_FOLDER/$name")
            val binpackVerfile = File(RUNTIME_FOLDER, "/$name/pojav_version")
            write(binpackVerfile, binpackVersion, StandardCharsets.UTF_8)
            forceReread(name)
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME)
        }
    }

    fun readInternalRuntimeVersion(name: String): String? {
        val versionFile = File(RUNTIME_FOLDER, "/$name/pojav_version")
        return try {
            if (versionFile.exists()) {
                Tools.read(versionFile.absolutePath)
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun readLastUpdateTime(name: String): Long {
        val lastUpdateTimeFile = File(RUNTIME_FOLDER, "$name/last_check_time")
        if (!lastUpdateTimeFile.exists()) return -1
        return try {
            Tools.read(lastUpdateTimeFile).trim().toLong()
        } catch (_: IOException) {
            -1
        } catch (_: NumberFormatException) {
            -1
        }
    }

    fun writeLastUpdateTime(name: String, time: Long) {
        val lastUpdateTimeFile = File(RUNTIME_FOLDER, "$name/last_check_time")
        try {
            Tools.write(lastUpdateTimeFile, time.toString())
        } catch (_: IOException) {
        }
    }

    @Throws(IOException::class)
    fun removeRuntimeNamed(name: String) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (dest.exists()) {
            FileUtils.deleteDirectory(dest)
            sCache.remove(name)
        }
    }

    fun getRuntimeHome(name: String): File {
        val dest = File(RUNTIME_FOLDER, name)
        Log.i("MiltiRTUitls", "Dest exists? " + dest.exists())
        if ((!dest.exists()) || forceReread(name).versionString == null) throw RuntimeException("Selected runtime is broken!")
        return dest
    }

    fun forceReread(name: String): Runtime {
        sCache.remove(name)
        return read(name)
    }

    fun read(name: String): Runtime {
        var returnRuntime = sCache[name]
        if (returnRuntime != null) return returnRuntime
        val release = File(RUNTIME_FOLDER, "$name/release")
        if (!release.exists()) {
            return Runtime(name).also { sCache[name] = it }
        }
        returnRuntime = try {
            val content = Tools.read(release.absolutePath)
            val javaVersion = Tools.extractUntilCharacter(content, JAVA_VERSION_STR, '"')
            val osArch = Tools.extractUntilCharacter(content, OS_ARCH_STR, '"')
            if (javaVersion != null && osArch != null) {
                val javaVersionSplit = javaVersion.split("\\.")
                val javaVersionInt = if (javaVersionSplit[0] == "1") {
                    javaVersionSplit[1].toInt()
                } else {
                    javaVersionSplit[0].toInt()
                }
                Runtime(name, javaVersion, osArch, javaVersionInt)
            } else {
                Runtime(name)
            }
        } catch (e: IOException) {
            Runtime(name)
        }
        sCache[name] = returnRuntime
        return returnRuntime
    }

    private fun unpack200(nativeLibraryDir: String, runtimePath: String) {
        val basePath = File(runtimePath)
        val files = listFiles(basePath, arrayOf("pack"), true)
        val workdir = File(nativeLibraryDir)
        val processBuilder = ProcessBuilder().directory(workdir)
        for (jarFile in files) {
            try {
                val process = processBuilder
                    .command(
                        "./libunpack200.so", "-r",
                        jarFile.absolutePath,
                        jarFile.absolutePath.replace(".pack", "")
                    )
                    .start()
                process.waitFor()
            } catch (e: Exception) {
                Log.e("MULTIRT", "Failed to unpack the runtime !")
            }
        }
    }

    private fun copyDummyNativeLib(name: String, dest: File) {
        val fileLib = File(dest, name)
        FileInputStream(File(NATIVE_LIB_DIR, name)).use { is_ ->
            FileOutputStream(fileLib).use { os ->
                IOUtils.copy(is_, os)
            }
        }
    }

    @Throws(IOException::class)
    private fun installRuntimeNamedNoRemove(runtimeInputStream: InputStream, dest: File) {
        uncompressTarXZ(runtimeInputStream, dest)
        runtimeInputStream.close()
    }

    @Throws(IOException::class)
    private fun uncompressTarXZ(tarFileInputStream: InputStream, dest: File) {
        net.kdt.pojavlaunch.utils.FileUtils.ensureDirectory(dest)
        val buffer = ByteArray(8192)
        TarArchiveInputStream(XZCompressorInputStream(tarFileInputStream)).use { tarIn ->
            var tarEntry: TarArchiveEntry?
            while (tarIn.nextTarEntry.also { tarEntry = it } != null) {
                val tarEntryName = tarEntry!!.name
                ProgressLayout.setProgress(
                    ProgressLayout.UNPACK_RUNTIME, 100,
                    R.string.global_unpacking, tarEntryName
                )
                val destPath = File(dest, tarEntry!!.name)
                net.kdt.pojavlaunch.utils.FileUtils.ensureParentDirectory(destPath)
                when {
                    tarEntry!!.isSymbolicLink -> {
                        try {
                            Os.symlink(tarEntry!!.name, tarEntry!!.linkName)
                        } catch (e: Throwable) {
                            Log.e("MultiRT", e.toString())
                        }
                    }

                    tarEntry!!.isDirectory -> {
                        net.kdt.pojavlaunch.utils.FileUtils.ensureDirectory(destPath)
                    }

                    !destPath.exists() || destPath.length() != tarEntry!!.size -> {
                        FileOutputStream(destPath).use { os ->
                            IOUtils.copyLarge(tarIn, os, buffer)
                        }
                    }
                }
            }
        }
    }
}
