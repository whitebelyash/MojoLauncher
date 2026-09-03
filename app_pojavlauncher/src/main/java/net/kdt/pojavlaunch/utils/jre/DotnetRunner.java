package net.kdt.pojavlaunch.utils.jre;

import android.content.Context;
import android.system.Os;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.IOException;

/**
 * .NET (CoreCLR) game runner, the dotnet counterpart of {@link JavaRunner}. Boots a .NET game
 * (e.g. Vintage Story) through the bundled nethost/hostfxr hosted inside pojavexec.
 */
public class DotnetRunner {

    private static File findCertsDir() {
        File certsDir = new File("/apex/com.android.conscrypt/cacerts/");
        if(certsDir.exists()) return certsDir;
        certsDir = new File("/system/etc/security/cacerts");
        if(certsDir.exists()) return certsDir;
        return null;
    }

    /**
     * Set up the .NET-specific environment and launch a .NET game located in {@code gameDir}.
     * The OpenGL/EGL environment is set up beforehand by {@link VintageStoryRunner}.
     */
    public static void launchDotnetVM(Context context, String runtimeRoot, String gameDir, String appDll, String fontconfig) throws IOException {
        File homeDir = new File(Tools.DIR_GAME_HOME, "home");
        File certsDir = findCertsDir();
        if(certsDir == null) throw new IOException("Cannot start: can't find HTTPS certificate directory");

        try {
            Os.setenv("HOME", homeDir.getAbsolutePath(), true);
            Os.setenv("SSL_CERT_DIR", certsDir.getAbsolutePath(), true);
            if(fontconfig != null) {
                Os.setenv("FONTCONFIG_PATH", fontconfig, true);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(!nativeLoadDotnet(runtimeRoot, gameDir, appDll)) {
            throw new IOException("Failed to launch .NET runtime (see logcat for details)");
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static native boolean nativeLoadDotnet(String dotnetRoot, String vsDir, String appDll);
    static {
        try {
            System.loadLibrary("pojavexec");
        } catch (Throwable t) {
            Log.e("DotnetRunner", "Failed to load pojavexec", t);
        }
    }
}
