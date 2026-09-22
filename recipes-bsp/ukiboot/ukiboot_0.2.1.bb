# SPDX-License-Identifier: MIT
SUMMARY = "AutoSD A/B UKI EFI boot manager"
HOMEPAGE = "https://gitlab.com/CentOS/automotive/src/ukiboot"
LICENSE = "LGPL-2.1-or-later"
LIC_FILES_CHKSUM = "file://COPYING.LIB;md5=4fbd65380cdd255951079008b364516c"

inherit meson pkgconfig externalsrc deploy systemd

COMPATIBLE_MACHINE = "^apollo-qvp$"
PACKAGE_ARCH = "${MACHINE_ARCH}"
EXTERNALSRC = "${HSOC_APOLLO_BASE}/../autosd/ukiboot"
EXTERNALSRC_BUILD = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS = ""

DEPENDS = "gnu-efi systemd openssl systemd-boot-native"
# EFI custom targets use Meson's compiler command rather than c_args. Keep
# both the loader and its GNU-EFI library within the firmware register ABI.
CC:append = " -mgeneral-regs-only"
EXTRA_OEMESON += "-Defi-includedir=${STAGING_INCDIR}/efi -Defi-libdir=${STAGING_LIBDIR} -Dopenssl=enabled"

# Upstream's addon targets infer the build host architecture. Build those
# explicitly with the target stub instead, keeping the external tree intact.
do_compile[depends] += "systemd-boot:do_deploy"
do_compile() {
    meson compile -C ${B} efi/ukibootaa64.efi ukibootctl ukibootimg
    for slot in a b; do
        ukify build --efi-arch=aa64 \
            --stub=${DEPLOY_DIR_IMAGE}/addonaa64.efi.stub \
            --cmdline="androidboot.slot_suffix=_$slot" \
            --output=${B}/slot_$slot.addon.efi
    done
}

do_install() {
    install -d ${D}${bindir} ${D}${libexecdir}/ukiboot/efi ${D}${systemd_system_unitdir}
    install -m 0755 ${B}/ukibootctl ${B}/ukibootimg ${D}${bindir}/
    install -m 0644 ${B}/efi/ukibootaa64.efi ${B}/slot_*.addon.efi ${D}${libexecdir}/ukiboot/efi/
    install -m 0644 ${S}/ukiboot-set-success.service ${D}${systemd_system_unitdir}/
}

SYSTEMD_SERVICE:${PN} = "ukiboot-set-success.service"
SYSTEMD_AUTO_ENABLE:${PN} = "disable"
FILES:${PN} += "${libexecdir}/ukiboot/efi"

do_deploy() {
    install -m 0644 ${B}/efi/ukibootaa64.efi ${B}/slot_*.addon.efi ${DEPLOYDIR}/
}
addtask deploy after do_compile before do_build
