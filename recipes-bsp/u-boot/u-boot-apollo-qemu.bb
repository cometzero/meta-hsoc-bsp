# SPDX-License-Identifier: MIT
SUMMARY = "Standalone Apollo QEMU EFI firmware"
LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=2ca5f2c35c8cc335f0a19756634782f1"

inherit externalsrc deploy python3native kernel-arch

COMPATIBLE_MACHINE = "^apollo-qvp$"
PACKAGE_ARCH = "${MACHINE_ARCH}"
EXTERNALSRC = "${HSOC_APOLLO_UBOOT_SRC}"
EXTERNALSRC_BUILD = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS = ""
SRC_URI = "file://apollo-qemu.c file://apollo-qemu.cfg file://apollo-qemu.Kconfig"
DEPENDS = "flex-native bison-native swig-native dtc-native openssl-native gnutls-native python3-setuptools-native python3-pyelftools-native"

# externalsrc hashes the tracked source tree; the private copy permits this
# standalone board adaptation without modifying the full-platform source.
APOLLO_QEMU_SOURCE = "${B}/source"
EXTRA_OEMAKE = 'CROSS_COMPILE=${TARGET_PREFIX} CC="${TARGET_PREFIX}gcc ${TOOLCHAIN_OPTIONS} ${DEBUG_PREFIX_MAP}" HOSTCC="${BUILD_CC} ${BUILD_CFLAGS} ${BUILD_LDFLAGS}"'

python __anonymous() {
    d.appendVarFlag('do_configure', 'file-checksums', ' ${@srctree_hash_files(d)}')
}
do_configure[cleandirs] = "${APOLLO_QEMU_SOURCE}"

do_configure() {
    install -d ${APOLLO_QEMU_SOURCE}
    tar -C ${S} --exclude=.git --exclude=oe-workdir --exclude=oe-logs -cf - . | tar -C ${APOLLO_QEMU_SOURCE} -xf -
    install -m 0644 ${UNPACKDIR}/apollo-qemu.c ${APOLLO_QEMU_SOURCE}/board/emulation/qemu-arm/qemu-arm.c
    cat ${UNPACKDIR}/apollo-qemu.Kconfig >> ${APOLLO_QEMU_SOURCE}/board/emulation/qemu-arm/Kconfig
    oe_runmake -C ${APOLLO_QEMU_SOURCE} O=${B}/output qemu_arm64_defconfig
    ${APOLLO_QEMU_SOURCE}/scripts/kconfig/merge_config.sh -m -O ${B}/output ${B}/output/.config ${UNPACKDIR}/apollo-qemu.cfg
    oe_runmake -C ${APOLLO_QEMU_SOURCE} O=${B}/output olddefconfig
}

do_compile() {
    unset CFLAGS CPPFLAGS LDFLAGS
    oe_runmake -C ${APOLLO_QEMU_SOURCE} O=${B}/output all
}

do_install[noexec] = "1"
do_deploy() {
    install -m 0644 ${B}/output/u-boot.bin ${DEPLOYDIR}/u-boot-apollo-qemu.bin
    install -m 0644 ${B}/output/.config ${DEPLOYDIR}/u-boot-apollo-qemu.config
}
addtask deploy after do_compile before do_build
