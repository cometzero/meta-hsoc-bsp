#
# SPDX-License-Identifier: MIT
#

SUMMARY = "Auto AD Nexios systemd-boot handoff script"
DESCRIPTION = "Deploys the Apollo product A/B systemd-boot handoff script."
HOMEPAGE = "https://gitlab.arm.com/automotive-and-industrial/arm-auto-solutions"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://auto-ad-nexios-systemd-boot.cmd"

S = "${UNPACKDIR}"
B = "${WORKDIR}/build"

DEPENDS = "u-boot-mkimage-native"

inherit deploy

do_configure[noexec] = "1"
do_install[noexec] = "1"

do_compile() {
    install -d ${B}
    uboot-mkimage -A arm64 -T script -C none \
        -n "Auto AD Nexios systemd-boot handoff" \
        -d ${UNPACKDIR}/auto-ad-nexios-systemd-boot.cmd \
        ${B}/auto-ad-nexios-systemd-boot.scr
}

do_deploy() {
    install -Dm0644 ${B}/auto-ad-nexios-systemd-boot.scr \
        ${DEPLOYDIR}/auto-ad-nexios-systemd-boot.scr
}

addtask deploy after do_compile before do_build
