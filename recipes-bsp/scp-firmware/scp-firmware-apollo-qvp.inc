#
# SPDX-License-Identifier: MIT
#

SCP_PLATFORM:pn-scp-firmware:apollo-qvp = "apollo-qvp"

EXTRA_OECMAKE:append:pn-scp-firmware:apollo-qvp = " \
    -D SCP_SICL1_PFDI_ONLINE_TIMEOUT_US=250000UL \
"

require scp-firmware-apollo-common.inc
