SUMMARY = "Apollo QVP native libqemu sysroot provider"
DESCRIPTION = "Builds the local Apollo QBox libqemu dependency from hsoc-stack/tools/qemu and installs the native aarch64 libqemu output into the native sysroot."
HOMEPAGE = "https://github.com/qemu/qemu"
LICENSE = "GPL-2.0-only & LGPL-2.1-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=6541297aed25bfd5e8d893ea097e838c \
                    file://COPYING;md5=a3b50d8b88dcc0eb3d7d39b760b9e821 \
                    file://COPYING.LIB;md5=f4457173749eb816989d739d14ba7c13"

require qbox-native-common.inc

EXTERNALSRC = "${HSOC_APOLLO_QEMU_SRC}"
QBOX_LIBQEMU_NATIVE_CMAKE_DIR = "${WORKDIR}/qbox-libqemu-native-cmake"
OECMAKE_SOURCEPATH = "${QBOX_LIBQEMU_NATIVE_CMAKE_DIR}"

LIBQEMU_TARGETS = "aarch64"
QBOX_LIBQEMU_NATIVE_PACKAGECONFIG ?= "opengl sdl vnc vnc-jpeg"
PACKAGECONFIG ??= "${QBOX_LIBQEMU_NATIVE_PACKAGECONFIG}"

PACKAGECONFIG[gtk] = "-DLIBQEMU_ENABLE_GTK=ON,-DLIBQEMU_ENABLE_GTK=OFF,gtk+3-native gettext-native"
PACKAGECONFIG[opengl] = ",,libepoxy-native"
PACKAGECONFIG[sdl] = ",,libsdl2-native"
PACKAGECONFIG[sdl-image] = "-DLIBQEMU_ENABLE_SDL_IMAGE=ON,-DLIBQEMU_ENABLE_SDL_IMAGE=OFF,libsdl2-image-native"
PACKAGECONFIG[vnc] = ",,"
PACKAGECONFIG[vnc-jpeg] = ",,jpeg-native"

QBOX_LIBQEMU_NATIVE_SDL_CFLAGS = "${@bb.utils.contains('PACKAGECONFIG', 'sdl', ' -isystem${STAGING_INCDIR_NATIVE}/SDL2', '', d)}"
OECMAKE_C_FLAGS:append = "${QBOX_LIBQEMU_NATIVE_SDL_CFLAGS}"
OECMAKE_CXX_FLAGS:append = "${QBOX_LIBQEMU_NATIVE_SDL_CFLAGS}"

do_compile:prepend() {
    if ${@bb.utils.contains('PACKAGECONFIG', 'sdl', 'true', 'false', d)}; then
        export CPATH="${STAGING_INCDIR_NATIVE}/SDL2${CPATH:+:${CPATH}}"
    fi
}

DEPENDS = "glib-2.0-native \
           meson-native"

EXTRA_OECMAKE += "-DLIBQEMU_TARGETS=${LIBQEMU_TARGETS} \
                  -DLIBQEMU_BUILD_ALWAYS=OFF \
                  -DLIBQEMU_PYTHON=${PYTHON} \
                  -DLIBQEMU_EXTRA_CONFIGURE_ARGS=--disable-download \
                  -DLIBQEMU_QEMU_SOURCE_DIR=${EXTERNALSRC}"

do_install:append() {
    # These host QEMU data files are also staged by qemu-native.
    rm -f "${D}${datadir}/qemu/trace-events-all"
    rm -rf "${D}${datadir}/qemu/keymaps"
    rm -rf "${D}${datadir}/icons"
    rm -f "${D}${datadir}/applications/qemu.desktop"
    rmdir --ignore-fail-on-non-empty "${D}${datadir}/applications" || true
    rmdir --ignore-fail-on-non-empty "${D}${datadir}" || true

    for qbox_libqemu_required in \
        "${D}${libdir}/libqemu-system-aarch64.so" \
        "${D}${libdir}/cmake/libqemu/libqemuConfig.cmake" \
        "${D}${includedir}/libqemu"
    do
        if [ ! -e "${qbox_libqemu_required}" ]; then
            bbfatal "Missing libqemu sysroot output: ${qbox_libqemu_required}"
        fi
    done

    if [ -d "${B}/qemu-prefix/share/qemu" ] && [ ! -d "${D}${datadir}/qemu" ]; then
        bbfatal "Missing libqemu sysroot data: ${D}${datadir}/qemu"
    fi
}

do_configure:prepend() {
    if [ ! -d "${EXTERNALSRC}" ]; then
        bbfatal "HSOC_APOLLO_QEMU_SRC does not exist: ${EXTERNALSRC}"
    fi
    if [ ! -f "${EXTERNALSRC}/CMakeLists.txt" ] || [ ! -x "${EXTERNALSRC}/configure" ]; then
        bbfatal "HSOC_APOLLO_QEMU_SRC is not a QEMU/libqemu source tree: ${EXTERNALSRC}"
    fi

    rm -rf "${QBOX_LIBQEMU_NATIVE_CMAKE_DIR}"
    install -d "${QBOX_LIBQEMU_NATIVE_CMAKE_DIR}"
    install -m 0644 "${EXTERNALSRC}/CMakeLists.txt" \
        "${QBOX_LIBQEMU_NATIVE_CMAKE_DIR}/CMakeLists.txt"
    install -m 0644 "${EXTERNALSRC}/qemu.cmake" \
        "${QBOX_LIBQEMU_NATIVE_CMAKE_DIR}/qemu.cmake"
    install -m 0644 "${EXTERNALSRC}/libqemuConfig.cmake.in" \
        "${QBOX_LIBQEMU_NATIVE_CMAKE_DIR}/libqemuConfig.cmake.in"
}
