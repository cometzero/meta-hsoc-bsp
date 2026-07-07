inherit cmake externalsrc pkgconfig python3native native

EXTERNALSRC_BUILD ?= "${WORKDIR}/build"
S = "${EXTERNALSRC}"
B = "${EXTERNALSRC_BUILD}"

QBOX_NATIVE_COMMON_DEPENDS ?= "libslirp-native \
                              pixman-native \
                              python3-setuptools-native \
                              python3-wheel-native \
                              zlib-native"
DEPENDS:append = " ${QBOX_NATIVE_COMMON_DEPENDS}"

QBOX_NATIVE_PACKAGECONFIG ?= ""
PACKAGECONFIG ??= "${QBOX_NATIVE_PACKAGECONFIG}"

PACKAGECONFIG[gtk] = "-DLIBQEMU_ENABLE_GTK=ON,-DLIBQEMU_ENABLE_GTK=OFF,gtk+3-native gettext-native"
PACKAGECONFIG[opengl] = "-DLIBQEMU_ENABLE_OPENGL=ON,-DLIBQEMU_ENABLE_OPENGL=OFF,libepoxy-native"
PACKAGECONFIG[sdl] = "-DLIBQEMU_ENABLE_SDL=ON,-DLIBQEMU_ENABLE_SDL=OFF,libsdl2-native"
PACKAGECONFIG[sdl-image] = "-DLIBQEMU_ENABLE_SDL_IMAGE=ON,-DLIBQEMU_ENABLE_SDL_IMAGE=OFF,libsdl2-image-native"
PACKAGECONFIG[vnc] = "-DLIBQEMU_ENABLE_VNC=ON,-DLIBQEMU_ENABLE_VNC=OFF"
PACKAGECONFIG[vnc-jpeg] = "-DLIBQEMU_ENABLE_VNC_JPEG=ON,-DLIBQEMU_ENABLE_VNC_JPEG=OFF,jpeg-native"

QBOX_NATIVE_SDL_CFLAGS = "${@bb.utils.contains('PACKAGECONFIG', 'sdl', ' -isystem${STAGING_INCDIR_NATIVE}/SDL2', '', d)}"
OECMAKE_C_FLAGS:append = "${QBOX_NATIVE_SDL_CFLAGS}"
OECMAKE_CXX_FLAGS:append = "${QBOX_NATIVE_SDL_CFLAGS}"

do_compile:prepend() {
    if ${@bb.utils.contains('PACKAGECONFIG', 'sdl', 'true', 'false', d)}; then
        export CPATH="${STAGING_INCDIR_NATIVE}/SDL2${CPATH:+:${CPATH}}"
    fi
}
