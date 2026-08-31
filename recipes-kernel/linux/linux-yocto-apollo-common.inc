#
# SPDX-License-Identifier: MIT
#

inherit externalsrc

FILESEXTRAPATHS:prepend := "${THISDIR}:${THISDIR}/files:"

APOLLO_KERNEL_CONFIG_FRAGMENT ?= ""
APOLLO_KERNEL_CONFIG_FRAGMENT_DIR := "${THISDIR}/files"
APOLLO_KERNEL_CONFIG_FRAGMENT_PATH = "${APOLLO_KERNEL_CONFIG_FRAGMENT_DIR}/${APOLLO_KERNEL_CONFIG_FRAGMENT}"

EXTERNALSRC:pn-linux-yocto = "${HSOC_APOLLO_LINUX_SRC}"
EXTERNALSRC_BUILD:pn-linux-yocto = "${WORKDIR}/build"
EXTERNALSRC:pn-linux-yocto-rt = "${HSOC_APOLLO_LINUX_SRC}"
EXTERNALSRC_BUILD:pn-linux-yocto-rt = "${WORKDIR}/build"

SRC_URI:remove = "${SRC_URI_EXTRAS_KMETA}"
SRC_URI:remove = "file://arm-auto-solutions-kmeta-extra;type=kmeta;name=arm-auto-solutions-kmeta-extra;destsuffix=arm-auto-solutions-kmeta-extra"
SRC_URI:remove = "file://apollo-kmeta;type=kmeta;name=apollo-kmeta;destsuffix=apollo-kmeta"
SRC_URI:remove = "file://modsign.scc file://modsign.cfg"
SRC_URI:remove = "file://extra-configs.cfg"
SRC_URI:remove = "file://arm-ffa-transport.cfg"

APOLLO_KERNEL_FEATURES_REMOVE = "\
    cfg/efi.scc \
    features/arm-mhuv3/arm-mhuv3.scc \
    features/bpf/bpf.scc \
    features/mailbox/mailbox.scc \
    features/remoteproc/remoteproc.scc \
    features/rpmsg-virtio/rpmsg-virtio.scc \
    features/vswitch/vswitch.scc \
    features/xen/xen.scc \
    features/arm-pci-ahci/arm-pci-ahci.scc \
    features/ras/arm-ras-dt.scc \
    features/podman/podman.scc \
    ${KERNEL_CACHE_FEATURES} \
"
KERNEL_FEATURES:remove = "${APOLLO_KERNEL_FEATURES_REMOVE}"

do_kernel_configme() {
    bbplain "Applying ${APOLLO_KERNEL_DEFCONFIG} to ${B}/.config"
    oe_runmake -C ${S} O=${B} ${APOLLO_KERNEL_DEFCONFIG}
    if [ -n "${APOLLO_KERNEL_CONFIG_FRAGMENT}" ]; then
        ${S}/scripts/kconfig/merge_config.sh -m -O ${B} \
            ${B}/.config ${APOLLO_KERNEL_CONFIG_FRAGMENT_PATH}
        oe_runmake -C ${S} O=${B} olddefconfig
    fi
}
do_kernel_configme[file-checksums] += "${APOLLO_KERNEL_DEFCONFIG_PATH}:True ${@d.getVar('APOLLO_KERNEL_CONFIG_FRAGMENT_PATH') + ':True' if d.getVar('APOLLO_KERNEL_CONFIG_FRAGMENT') else ''}"

do_defconfig() {
    do_kernel_configme
}
do_defconfig[dirs] = "${B}"
do_defconfig[nostamp] = "1"
addtask defconfig after do_configure

python apollo_kernel_menuconfig_prepare() {
    import os

    config = os.path.join(d.getVar("KCONFIG_CONFIG_ROOTDIR"), ".config")
    if os.path.exists(config):
        return

    bb.note("No %s found; applying %s before menuconfig" %
            (config, d.getVar("APOLLO_KERNEL_DEFCONFIG")))
    bb.build.exec_func("do_defconfig", d)
}
do_menuconfig[prefuncs] += "apollo_kernel_menuconfig_prepare"

do_savedefconfig() {
    if [ ! -f "${B}/.config" ]; then
        bbnote "No ${B}/.config found; applying ${APOLLO_KERNEL_DEFCONFIG}"
        oe_runmake -C ${S} O=${B} ${APOLLO_KERNEL_DEFCONFIG}
    fi

    oe_runmake -C ${S} O=${B} savedefconfig
    install -m 0644 "${B}/defconfig" "${APOLLO_KERNEL_DEFCONFIG_PATH}"
    rm -f "${B}/defconfig"
    bbplain "Saved ${APOLLO_KERNEL_DEFCONFIG_PATH}"
}
do_savedefconfig[dirs] = "${B}"
do_savedefconfig[nostamp] = "1"

do_shared_workdir:append() {
    if [ "${MODSIGN_ENABLED}" = "1" ]; then
        if [ ! -f "${B}/modsign_key.pem" ]; then
            bbfatal "modsign is enabled, but ${B}/modsign_key.pem is missing"
        fi
        install -m 0600 "${B}/modsign_key.pem" \
            "${STAGING_KERNEL_BUILDDIR}/modsign_key.pem"
    fi
}
