#
# SPDX-License-Identifier: MIT
#

require recipes-bsp/images/firmware-fvp-rd-aspen.bb

FILESEXTRAPATHS:prepend := "${ZENA_CSS_BSP_LAYER}/recipes-bsp/images/files:"

SUMMARY = "The firmware images for apollo-qvp"
DESCRIPTION = "A recipe to generate apollo-qvp firmware images using the RD-Aspen firmware layout."
COMPATIBLE_MACHINE = "apollo-qvp"
