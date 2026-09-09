#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>

#define LOG_TAG "GTA5Shim"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef int (*SDL_main_func)(int argc, char **argv);

JNIEXPORT jint JNICALL
Java_net_kdt_pojavlaunch_game_GameActivity_nativeBootGta5(JNIEnv *env, jclass clazz, jstring jLibraryPath) {
    const char *library_path = (*env)->GetStringUTFChars(env, jLibraryPath, NULL);
    LOGI("Loading GTA5 from: %s", library_path);

    void *handle = dlopen(library_path, RTLD_GLOBAL);
    if (!handle) {
        LOGE("Failed to dlopen %s: %s", library_path, dlerror());
        (*env)->ReleaseStringUTFChars(env, jLibraryPath, library_path);
        return -1;
    }

    SDL_main_func game_main = (SDL_main_func)dlsym(handle, "SDL_main");
    if (!game_main) {
        LOGE("Failed to find SDL_main in %s: %s", library_path, dlerror());
        dlclose(handle);
        (*env)->ReleaseStringUTFChars(env, jLibraryPath, library_path);
        return -1;
    }

    void (*set_main_ready)(void) = (void (*)(void))dlsym(handle, "SDL_SetMainReady");
    if (set_main_ready) {
        LOGI("Marking current thread as SDL main thread");
        set_main_ready();
    } else {
        LOGE("SDL_SetMainReady not found in %s", library_path);
    }

    LOGI("Resolved SDL_main at %p, calling...", (void *)(uintptr_t)game_main);

    int argc = 1;
    char *argv[] = { "app_process", NULL };

    int result = game_main(argc, argv);

    LOGI("SDL_main returned %d", result);
    dlclose(handle);
    (*env)->ReleaseStringUTFChars(env, jLibraryPath, library_path);
    return result;
}