#
# SPDX-License-Identifier: MIT
#

ZEPHYR_BOARD = "apollo_fvp_safety_island_c1"

require zephyr-demos-cl1-apollo-common.inc

OVERLAY_HIPC = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}/overlays/hipc/${BOARD}"
OVERLAY_PFDI = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}/overlays/pfdi/${BOARD}"
OVERLAY_PFDI_AGENT = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}/overlays/pfdi_agent/${BOARD}"
OVERLAY_ZPERF = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}/overlays/zperf/${BOARD}"
