#
# SPDX-License-Identifier: MIT
#

UBOOT_MACHINE:pn-u-boot:apollo-qvp = "apollo_qvp_defconfig"

# The QVP defconfig owns the platform and cfg2 configuration.
SRC_URI:remove:pn-u-boot:apollo-qvp = " \
    file://rd-aspen.cfg \
    file://rd-aspen-cfg2-extra.cfg \
"

SRC_URI:append:pn-u-boot:apollo-qvp:auto-ad-nexios = " \
    file://apollo-qvp-auto-ad-nexios.cfg \
"

EXTERNALSRC:pn-u-boot:apollo-qvp = "${HSOC_APOLLO_UBOOT_SRC}"
EXTERNALSRC_BUILD:pn-u-boot:apollo-qvp = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS:pn-u-boot:apollo-qvp = ""

APOLLO_QVP_UBOOT_DEFCONFIG ?= "apollo_qvp_defconfig"
APOLLO_QVP_UBOOT_DEFCONFIG_PATH ?= "${S}/configs/${APOLLO_QVP_UBOOT_DEFCONFIG}"

APOLLO_UBOOT_DEFCONFIG = "${APOLLO_QVP_UBOOT_DEFCONFIG}"
APOLLO_UBOOT_DEFCONFIG_PATH = "${APOLLO_QVP_UBOOT_DEFCONFIG_PATH}"

require u-boot-apollo-common.inc
