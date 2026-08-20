#
# SPDX-License-Identifier: MIT
#

SCP_PLATFORM:pn-scp-firmware:apollo-qvp = "apollo-qvp"

EXTRA_OECMAKE:append:pn-scp-firmware:apollo-qvp = " \
    -D SCP_PFDI_BOOT_TIMEOUT_US=100000000UL \
    -D SCP_PFDI_ONLINE_TIMEOUT_US=500000UL \
    -D SCP_SICL1_PFDI_ONLINE_TIMEOUT_US=500000UL \
"

require scp-firmware-apollo-common.inc
