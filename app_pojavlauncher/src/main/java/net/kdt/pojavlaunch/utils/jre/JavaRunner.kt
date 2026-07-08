package net.kdt.pojavlaunch.utils.jre

import android.content.Context
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import androidx.annotation.NonNull
import net.kdt.pojavlaunch.AWTCanvasView
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import java.io.File
import java.util.*

object JavaRunner {
    private fun getCacioJavaArgs(javaArgList: MutableList<String>, isJava8: Boolean): Boolean {
        javaArgList.add("-Djava.awt.headless=false")
        javaArgList.add("-Dcacio.managed.screensize=${AWTCanvasView.AWT_CANVAS_WIDTH}x${AWTCanvasView.AWT_CANVAS_HEIGHT}")
        javaArgList.add("-Dcacio.font.fontmanager=sun.awt.X11FontManager")
        javaArgList.add("-Dcacio.font.fontscaler=sun.font.FreetypeFontScaler")
        javaArgList.add("-Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel")
        if (isJava8) {
            javaArgList.add("-Dawt.toolkit=net.java.openjdk.cacio.ctc.CTCToolkit")
            javaArgList.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment")
            val cacioClasspath = createCacioClasspath()
            javaArgList.add(cacioClasspath.toString())
            return false
        } else {
            val caciocavallo17AgentDir = File(Tools.DIR_GAME_HOME, "caciocavallo17")
            val cacioJars = caciocavallo17AgentDir.listFiles { _, s -> s.endsWith(".jar") }
            if (cacioJars == null || cacioJars.size < 1) return false
            javaArgList.add("-javaagent:${cacioJars[0].absolutePath}")
            javaArgList.add("-Dawt.toolkit=com.github.caciocavallosilano.cacio.ctc.CTCToolkit")
            javaArgList.add("-Djava.awt.graphicsenv=com.github.caciocavallosilano.cacio.ctc.CTCGraphicsEnvironment")
            javaArgList.add("--add-exports=java.desktop/java.awt=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.java2d=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/java.awt.dnd.peer=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.awt.event=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.awt.datatransfer=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.font=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.base/sun.security.action=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.base/java.util=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
            return true
        }
    }

    @NonNull
    private fun createCacioClasspath(): StringBuilder {
        val cacioClasspath = StringBuilder()
        cacioClasspath.append("-Xbootclasspath/p")
        val cacioDir = File(Tools.DIR_GAME_HOME, "caciocavallo")
        val cacioFiles = cacioDir.listFiles()
        if (cacioFiles != null) {
            for (file in cacioFiles) {
                if (file.name.endsWith(".jar")) {
                    cacioClasspath.append(":").append(file.absolutePath)
                }
            }
        }
        return cacioClasspath
    }

    private fun getJavaArgs(runtimeHome: String, userArguments: MutableList<String>): List<String> {
        val resolvFile = File(Tools.DIR_DATA, "resolv.conf").absolutePath

        userArguments.add(0, "-Xms${LauncherPreferences.PREF_RAM_ALLOCATION}M")
        userArguments.add(0, "-Xmx${LauncherPreferences.PREF_RAM_ALLOCATION}M")

        val overridableArguments = ArrayList(Arrays.asList(
            "-Djava.home=$runtimeHome",
            "-Djava.io.tmpdir=${Tools.DIR_CACHE.absolutePath}",
            "-Djna.boot.library.path=${Tools.NATIVE_LIB_DIR}",
            "-Duser.home=${Tools.DIR_GAME_HOME}",
            "-Duser.language=${System.getProperty("user.language")}",
            "-Dos.name=Linux",
            "-Dos.version=Android-${Build.VERSION.RELEASE}",
            "-Dpojav.path.minecraft=${Tools.DIR_GAME_NEW}",
            "-Dpojav.path.private.account=${Tools.DIR_ACCOUNT_NEW}",
            "-Duser.timezone=${TimeZone.getDefault().id}",
            "-Dorg.lwjgl.vulkan.libname=libvulkan.so",
            "-Dorg.lwjgl.spvc.libname=spirv-cross-c-shared",
            "-Dorg.lwjgl.system.allocator=system",
            "-Dext.net.resolvPath=$resolvFile",
            "-Dlog4j2.formatMsgNoLookups=true",
            "-Dfml.earlyprogresswindow=false",
            "-Dloader.disable_forked_guis=true",
            "-Djdk.lang.Process.launchMechanism=FORK"
        ))
        val additionalArguments = ArrayList<String>()
        for (arg in overridableArguments) {
            val strippedArg = arg.substring(0, arg.indexOf('='))
            var add = true
            for (uarg in userArguments) {
                if (uarg.startsWith(strippedArg)) {
                    add = false
                    break
                }
            }
            if (add) additionalArguments.add(arg)
            else Log.i("ArgProcessor", "Arg skipped: $arg")
        }

        userArguments.addAll(additionalArguments)
        return userArguments
    }

    private fun getVmPath(runtimeHomeDir: File, arch: String?, flavor: String): File {
        return if (arch != null) File(runtimeHomeDir, "lib/$arch/$flavor/libjvm.so")
        else File(runtimeHomeDir, "lib/$flavor/libjvm.so")
    }

    private fun findVmForArch(runtimeHomeDir: File, arch: String?): File? {
        var finalPath: File
        finalPath = getVmPath(runtimeHomeDir, arch, "server")
        if (finalPath.exists()) return finalPath
        finalPath = getVmPath(runtimeHomeDir, arch, "client")
        if (finalPath.exists()) return finalPath
        return null
    }

    fun findVmPath(runtimeHomeDir: File, runtimeArch: String): File? {
        var finalPath: File
        finalPath = findVmForArch(runtimeHomeDir, null)
        if (finalPath != null) return finalPath
        when (runtimeArch) {
            "i386", "i486", "i586" -> {
                finalPath = findVmForArch(runtimeHomeDir, "i386")
                if (finalPath != null) return finalPath
                finalPath = findVmForArch(runtimeHomeDir, "i486")
                if (finalPath != null) return finalPath
                finalPath = findVmForArch(runtimeHomeDir, "i586")
                if (finalPath != null) return finalPath
            }
            else -> {
                finalPath = findVmForArch(runtimeHomeDir, runtimeArch)
                if (finalPath != null) return finalPath
            }
        }
        return null
    }

    private fun relocateLdLibPath(vmPath: File, extraDirs: List<String>?) {
        val vmDir = vmPath.parentFile!!
        val libsDir = vmDir.parentFile!!
        val libPathBuilder = StringBuilder()
            .append(libsDir.absolutePath).append(":")
            .append(Tools.NATIVE_LIB_DIR).append(':')
            .append(vmDir.absolutePath).append(':')
            .append(File(libsDir, "jli").absolutePath)

        if (extraDirs != null) for (path in extraDirs) {
            libPathBuilder.append(':').append(path)
        }

        val ldLibPath = libPathBuilder.toString()
        try {
            Os.setenv("LD_LIBRARY_PATH", ldLibPath, true)
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }
        JREUtils.setLdLibraryPath(ldLibPath)
    }

    private fun setImmutableEnvVars(jreHome: File) {
        try {
            Os.setenv("POJAV_NATIVEDIR", Tools.NATIVE_LIB_DIR, true)
            Os.setenv("JAVA_HOME", jreHome.absolutePath, true)
            Os.setenv("HOME", Tools.DIR_GAME_HOME, true)
            Os.setenv("TMPDIR", Tools.DIR_CACHE.absolutePath, true)
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }
    }

    private fun preprocessUserArgs(args: MutableList<String>): Boolean {
        val iterator = args.listIterator()
        var hasJavaAgent = false
        while (iterator.hasNext()) {
            var arg = iterator.next()
            when (arg) {
                "-p" -> arg = "--module-path"
                "--add-reads", "--add-exports", "--add-opens", "--add-modules",
                "--limit-modules", "--module-path", "--patch-module", "--upgrade-module-path" -> {
                    iterator.remove()
                    val argValue = iterator.next()
                    iterator.remove()
                    iterator.add("$arg=$argValue")
                }
                "-d32", "-d64", "-Xint", "-XX:+UseTransparentHugePages",
                "-XX:+UseLargePagesInMetaspace", "-XX:+UseLargePages" -> iterator.remove()
                else -> {
                    if (arg.startsWith("-Xms") || arg.startsWith("-Xmx") || arg.startsWith("-XX:ActiveProcessorCount")) iterator.remove()
                    if (!hasJavaAgent && arg.startsWith("-javaagent:")) hasJavaAgent = true
                }
            }
        }
        return hasJavaAgent
    }

    private fun addx86SignalWorkaround(args: MutableList<String>) {
        if (Build.VERSION.SDK_INT != 23) return
        if (Architecture.getDeviceArchitecture() != Architecture.ARCH_X86) return
        args.add("-Xrs")
    }

    @Throws(VMLoadException::class)
    fun startJvm(runtime: net.kdt.pojavlaunch.multirt.Runtime, vmArgs: MutableList<String>, classpathEntries: List<String>, mainClass: String, applicationArgs: List<String>) {
        val runtimeHomeDir = MultiRTUtils.getRuntimeHome(runtime.name)
        val vmPath = findVmPath(runtimeHomeDir, runtime.arch)
            ?: throw VMLoadException("Unable to find the Java VM", 0, -1)

        var hasJavaAgent = preprocessUserArgs(vmArgs)
        val runtimeArgs = ArrayList<String>()
        if (getCacioJavaArgs(runtimeArgs, runtime.javaVersion == 8)) hasJavaAgent = true
        runtimeArgs.addAll(getJavaArgs(runtimeHomeDir.absolutePath, vmArgs))

        runtimeArgs.add("-XX:ActiveProcessorCount=${java.lang.Runtime.getRuntime().availableProcessors()}")
        addx86SignalWorkaround(runtimeArgs)
        val classpathBuilder = StringBuilder().append("-Djava.class.path=")
        var first = true
        for (entry in classpathEntries) {
            if (first) first = false
            else classpathBuilder.append(':')
            classpathBuilder.append(entry)
        }
        runtimeArgs.add(classpathBuilder.toString())

        setImmutableEnvVars(runtimeHomeDir)
        relocateLdLibPath(vmPath, null)

        nativeLoadJVM(vmPath.absolutePath, runtimeArgs.toTypedArray(), mainClass, applicationArgs.toTypedArray(), hasJavaAgent)
    }

    @Throws(VMLoadException::class)
    external fun nativeLoadJVM(vmPath: String, javaArgs: Array<String>, mainClass: String, appArgs: Array<String>, hasJavaAgents: Boolean): Boolean
    external fun nativeSetupExit(context: Context?)
}
