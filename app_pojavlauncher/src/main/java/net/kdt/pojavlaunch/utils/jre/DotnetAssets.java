package net.kdt.pojavlaunch.utils.jre;

import android.content.Context;
import android.net.Uri;

import net.kdt.pojavlaunch.Tools;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * Installers for Vintage Story (single global install) and the bundled .NET runtime / fontconfig.
 *
 * VS exists as one version only, so it's installed once into a global {@link #VS_DIR} and
 * instances just point at it.
 */
public final class DotnetAssets {

    /** Root of the global, single-instance Vintage Story data. */
    public static final File VS_DIR = new File(Tools.DIR_DATA, "vs");

    private DotnetAssets() {}

    /** @return whether Vintage Story has been installed into {@link #VS_DIR}. */
    public static boolean isInstalled() {
        if(!VS_DIR.isDirectory()) return false;
        return VintageStoryRunner.locateGameDir(VS_DIR) != null;
    }

    /** Extract the user-supplied VSMobile game archive into the global install directory. */
    public static void installGameData(Context context, Uri gameUri) throws IOException {
        if(VS_DIR.exists() && VS_DIR.isDirectory()) {
            FileUtils.deleteDirectory(VS_DIR);
        }
        try(InputStream inputStream = context.getContentResolver().openInputStream(gameUri)) {
            if(inputStream == null) throw new IOException("Failed to open selected archive");
            extractTarGz(inputStream, VS_DIR);
        }
    }

    /** Remove the entire global Vintage Story install (game data). */
    public static void removeAll() throws IOException {
        if(VS_DIR.exists()) {
            FileUtils.deleteDirectory(VS_DIR);
        }
    }

    /**
     * Install an arbitrary .NET runtime archive into the shared location, replacing any previous
     * one. Single installation, no multirt.
     */
    public static void installRuntime(Context context, Uri uri) throws IOException {
        File runtimeRoot = VintageStoryRunner.getRuntimeRoot();
        if(runtimeRoot.exists()) {
            FileUtils.deleteDirectory(runtimeRoot);
        }
        try(InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if(inputStream == null) throw new IOException("Failed to open selected archive");
            extractTarGz(inputStream, runtimeRoot);
        }
        markComponentInstalled(runtimeRoot);
    }

    /** @return the installed .NET runtime path, or null if not installed. */
    public static File getRuntimeHome() {
        File runtimeRoot = VintageStoryRunner.getRuntimeRoot();
        return checkComponentInstalled(runtimeRoot) ? runtimeRoot : null;
    }

    /** Extract the bundled .NET runtime component into place, if missing. */
    public static void ensureRuntime(Context context) throws IOException {
        File runtimeRoot = VintageStoryRunner.getRuntimeRoot();
        if(checkComponentInstalled(runtimeRoot)) return;
        extractAppComponent(context, "dotnet-runtime.tgz", runtimeRoot);
        markComponentInstalled(runtimeRoot);
    }

    /** Extract the bundled fontconfig component into place, if missing. */
    public static void ensureFontconfig(Context context) throws IOException {
        File fontconfigDir = VintageStoryRunner.getFontconfigDir();
        if(checkComponentInstalled(fontconfigDir)) return;
        extractAppComponent(context, "fontconfig.tgz", fontconfigDir);
        markComponentInstalled(fontconfigDir);
    }

    private static void extractAppComponent(Context context, String assetName, File outDir) throws IOException {
        try(InputStream stream = context.getAssets().open(assetName)) {
            extractTarGz(stream, outDir);
        }
    }

    private static void extractTarGz(InputStream inputStream, File targetDir) throws IOException {
        byte[] buffer = new byte[65535];
        try(GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
            TarArchiveInputStream tarStream = new TarArchiveInputStream(gzipInputStream)) {
            TarArchiveEntry entry;
            while((entry = tarStream.getNextTarEntry()) != null) {
                File destination = new File(targetDir, entry.getName());
                if(entry.isDirectory()) {
                    tryMkdirs(destination);
                } else {
                    tryMkdirs(Objects.requireNonNull(destination.getParentFile()));
                    try(FileOutputStream fos = new FileOutputStream(destination)) {
                        int rc;
                        while((rc = tarStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, rc);
                        }
                    }
                }
            }
        }
    }

    private static void tryMkdirs(File dir) throws IOException {
        if(dir.isDirectory()) return;
        if(!dir.mkdirs()) throw new IOException("Failed to create directory " + dir.getAbsolutePath());
    }

    private static boolean checkComponentInstalled(File componentDir) {
        return new File(componentDir, ".installed").exists();
    }

    private static void markComponentInstalled(File componentDir) throws IOException {
        File markFile = new File(componentDir, ".installed");
        if(markFile.exists()) return;
        if(!markFile.createNewFile()) throw new IOException("Failed to mark component installed " + componentDir.getName());
    }
}
