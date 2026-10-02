#
# SPDX-License-Identifier: MIT
#

KMACHINE:pn-linux-yocto:apollo-qvp = "apollo-qvp"
KMACHINE:pn-linux-yocto-rt:apollo-qvp = "apollo-qvp"
KBUILD_DEFCONFIG:pn-linux-yocto:apollo-qvp = "apollo_qvp_defconfig"
KBUILD_DEFCONFIG:pn-linux-yocto-rt:apollo-qvp = "apollo_qvp_defconfig"
# apollo_qvp_defconfig is maintained by savedefconfig, so unspecified
# symbols must resolve to their Kconfig defaults instead of being forced off.
KCONFIG_MODE:pn-linux-yocto:apollo-qvp = "alldefconfig"
KCONFIG_MODE:pn-linux-yocto-rt:apollo-qvp = "alldefconfig"
KERNEL_DEVICETREE:apollo-qvp = "hsoc/apollo-qvp-saturn-v.dtb"

# BTF enables AutoSD systemd's BPF LSM programs. linux-yocto.inc stages
# pahole-native before configuration and permits pahole only in this mode.
KERNEL_DEBUG:apollo-qvp = "True"

APOLLO_QVP_KERNEL_FEATURES_REMOVE = "${APOLLO_KERNEL_FEATURES_REMOVE}"
APOLLO_QVP_KERNEL_DEFCONFIG ?= "apollo_qvp_defconfig"
APOLLO_QVP_KERNEL_DEFCONFIG_PATH ?= "${S}/arch/${ARCH}/configs/${APOLLO_QVP_KERNEL_DEFCONFIG}"

APOLLO_KERNEL_DEFCONFIG = "${APOLLO_QVP_KERNEL_DEFCONFIG}"
APOLLO_KERNEL_DEFCONFIG_PATH = "${APOLLO_QVP_KERNEL_DEFCONFIG_PATH}"

require linux-yocto-apollo-common.inc
