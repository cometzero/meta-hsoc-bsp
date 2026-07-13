#
# SPDX-License-Identifier: MIT
#

require recipes-bsp/images/firmware-fvp-rd-aspen.bb

FILESEXTRAPATHS:prepend := "${ZENA_CSS_BSP_LAYER}/recipes-bsp/images/files:"

SUMMARY = "The firmware images for ${APOLLO_FIRMWARE_MACHINE}"
DESCRIPTION = "A recipe to generate ${APOLLO_FIRMWARE_MACHINE} firmware images using the RD-Aspen firmware layout."
COMPATIBLE_MACHINE = "${APOLLO_FIRMWARE_MACHINE}"
