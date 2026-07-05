#
# SPDX-License-Identifier: MIT
#

inherit externalsrc

SCP_PLATFORM:pn-scp-firmware:apollo-qvp = "apollo-qvp"

EXTRA_OECMAKE:append:pn-scp-firmware:apollo-qvp = " \
    -D SCP_APOLLO_FVP_VARIANT_CFG1=${@bb.utils.contains('RD_ASPEN_VARIANT', 'cfg1', '1', '0', d)} \
"

EXTERNALSRC:pn-scp-firmware:apollo-qvp = "${HSOC_APOLLO_SCP_FIRMWARE_SRC}"
EXTERNALSRC_BUILD:pn-scp-firmware:apollo-qvp = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS:pn-scp-firmware:apollo-qvp = ""
