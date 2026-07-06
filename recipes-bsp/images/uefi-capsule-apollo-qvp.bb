#
# SPDX-License-Identifier: MIT
#

require recipes-bsp/images/uefi-capsule-fvp-rd-aspen.bb

SUMMARY = "The UEFI capsule generation for apollo-qvp"
DESCRIPTION = "A recipe to generate apollo-qvp UEFI capsule using the RD-Aspen firmware layout."
COMPATIBLE_MACHINE = "apollo-qvp"

do_image_uefi_capsule[depends] += "trusted-firmware-m:do_deploy"
