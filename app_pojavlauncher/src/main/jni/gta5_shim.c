#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>

#include <SDL3/SDL.h>
#include <SDL3/SDL_main.h>

#define LOG_TAG "GTA5Shim"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef int (*common_main_fn)(int argc, char **argv);
typedef int (*sdl_main_fn)(int argc, char **argv);

#define COMMON_MAIN_SYMBOL "_ZN4rage10CommonMainEiPPc"
#define SDL_MAIN_SYMBOL "SDL_main"

/*
 * Route everything the engine prints to stdout/stderr into the launcher log
 * (latestlog.txt). Without this the game used to freopen() them into
 * /storage/emulated/0/Games/GTAV/gtav_tty.log, which we do not want here.
 */
static void redirect_std_streams(const char *path) {
    if(!path || !*path) return;
    int fd = open(path, O_CREAT | O_WRONLY | O_APPEND, 0666);
    if(fd < 0) {
        LOGE("Failed to open launcher log %s: %s", path, strerror(errno));
        return;
    }
    dup2(fd, STDOUT_FILENO);
    dup2(fd, STDERR_FILENO);
    if(fd > STDERR_FILENO) close(fd);
    LOGI("stdout/stderr redirected to %s", path);
}

/* Minimal env parity with what the stock SDL_main sets up before RAGE. */
static void setup_environment(void) {
    const char *base = getenv("GTAV_GAME_DIR");
    if(!base || !*base) base = "/storage/emulated/0/Games/GTAV";
    char cache[1024];
    snprintf(cache, sizeof(cache), "%s/dxvk-cache", base);
    mkdir(cache, 0755);
    setenv("DXVK_SHADER_CACHE_PATH", cache, 1);
    setenv("DXVK_WSI_DRIVER", "SDL2", true);
    LOGI("DXVK_SHADER_CACHE_PATH=%s", cache);
}

static char g_argbuf[4096];
static char *g_argv[128];

/* Build an argv table from a whitespace-separated command line string.
   Returns argc (0 if the string was empty/unset). */
static int parse_command_line(const char *cmdline) {
    if(!cmdline) return 0;
    strncpy(g_argbuf, cmdline, sizeof(g_argbuf) - 1);
    g_argbuf[sizeof(g_argbuf) - 1] = '\0';
    int argc = 0;
    char *p = g_argbuf;
    while(argc < (int)(sizeof(g_argv) / sizeof(g_argv[0])) - 1) {
        while(*p == ' ' || *p == '\t') p++;
        if(!*p) break;
        g_argv[argc++] = p;
        while(*p && *p != ' ' && *p != '\t') p++;
        if(*p) *p++ = '\0';
    }
    g_argv[argc] = NULL;
    return argc;
}

static int init_sdl(void) {
    SDL_SetMainReady();
    SDL_InitFlags flags = SDL_INIT_VIDEO | SDL_INIT_AUDIO | SDL_INIT_JOYSTICK |
                          SDL_INIT_HAPTIC | SDL_INIT_GAMEPAD | SDL_INIT_SENSOR |
                          SDL_INIT_EVENTS;
    if(!SDL_Init(flags)) {
        LOGE("SDL_Init failed: %s", SDL_GetError());
        return -1;
    }
    LOGI("SDL3 initialized");
    return 0;
}

JNIEXPORT jint JNICALL
Java_net_kdt_pojavlaunch_game_GameActivity_nativeBootGta5(
        JNIEnv *env, jclass clazz, jstring jLibraryPath, jstring jLogPath) {
    const char *library_path = (*env)->GetStringUTFChars(env, jLibraryPath, NULL);
    const char *log_path = jLogPath ? (*env)->GetStringUTFChars(env, jLogPath, NULL) : NULL;

    redirect_std_streams(log_path);
    setup_environment();

    LOGI("Loading GTA5 from: %s", library_path);
    void *handle = dlopen(library_path, RTLD_GLOBAL);
    if(!handle) {
        LOGE("Failed to dlopen %s: %s", library_path, dlerror());
        (*env)->ReleaseStringUTFChars(env, jLibraryPath, library_path);
        return -1;
    }

    if(init_sdl() != 0) {
        dlclose(handle);
        (*env)->ReleaseStringUTFChars(env, jLibraryPath, library_path);
        return -1;
    }

    common_main_fn common_main = (common_main_fn)dlsym(handle, COMMON_MAIN_SYMBOL);
    sdl_main_fn sdl_main = NULL;
    if(!common_main) {
        LOGE("Failed to resolve %s: %s", COMMON_MAIN_SYMBOL, dlerror());
        sdl_main = (sdl_main_fn)dlsym(handle, SDL_MAIN_SYMBOL);
        if(!sdl_main) {
            LOGE("Failed to resolve %s either: %s", SDL_MAIN_SYMBOL, dlerror());
            dlclose(handle);
            (*env)->ReleaseStringUTFChars(env, jLibraryPath, library_path);
            return -1;
        }
        LOGI("Falling back to %s", SDL_MAIN_SYMBOL);
    }

    int argc = parse_command_line(getenv("GTA5_ARGS"));
    if(argc == 0) {
        g_argv[0] = "com.gtavsource.android";
        g_argv[1] = NULL;
        argc = 1;
    } else {
        /* RAGE treats argv[0] as the program name and starts parsing at
           argv[1]. The user's command line must start at argv[1], so put a
           placeholder program name in front of it. */
        int i;
        for(i = argc; i > 0; i--) g_argv[i] = g_argv[i - 1];
        g_argv[0] = "com.gtavsource.android";
        g_argv[argc + 1] = NULL;
        argc += 1;
    }
    int i;
    for(i = 0; i < argc; i++) LOGI("arg[%d]=%s", i, g_argv[i]);

    int result;
    if(common_main) {
        LOGI("Invoking rage::CommonMain(argc=%d)", argc);
        result = common_main(argc, g_argv);
        LOGI("rage::CommonMain returned %d", result);
    } else {
        char *legacy_argv[2] = { g_argv[0], NULL };
        LOGI("Invoking SDL_main(argc=%d)", argc);
        result = sdl_main(argc, legacy_argv);
        LOGI("SDL_main returned %d", result);
    }

    SDL_Quit();
    dlclose(handle);
    if(log_path) (*env)->ReleaseStringUTFChars(env, jLogPath, log_path);
    (*env)->ReleaseStringUTFChars(env, jLibraryPath, library_path);
    return result;
}