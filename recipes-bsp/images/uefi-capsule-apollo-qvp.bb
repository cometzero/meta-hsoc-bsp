#
# SPDX-License-Identifier: MIT
#

APOLLO_UEFI_CAPSULE_MACHINE = "apollo-qvp"

require uefi-capsule-apollo-common.inc

do_image_uefi_capsule[depends] += "trusted-firmware-m:do_deploy"
