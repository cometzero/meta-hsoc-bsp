#
# SPDX-License-Identifier: MIT
#

SCP_PLATFORM:pn-scp-firmware:apollo-fvp = "apollo-fvp"

SCP_SICL1_PFDI_ONLINE_TIMEOUT_US = "500000UL"

EXTRA_OECMAKE:append:pn-scp-firmware:apollo-fvp = " \
    -D SCP_SICL1_PFDI_ONLINE_TIMEOUT_US=${SCP_SICL1_PFDI_ONLINE_TIMEOUT_US} \
"

require scp-firmware-apollo-common.inc
