#
# SPDX-License-Identifier: MIT
#

FILESEXTRAPATHS:prepend := "${THISDIR}/files/apollo-qvp:"

require recipes-bsp/images/efi-capsule-update-disk-image-fvp-rd-aspen.bb

SUMMARY = "A disk image that contains an Apollo QVP UEFI update capsule"
DESCRIPTION = "A genimage based image recipe which generates an Apollo QVP disk image containing a UEFI update capsule."
COMPATIBLE_MACHINE = "apollo-qvp"
