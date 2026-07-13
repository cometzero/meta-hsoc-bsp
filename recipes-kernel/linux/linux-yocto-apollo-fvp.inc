#
# SPDX-License-Identifier: MIT
#

KMACHINE:pn-linux-yocto:apollo-fvp = "apollo-fvp"
KMACHINE:pn-linux-yocto-rt:apollo-fvp = "apollo-fvp"
KBUILD_DEFCONFIG:pn-linux-yocto:apollo-fvp = "apollo_fvp_defconfig"
KBUILD_DEFCONFIG:pn-linux-yocto-rt:apollo-fvp = "apollo_fvp_defconfig"
# apollo_fvp_defconfig is maintained by savedefconfig, so unspecified
# symbols must resolve to their Kconfig defaults instead of being forced off.
KCONFIG_MODE:pn-linux-yocto:apollo-fvp = "alldefconfig"
KCONFIG_MODE:pn-linux-yocto-rt:apollo-fvp = "alldefconfig"
KERNEL_DEVICETREE:apollo-fvp = "arm/apollo-fvp.dtb"

APOLLO_FVP_KERNEL_FEATURES_REMOVE = "${APOLLO_KERNEL_FEATURES_REMOVE}"
APOLLO_FVP_KERNEL_DEFCONFIG ?= "apollo_fvp_defconfig"
APOLLO_FVP_KERNEL_DEFCONFIG_PATH ?= "${S}/arch/${ARCH}/configs/${APOLLO_FVP_KERNEL_DEFCONFIG}"

APOLLO_KERNEL_DEFCONFIG = "${APOLLO_FVP_KERNEL_DEFCONFIG}"
APOLLO_KERNEL_DEFCONFIG_PATH = "${APOLLO_FVP_KERNEL_DEFCONFIG_PATH}"

require linux-yocto-apollo-common.inc
