#
# SPDX-License-Identifier: MIT
#

require recipes-bsp/images/uefi-capsule-fvp-rd-aspen.bb

SUMMARY = "The UEFI capsule generation for ${APOLLO_UEFI_CAPSULE_MACHINE}"
DESCRIPTION = "A recipe to generate ${APOLLO_UEFI_CAPSULE_MACHINE} UEFI capsule using the RD-Aspen firmware layout."
COMPATIBLE_MACHINE = "${APOLLO_UEFI_CAPSULE_MACHINE}"
