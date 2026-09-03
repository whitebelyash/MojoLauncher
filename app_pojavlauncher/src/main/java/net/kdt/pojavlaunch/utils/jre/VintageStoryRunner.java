package net.kdt.pojavlaunch.utils.jre;

import android.content.Context;
import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;
import android.util.ArrayMap;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.GLInfoUtils;
import net.kdt.pojavlaunch.utils.JREUtils;
import net.kdt.pojavlaunch.utils.MesaUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import git.artdeell.mojo.BuildConfig;
import git.artdeell.mojoexec.MojoExec;

/**
 * .NET-backed game (Vintage Story) launcher, the .NET counterpart of {@link GameRunner}.
 * GL4ES is blocked by switching the instance renderer to LTW, same as GameRunner does.
 */
public class VintageStoryRunner {
    private static final String VS_ASSEMBLY = "Vintagestory.dll";
    private static final String GL4ES_RENDERER = "opengles2";
    private static final String LTW_RENDERER = "opengles3_ltw";

    /** Shared .NET runtime + fontconfig component locations. */
    public static File getRuntimeRoot() {
        return new File(Tools.DIR_DATA, "dotnet-runtime");
    }

    public static File getFontconfigDir() {
        return new File(Tools.DIR_DATA, "fonts");
    }

    /**
     * Launch a Vintage Story instance.
     */
    public static void launchGame(AppCompatActivity context, Instance instance) throws Throwable {
        if(!instance.isVintageStory()) {
            throw new IllegalStateException("Not a Vintage Story instance: " + instance.name);
        }
        if(!isArm64()) {
            throw new UnsupportedOperationException("Vintage Story requires a 64-bit ARM device");
        }

        File vsDir = locateGameDir(DotnetAssets.VS_DIR);
        if(vsDir == null) {
            throw new java.io.IOException(
                    "Vintage Story is not installed (" + DotnetAssets.VS_DIR.getAbsolutePath() +
                    "). Create a Vintage Story instance and import the game data first.");
        }

        Log.i("VintageStoryRunner", "Booting VS from " + vsDir.getAbsolutePath());

        // GL4ES is not supported here, so switch the renderer to LTW.
        String renderer = instance.getLaunchRenderer();
        if(GL4ES_RENDERER.equals(renderer)) {
            renderer = LTW_RENDERER;
        }

        printLauncherInfo(context, renderer);

        // VS loads its audio/cairo natives under sonames the app doesn't expose, so mirror
        // dnbootstrap's SymlinkUtil and point them at the bundled libs. The link is recreated
        // every run and a missing source is fatal, otherwise the game dies with a bare
        // "cannot open libcairo.so.2" crash.
        File nativeLibDir = new File(context.getApplicationInfo().nativeLibraryDir);
        symlinkLibrary(vsDir, nativeLibDir, "libopenal.so", "libopenal.so.1");
        symlinkLibrary(vsDir, nativeLibDir, "libcairo.so", "libcairo.so.2");

        setEnviroimentForGame(context, renderer);

        JREUtils.chdir(vsDir.getAbsolutePath());

        DotnetRunner.launchDotnetVM(
                context,
                getRuntimeRoot().getAbsolutePath(),
                vsDir.getAbsolutePath(),
                VS_ASSEMBLY,
                getFontconfigDir().getAbsolutePath()
        );
    }

