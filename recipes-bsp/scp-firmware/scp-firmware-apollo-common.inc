#
# SPDX-License-Identifier: MIT
#

inherit externalsrc

EXTRA_OECMAKE:append:pn-scp-firmware = " \
    -D SCP_APOLLO_FVP_VARIANT_CFG1=${@bb.utils.contains('RD_ASPEN_VARIANT', 'cfg1', '1', '0', d)} \
"

EXTERNALSRC:pn-scp-firmware = "${HSOC_APOLLO_SCP_FIRMWARE_SRC}"
EXTERNALSRC_BUILD:pn-scp-firmware = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS:pn-scp-firmware = ""
