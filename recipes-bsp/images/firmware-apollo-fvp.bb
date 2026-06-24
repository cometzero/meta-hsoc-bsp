#
# SPDX-License-Identifier: MIT
#

require recipes-bsp/images/firmware-fvp-rd-aspen.bb

FILESEXTRAPATHS:prepend := "${ZENA_CSS_BSP_LAYER}/recipes-bsp/images/files:"

SUMMARY = "The firmware images for apollo-fvp"
DESCRIPTION = "A recipe to generate apollo-fvp firmware images using the RD-Aspen firmware layout."
COMPATIBLE_MACHINE = "apollo-fvp"
