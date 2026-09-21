# SPDX-License-Identifier: MIT
SUMMARY = "Apollo standalone native QEMU system emulator"
DESCRIPTION = "Builds qemu-system-aarch64 with the Apollo machine from the local QEMU tree, independently of QBox and libqemu."
HOMEPAGE = "https://www.qemu.org/"
LICENSE = "GPL-2.0-only & LGPL-2.1-only & (GPL-2.0-or-later | BSD-3-Clause)"
LIC_FILES_CHKSUM = "file://LICENSE;md5=6541297aed25bfd5e8d893ea097e838c \
                    file://COPYING;md5=a3b50d8b88dcc0eb3d7d39b760b9e821 \
                    file://COPYING.LIB;md5=f4457173749eb816989d739d14ba7c13 \
                    file://${UNPACKDIR}/keycodemapdb/LICENSE.BSD;md5=5ae30ba4123bc4f2fa49aa0b0dce887b \
                    file://${UNPACKDIR}/keycodemapdb/LICENSE.GPL2;md5=751419260aa954499f7abaabaa882bbe"

inherit externalsrc pkgconfig python3native deploy native

EXTERNALSRC = "${HSOC_APOLLO_QEMU_SRC}"
EXTERNALSRC_BUILD = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS = ""
S = "${EXTERNALSRC}"
B = "${EXTERNALSRC_BUILD}"

# Match the existing local-QEMU provider's explicitly fetched secondary source.
# externalsrc deliberately skips upstream qemu recipe patches: this checkout
# already carries the native Python/Meson configure adaptations.
SRC_URI = "git://gitlab.com/qemu-project/keycodemapdb.git;protocol=https;nobranch=1;name=keycodemapdb;destsuffix=keycodemapdb;type=git-dependency"
SRCREV_keycodemapdb = "f5772a62ec52591ff6870b7e8ef32482371f22c6"
SRCREV_FORMAT = "keycodemapdb"

DEPENDS = "bison-native dtc-native glib-2.0-native libslirp-native \
           meson-native ninja-native pixman-native python3-setuptools-native \
           python3-wheel-native zlib-native"

# Keep the executable and data separate from the upstream qemu-system-native
# provider so both can populate one native sysroot without file collisions.
QEMU_APOLLO_BINDIR = "${libexecdir}/qemu-apollo"
QEMU_APOLLO_DATADIR = "${datadir}/qemu-apollo"
DEPLOY_DIR_IMAGE = "${DEPLOY_DIR}/qemu-apollo-native"

# QEMU configure does not accept the distro's autotools --disable-static.
DISABLE_STATIC = ""

EXTRA_OECONF = "--prefix=${prefix} \
                --bindir=${QEMU_APOLLO_BINDIR} \
                --datadir=${datadir} \
                --with-suffix=qemu-apollo \
                --target-list=aarch64-softmmu \
                --cc='${CC}' --cxx='${CXX}' --host-cc='${BUILD_CC}' \
                --extra-cflags='${CFLAGS}' --extra-ldflags='${LDFLAGS}' \
                --python=${PYTHON} \
                --keycodemapdb-src=${UNPACKDIR}/keycodemapdb \
                --without-default-features \
                --enable-tcg --enable-slirp --enable-fdt=system --disable-libqemu \
                --disable-download --disable-docs --disable-tests \
                --disable-tools --disable-guest-agent --disable-install-blobs \
                --disable-strip --disable-werror"

do_configure() {
    export PKG_CONFIG=pkg-config
    ${S}/configure ${EXTRA_OECONF}
}
do_configure[dirs] = "${B}"

do_compile() {
    ninja ${PARALLEL_MAKE} qemu-system-aarch64 trace/trace-events-all
}

do_check() {
    ${B}/qemu-system-aarch64 --version
    ${B}/qemu-system-aarch64 -machine help > ${B}/apollo-machines.txt
    grep -q '^apollo-qvp ' ${B}/apollo-machines.txt || \
        bbfatal "The local QEMU binary does not provide the apollo-qvp machine"
}
addtask check after do_compile before do_install

do_install() {
    install -d ${D}${QEMU_APOLLO_BINDIR} ${D}${QEMU_APOLLO_DATADIR}
    install -m 0755 ${B}/qemu-system-aarch64 ${D}${QEMU_APOLLO_BINDIR}/
    install -m 0644 ${B}/trace/trace-events-all ${D}${QEMU_APOLLO_DATADIR}/
}

python do_deploy() {
    import json
    import os

    native_root = d.getVar('STAGING_DIR_NATIVE')
    bindir = os.path.relpath(d.getVar('QEMU_APOLLO_BINDIR'), native_root)
    component = os.path.join(d.getVar('COMPONENTS_DIR'), d.getVar('PACKAGE_ARCH'), d.getVar('PN'))
    manifest = {
        'schema_version': 1,
        'executable': os.path.join(component, bindir, 'qemu-system-aarch64'),
        'library_path': [d.getVar('STAGING_LIBDIR_NATIVE'),
                         d.getVar('STAGING_BASE_LIBDIR_NATIVE')],
        'machine': 'apollo-qvp',
        'source': d.getVar('EXTERNALSRC'),
    }
    with open(os.path.join(d.getVar('DEPLOYDIR'), 'qemu-apollo-native.json'), 'w') as output:
        json.dump(manifest, output, indent=2, sort_keys=True)
        output.write('\n')
}
addtask deploy after do_populate_sysroot before do_build
