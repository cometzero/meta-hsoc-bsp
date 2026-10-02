#
# SPDX-License-Identifier: MIT
#

ZEPHYR_BOARD:apollo-qvp = "apollo_qvp_safety_island_c1"

# Zephyr merges CONFIG_* CMake options after its Kconfig overlay files.
EXTRA_OECMAKE:append:apollo-qvp = " -DCONFIG_PFDI_MGMT_PERIOD_MS=${PFDI_SI_CL1_PERIOD_MS}"

require zephyr-demos-cl1-apollo-common.inc
