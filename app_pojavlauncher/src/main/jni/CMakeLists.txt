
cmake_minimum_required(VERSION 3.22.1)

# ------ MojoExec ------

add_subdirectory(mojoexec)

# ------   GLFW   ------

set(GLFW_BUILD_WAYLAND  OFF CACHE BOOL "" FORCE)
set(GLFW_BUILD_X11      OFF CACHE BOOL "" FORCE)
set(GLFW_BUILD_ANDROID  ON  CACHE BOOL "" FORCE)
set(GLFW_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GLFW_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(GLFW_INSTALL        OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS   ON  CACHE BOOL "" FORCE)

add_subdirectory(glfw)

target_compile_options(glfw PRIVATE -Wno-nullability-completeness)

# ------   SDL   ------

set(SDL_SHARED       ON CACHE BOOL "" FORCE)
set(SDL_VIDEO        ON CACHE BOOL "" FORCE)
set(SDL_AUDIO		 ON CACHE BOOL "" FORCE)
set(SDL_STATIC       OFF CACHE BOOL "" FORCE)
set(SDL_CAMERA		 OFF CACHE BOOL "" FORCE)
set(SDL_TESTS		 OFF CACHE BOOL "" FORCE)
set(SDL_EXAMPLES	 OFF CACHE BOOL "" FORCE)
set(SDL3_MAINPROJECT OFF CACHE BOOL "" FORCE)
add_subdirectory(sdl)
target_link_libraries(SDL3-shared PRIVATE mojoexec)

# ----------------------

project("MojoLauncher")

# ------   .NET host (nethost, for .NET runtime hosting, e.g. Vintage Story)   ------
# The bundled libnethost.so is built for arm64-v8a only, so the .NET host is
# only compiled and linked into pojavexec on that ABI (blocking other ABIs from
# trying to link a non-existent library).

set(DOTNET_HOST_ENABLED OFF)
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(DOTNET_HOST_ENABLED ON)
endif()

if(DOTNET_HOST_ENABLED)
    add_library(nethost SHARED IMPORTED)
    set_target_properties(nethost PROPERTIES
            IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/dotnet_launcher/libnethost.so"
    )
    target_include_directories(nethost INTERFACE
            "${CMAKE_CURRENT_SOURCE_DIR}/dotnet_launcher/include"
    )
endif()

add_library(pojavexec SHARED
        affinity.c
        jvm_hooks/emui_iterator_fix_hook.c
        jvm_hooks/java_exec_hooks.c
        jvm_hooks/lwjgl_dlopen_hook.c
        jre_launcher/jre_launcher.c
        jre_launcher/native_library_hook.c
        jre_launcher/elf_hinter.c
        jre_launcher/abort_wait.c
        utils.c
        stdio_is.c
        minibridge.c
)

if(DOTNET_HOST_ENABLED)
    target_sources(pojavexec PRIVATE dotnet_launcher/dotnet_launcher.c)
endif()

target_include_directories(pojavexec PRIVATE .)
target_link_libraries(pojavexec PRIVATE log mojoexec)
if(DOTNET_HOST_ENABLED)
    target_link_libraries(pojavexec PRIVATE nethost)
endif()

add_library(pojavexec_awt SHARED
		awt/awt.c
		awt/awt_window.c
		awt/awt_util.c
		awt/awt_input.c
		awt/awt_clipboard.c
        awt/awt_mapper.c
)

target_link_libraries(pojavexec_awt PRIVATE android log)

add_library(awt_headless SHARED 
	awt_xawt/dummy.c
)

add_library(awt_xawt SHARED
        awt_xawt/xawt_fake.c
)
target_link_libraries(awt_xawt PRIVATE awt_headless)