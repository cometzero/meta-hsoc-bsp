#
# SPDX-License-Identifier: MIT
#

inherit externalsrc

SCP_PLATFORM:pn-scp-firmware:apollo-fvp = "apollo-fvp"

EXTRA_OECMAKE:append:pn-scp-firmware:apollo-fvp = " \
    -D SCP_APOLLO_FVP_VARIANT_CFG1=${@bb.utils.contains('RD_ASPEN_VARIANT', 'cfg1', '1', '0', d)} \
"

EXTERNALSRC:pn-scp-firmware:apollo-fvp = "${HSOC_APOLLO_SCP_FIRMWARE_SRC}"
EXTERNALSRC_BUILD:pn-scp-firmware:apollo-fvp = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS:pn-scp-firmware:apollo-fvp = ""
