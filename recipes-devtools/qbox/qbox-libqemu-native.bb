SUMMARY = "Apollo QVP native libqemu build"
DESCRIPTION = "Builds the local Apollo QBox libqemu dependency from hsoc-stack/tools/qemu and deploys the native aarch64 libqemu output for QBox runtime bundles."
HOMEPAGE = "https://github.com/qemu/qemu"
LICENSE = "GPL-2.0-only & LGPL-2.1-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=6541297aed25bfd5e8d893ea097e838c \
                    file://COPYING;md5=a3b50d8b88dcc0eb3d7d39b760b9e821 \
                    file://COPYING.LIB;md5=f4457173749eb816989d739d14ba7c13"

SRC_URI = "file://0001-qemu-cmake-add-headless-native-libqemu-option.patch"

inherit cmake externalsrc deploy native

EXTERNALSRC = "${HSOC_APOLLO_QEMU_SRC}"
EXTERNALSRC_BUILD = "${WORKDIR}/build"
S = "${EXTERNALSRC}"
B = "${EXTERNALSRC_BUILD}"
QBOX_LIBQEMU_NATIVE_CMAKE_DIR = "${WORKDIR}/qbox-libqemu-native-cmake"
OECMAKE_SOURCEPATH = "${QBOX_LIBQEMU_NATIVE_CMAKE_DIR}"

LIBQEMU_TARGETS = "aarch64"

DEPENDS = "glib-2.0-native \
           jpeg-native \
           libslirp-native \
           libsdl2-native \
           meson-native \
           ninja-native \
           pixman-native \
           pkgconfig-native \
           python3-native \
           zlib-native"

OECMAKE_C_FLAGS:append = " -isystem${STAGING_INCDIR_NATIVE}/SDL2"
OECMAKE_CXX_FLAGS:append = " -isystem${STAGING_INCDIR_NATIVE}/SDL2"

EXTRA_OECMAKE += "-DLIBQEMU_TARGETS=${LIBQEMU_TARGETS} \
                  -DLIBQEMU_BUILD_ALWAYS=OFF \
                  -DLIBQEMU_HEADLESS=ON \
                  -DLIBQEMU_PYTHON=${HOSTTOOLS_DIR}/python3 \
                  -DLIBQEMU_QEMU_SOURCE_DIR=${EXTERNALSRC}"

do_compile:prepend() {
    export CPATH="${STAGING_INCDIR_NATIVE}/SDL2${CPATH:+:${CPATH}}"
}

do_install:append() {
    rm -rf "${D}${datadir}/qemu" "${D}${datadir}/icons"
    rm -f "${D}${datadir}/applications/qemu.desktop"
    rmdir --ignore-fail-on-non-empty "${D}${datadir}/applications" || true
    rmdir --ignore-fail-on-non-empty "${D}${datadir}" || true
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

    if ! grep -q "option(LIBQEMU_HEADLESS" \
        "${QBOX_LIBQEMU_NATIVE_CMAKE_DIR}/qemu.cmake"; then
        patch -p1 -d "${QBOX_LIBQEMU_NATIVE_CMAKE_DIR}" < \
            "${UNPACKDIR}/0001-qemu-cmake-add-headless-native-libqemu-option.patch"
    fi
}

do_deploy() {
    qbox_libqemu_deploy_dir="${DEPLOYDIR}/qbox-apollo-qvp/libqemu"
    qbox_libqemu_share_dir="${B}/qemu-prefix/share"

    rm -rf "${qbox_libqemu_deploy_dir}"
    install -d "${qbox_libqemu_deploy_dir}"

    if [ ! -f "${D}${libdir}/libqemu-system-aarch64.so" ]; then
        bbfatal "Missing libqemu output: ${D}${libdir}/libqemu-system-aarch64.so"
    fi

    cp -R "${D}${libdir}" "${qbox_libqemu_deploy_dir}/"
    cp -R "${D}${includedir}" "${qbox_libqemu_deploy_dir}/"

    if [ -d "${qbox_libqemu_share_dir}" ]; then
        cp -R "${qbox_libqemu_share_dir}" "${qbox_libqemu_deploy_dir}/"
    elif [ -d "${D}${datadir}" ]; then
        cp -R "${D}${datadir}" "${qbox_libqemu_deploy_dir}/"
    fi

    cat > "${qbox_libqemu_deploy_dir}/manifest.txt" <<EOF
recipe=${PN}
version=${PV}
machine=${MACHINE}
source=${EXTERNALSRC}
build=${B}
targets=${LIBQEMU_TARGETS}
lib=lib/libqemu-system-aarch64.so
include=include/libqemu
share=share
EOF
}

addtask deploy after do_install before do_build
