#
# SPDX-License-Identifier: MIT
#

inherit externalsrc

APOLLO_TFA_VARIANT = "${@oe.utils.conditional('RD_ASPEN_VARIANT', 'cfg1', 'cfg1', \
                                              bb.utils.contains('RD_ASPEN_VARIANT', 'rtl', \
                                              bb.utils.contains('RD_ASPEN_RTL_VARIANT', 'emu', 'emu', 'fpga', d), \
                                              'cfg2', d), d)}"

EXTRA_OEMAKE:append:pn-trusted-firmware-a = " \
    ${APOLLO_TFA_VARIANT_MAKE_VARIABLE}=${APOLLO_TFA_VARIANT} \
"

EXTERNALSRC:pn-trusted-firmware-a = "${HSOC_APOLLO_TFA_SRC}"
EXTERNALSRC_BUILD:pn-trusted-firmware-a = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS:pn-trusted-firmware-a = ""

# devtool modify keeps mbedTLS as a fetch dependency while the prepared
# source tree provides the checked-out submodule content.
SRC_URI:remove:pn-trusted-firmware-a = "${SRC_URI_MBEDTLS}"
SRC_URI:append:pn-trusted-firmware-a = " ${SRC_URI_MBEDTLS};type=git-dependency"
