#
# SPDX-License-Identifier: MIT
#

inherit externalsrc

TFA_PLATFORM:pn-trusted-firmware-a:apollo-fvp = "apollo_fvp"

EXTRA_OEMAKE:append:pn-trusted-firmware-a:apollo-fvp = " \
    APOLLO_FVP_VARIANT=${@oe.utils.conditional('RD_ASPEN_VARIANT', 'cfg1', 'cfg1', \
                                              bb.utils.contains('RD_ASPEN_VARIANT', 'rtl', \
                                              bb.utils.contains('RD_ASPEN_RTL_VARIANT', 'emu', 'emu', 'fpga', d), \
                                              'cfg2', d), d)} \
"

EXTERNALSRC:pn-trusted-firmware-a:apollo-fvp = "${HSOC_APOLLO_TFA_SRC}"
EXTERNALSRC_BUILD:pn-trusted-firmware-a:apollo-fvp = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS:pn-trusted-firmware-a:apollo-fvp = ""

# devtool modify keeps mbedTLS as a fetch dependency while the prepared
# source tree provides the checked-out submodule content.
SRC_URI:remove:pn-trusted-firmware-a:apollo-fvp = "${SRC_URI_MBEDTLS}"
SRC_URI:append:pn-trusted-firmware-a:apollo-fvp = " ${SRC_URI_MBEDTLS};type=git-dependency"
