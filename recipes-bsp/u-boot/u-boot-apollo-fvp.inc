#
# SPDX-License-Identifier: MIT
#

UBOOT_MACHINE:pn-u-boot:apollo-fvp = "apollo_fvp_defconfig"

SRC_URI:append:pn-u-boot:apollo-fvp:auto-ad-nexios = " \
    file://apollo-fvp-auto-ad-nexios.cfg \
"

EXTERNALSRC:pn-u-boot:apollo-fvp = "${HSOC_APOLLO_UBOOT_SRC}"
EXTERNALSRC_BUILD:pn-u-boot:apollo-fvp = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS:pn-u-boot:apollo-fvp = ""

APOLLO_FVP_UBOOT_DEFCONFIG ?= "apollo_fvp_defconfig"
APOLLO_FVP_UBOOT_DEFCONFIG_PATH ?= "${S}/configs/${APOLLO_FVP_UBOOT_DEFCONFIG}"

APOLLO_UBOOT_DEFCONFIG = "${APOLLO_FVP_UBOOT_DEFCONFIG}"
APOLLO_UBOOT_DEFCONFIG_PATH = "${APOLLO_FVP_UBOOT_DEFCONFIG_PATH}"

require u-boot-apollo-common.inc
