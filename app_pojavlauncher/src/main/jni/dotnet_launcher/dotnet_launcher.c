//
// Created by whbex on 04.09.2026.
//

//
// Native .NET (CoreCLR) host: boots a .NET game through nethost + hostfxr.
//

#include <jni.h>
#include <dlfcn.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <limits.h>
#include <pthread.h>

#include "include/libnethost.h"
#include "include/hostfxr.h"

#define TAG "dnb-main"
#include "../log.h"

typedef int (*get_hostfxr_path_fn)(char_t*, size_t*, const struct get_hostfxr_parameters*);

static int find_hostfxr(const char* dotnet_root, char_t path_buf[PATH_MAX]) {
    struct get_hostfxr_parameters parameters = {
            .assembly_path = NULL,
            .dotnet_root = dotnet_root
    };
    parameters.size = sizeof(parameters);

    size_t path_size = sizeof(char_t[PATH_MAX]) / sizeof(char_t);

    return get_hostfxr_path(path_buf, &path_size, &parameters);
}

static void run_dotnet(void* hostfxr_ptr, const char* dotnet_root, const char* app_dll, const char* app_dir) {
    hostfxr_main_startupinfo_fn main_startupinfo_fn = dlsym(hostfxr_ptr, "hostfxr_main_startupinfo");
    if(main_startupinfo_fn == NULL) {
        LOGE("Failed to resolve hostfxr_main_startupinfo: %s", dlerror());
        return;
    }

    if(app_dir != NULL) {
        if(chdir(app_dir) != 0) {
            LOGE("Failed to chdir to %s: %s", app_dir, strerror(errno));
        }
    }

    char host_path[PATH_MAX];
    snprintf(host_path, PATH_MAX, "%s/%s", dotnet_root, "dotnet");
    char host_app_path[PATH_MAX];
    snprintf(host_app_path, PATH_MAX, "%s/%s", dotnet_root, "dotnet.dll");

    const char* argv[] = { host_path, app_dll };
    const int argc = 2;

    LOGE("dotnet_root: %s app_path: %s", dotnet_root, app_dll);
    pthread_setname_np(pthread_self(), "dnb main thread");
    int rc = main_startupinfo_fn(argc, argv, host_path, dotnet_root, host_app_path);
    LOGI("hostfxr done: %x", rc);
}

JNIEXPORT jboolean JNICALL
Java_net_kdt_pojavlaunch_utils_jre_DotnetRunner_nativeLoadDotnet(JNIEnv *env, jclass clazz,
        jstring dotnetRoot, jstring vsDir, jstring appDll) {
    const char* dotnet_root = (*env)->GetStringUTFChars(env, dotnetRoot, NULL);
    const char* app_dir = (*env)->GetStringUTFChars(env, vsDir, NULL);
    const char* app_dll = (*env)->GetStringUTFChars(env, appDll, NULL);
    if(dotnet_root == NULL || app_dir == NULL || app_dll == NULL) return JNI_FALSE;

    char_t path_buf[PATH_MAX];
    int result = find_hostfxr(dotnet_root, path_buf);
    if(result != 0) {
        LOGE("Cannot find hostfxr: %x", result);
        (*env)->ReleaseStringUTFChars(env, dotnetRoot, dotnet_root);
        (*env)->ReleaseStringUTFChars(env, vsDir, app_dir);
        (*env)->ReleaseStringUTFChars(env, appDll, app_dll);
        return JNI_FALSE;
    }

    void* hostfxr_ptr = dlopen(path_buf, RTLD_NOW);
    if(hostfxr_ptr == NULL) {
        LOGE("Failed to dlopen hostfxr: %s", dlerror());
        (*env)->ReleaseStringUTFChars(env, dotnetRoot, dotnet_root);
        (*env)->ReleaseStringUTFChars(env, vsDir, app_dir);
        (*env)->ReleaseStringUTFChars(env, appDll, app_dll);
        return JNI_FALSE;
    }

    run_dotnet(hostfxr_ptr, dotnet_root, app_dll, app_dir);

    (*env)->ReleaseStringUTFChars(env, dotnetRoot, dotnet_root);
    (*env)->ReleaseStringUTFChars(env, vsDir, app_dir);
    (*env)->ReleaseStringUTFChars(env, appDll, app_dll);
    return JNI_TRUE;
}
