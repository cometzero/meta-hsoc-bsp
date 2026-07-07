#
# SPDX-License-Identifier: MIT
#

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " file://0001-ports-linux-avoid-openat2-via-syscall.patch"