    /**
     * The renderer/environment setup {@link JREUtils#setEnviroimentForGame} does for Minecraft,
     * reimplemented for the .NET path (GL4ES already blocked by the caller).
     */
    private static void setEnviroimentForGame(Context context, String renderer) throws Throwable {
        Map<String, String> envMap = new ArrayMap<>();

        envMap.put("FORCE_VSYNC", String.valueOf(LauncherPreferences.PREF_FORCE_VSYNC));
        envMap.put("MESA_GLSL_CACHE_DIR", Tools.DIR_CACHE.getAbsolutePath());
        envMap.put("force_glsl_extensions_warn", "true");
        envMap.put("allow_higher_compat_version", "true");
        envMap.put("allow_glsl_extension_directive_midshader", "true");
        envMap.put("LIBGL_NOERROR", "1");

        // Discovers the (optionally Zink) plugin and applies the gallium driver/GL version
        // overrides; freedreno_kgsl relies on these to pick the right driver instead of zink.
        MesaUtils.initEnvironment(context, renderer, envMap);

        if(LauncherPreferences.PREF_BIG_CORE_AFFINITY) envMap.put("POJAV_BIG_CORE_AFFINITY", "1");
        if(LauncherPreferences.PREF_ALSOFT_FORCE_OPENSL) envMap.put("ALSOFT_DRIVERS", "opensl");
        if(LauncherPreferences.PREF_FREEDRENO_SYSMEM) {
            envMap.put("FD_MESA_DEBUG", "sysmem");
            envMap.put("TU_DEBUG", "sysmem");
        }

        for (Map.Entry<String, String> env : envMap.entrySet()) {
            try {
                Os.setenv(env.getKey(), env.getValue(), true);
            }catch (NullPointerException exception){
                Log.e("VintageStoryRunner", "Failed to set env " + env.getKey(), exception);
            }
        }

        if(GLInfoUtils.getGlInfo().isAdreno() && !LauncherPreferences.PREF_ZINK_PREFER_SYSTEM_DRIVER) {
            MojoExec.setUseTurnip(true);
        }

        JREUtils.setRendererLibraryPath(Tools.NATIVE_LIB_DIR, MesaUtils.getCustomZinkLibraryPath());
        String rendererLibrary = JREUtils.loadGraphicsLibrary(renderer);
        if(rendererLibrary == null) {
            throw new IOException("Failed to load renderer " + renderer + " for Vintage Story");
        }
    }

    /**
     * VS-specific version of {@link Tools#printLauncherInfo}: same device/GPU info (initialising
     * the GL context) but without the Minecraft-only/JVM bits.
     */
    private static void printLauncherInfo(Context context, String renderer) {
        Logger.appendToLog("Info: Launcher version: " + BuildConfig.VERSION_NAME);
        Logger.appendToLog("Info: Build type: " + BuildConfig.BUILD_TYPE);
        Logger.appendToLog("Info: Architecture: " + Architecture.archAsString(Architecture.getDeviceArchitecture()));
        Logger.appendToLog("Info: Device model: " + Build.MANUFACTURER + " " + Build.MODEL);
        Logger.appendToLog("Info: API version: " + Build.VERSION.SDK_INT);
        Logger.appendToLog("Info: Total RAM on device: " + Tools.getTotalDeviceMemory(context) + " Mb");
        GLInfoUtils.GLInfo info = GLInfoUtils.getGlInfo();
        Logger.appendToLog("Info: Graphics device: " + info.vendor + " " + info.renderer +
                " (OpenGL ES " + info.glesMajorVersion + ")");
        Logger.appendToLog("Info: Selected renderer: " + renderer);
    }

    /** The bundled .NET nethost is arm64-v8a only. */
    public static boolean isArm64() {
        return Architecture.getDeviceArchitecture() == Architecture.ARCH_ARM64;
    }

    private static File locateVsDir(File start, int depth) {
        if(depth > 4 || start == null || !start.isDirectory()) return null;
        File[] children = start.listFiles();
        if(children == null) return null;
        for(File child : children) {
            if(child.isFile() && VS_ASSEMBLY.equalsIgnoreCase(child.getName())) {
                return start;
            }
        }
        for(File child : children) {
            if(!child.isDirectory()) continue;
            File found = locateVsDir(child, depth + 1);
            if(found != null) return found;
        }
        return null;
    }

    /**
     * Search under the root for the directory that directly contains the VS assembly, so the game
     * boots regardless of the archive layout (dll at the root or nested). Null if not found.
     */
    public static File locateGameDir(File start) {
        return locateVsDir(start, 0);
    }

    /**
     * Point {@code gameDir/targetLib} at {@code nativeLibDir/srcLib} so VS can load its
     * audio/cairo natives under the sonames it expects. The link is always re-pointed at the
     * current source; a missing source is an install/packaging error, not a dangling link.
     */
    private static void symlinkLibrary(File gameDir, File nativeLibDir, String srcLib, String targetLib) throws java.io.IOException {
        File src = new File(nativeLibDir, srcLib);
        if(!src.canRead()) {
            throw new java.io.IOException("Native library missing: " + src.getAbsolutePath());
        }
        File targetFile = new File(gameDir, targetLib);

        targetFile.delete();

        try {
            Os.symlink(src.getAbsolutePath(), targetFile.getAbsolutePath());
        }catch (ErrnoException e) {
            throw new java.io.IOException("Failed to create symlink " + targetFile.getAbsolutePath() +
                    " -> " + src.getAbsolutePath(), e);
        }

        if(!targetFile.canRead()) {
            throw new java.io.IOException("Symlink " + targetFile.getAbsolutePath() +
                    " does not resolve to a readable file: " + src.getAbsolutePath());
        }
    }
}
