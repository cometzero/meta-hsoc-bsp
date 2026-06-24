#
# SPDX-License-Identifier: MIT
#

inherit externalsrc

UBOOT_MACHINE:pn-u-boot:apollo-fvp = "apollo_fvp_defconfig"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI:append:pn-u-boot:apollo-fvp:auto-ad-nexios = " \
    file://apollo-fvp-auto-ad-nexios.cfg \
"

EXTERNALSRC:pn-u-boot:apollo-fvp = "${HSOC_APOLLO_UBOOT_SRC}"
EXTERNALSRC_BUILD:pn-u-boot:apollo-fvp = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS:pn-u-boot:apollo-fvp = ""

APOLLO_FVP_UBOOT_DEFCONFIG ?= "apollo_fvp_defconfig"
APOLLO_FVP_UBOOT_DEFCONFIG_PATH ?= "${S}/configs/${APOLLO_FVP_UBOOT_DEFCONFIG}"

apollo_fvp_uboot_set_capsule_crt_file() {
    capsule_crt_path="${@__import__('os').path.relpath('${B}/CRT.crt', '${S}')}"

    bbnote "Setting CONFIG_EFI_CAPSULE_CRT_FILE to ${capsule_crt_path}"
    "${S}/scripts/config" --file "${B}/.config" \
        --set-str EFI_CAPSULE_CRT_FILE "${capsule_crt_path}"
}

do_defconfig() {
    bbplain "Applying ${APOLLO_FVP_UBOOT_DEFCONFIG} to ${B}/.config"
    oe_runmake -C ${S} O=${B} ${APOLLO_FVP_UBOOT_DEFCONFIG}
    apollo_fvp_uboot_set_capsule_crt_file
}
do_defconfig[dirs] = "${B}"
do_defconfig[nostamp] = "1"
addtask defconfig after do_configure

python apollo_fvp_uboot_menuconfig_prepare() {
    import os

    config = os.path.join(d.getVar("KCONFIG_CONFIG_ROOTDIR"), ".config")
    if os.path.exists(config):
        return

    bb.note("No %s found; applying %s before menuconfig" %
            (config, d.getVar("APOLLO_FVP_UBOOT_DEFCONFIG")))
    bb.build.exec_func("do_defconfig", d)
}
do_menuconfig[prefuncs] += "apollo_fvp_uboot_menuconfig_prepare"

do_savedefconfig() {
    if [ ! -f "${B}/.config" ]; then
        bbnote "No ${B}/.config found; applying ${APOLLO_FVP_UBOOT_DEFCONFIG}"
        oe_runmake -C ${S} O=${B} ${APOLLO_FVP_UBOOT_DEFCONFIG}
    fi

    oe_runmake -C ${S} O=${B} savedefconfig
    sed -i '/^CONFIG_EFI_CAPSULE_CRT_FILE=/d' "${B}/defconfig"
    install -m 0644 "${B}/defconfig" "${APOLLO_FVP_UBOOT_DEFCONFIG_PATH}"
    rm -f "${B}/defconfig"
    bbplain "Saved ${APOLLO_FVP_UBOOT_DEFCONFIG_PATH}"
}
do_savedefconfig[dirs] = "${B}"
do_savedefconfig[nostamp] = "1"

do_configure:append() {
    apollo_fvp_uboot_set_capsule_crt_file
}
