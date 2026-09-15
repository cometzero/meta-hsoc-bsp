#
# SPDX-License-Identifier: MIT
#

SCP_PLATFORM:pn-scp-firmware:apollo-fvp = "apollo-fvp"

SCP_SICL1_PFDI_ONLINE_TIMEOUT_US = "500000UL"

# Opt-in destructive validation only; do not advertise suspend in normal images.
SCP_APOLLO_FVP_ISOLATE_CL1 ?= "0"
APOLLO_FVP_TEST_SUSPEND_WAKE_US ?= "0"
# Platform retention contract; this does not enable system suspend by itself.
APOLLO_FVP_AP_SRAM_RETAINED ?= "1"
APOLLO_FVP_SUSPEND_KEEP_SYSTOP_ON ?= "1"

EXTRA_OECMAKE:append:pn-scp-firmware:apollo-fvp = " \
    -D SCP_SICL1_PFDI_ONLINE_TIMEOUT_US=${SCP_SICL1_PFDI_ONLINE_TIMEOUT_US} \
    -D SCP_APOLLO_FVP_ISOLATE_CL1=${SCP_APOLLO_FVP_ISOLATE_CL1} \
    -D APOLLO_FVP_TEST_SUSPEND_WAKE_US=${APOLLO_FVP_TEST_SUSPEND_WAKE_US} \
    -D APOLLO_FVP_AP_SRAM_RETAINED=${APOLLO_FVP_AP_SRAM_RETAINED} \
    -D APOLLO_FVP_SUSPEND_KEEP_SYSTOP_ON=${APOLLO_FVP_SUSPEND_KEEP_SYSTOP_ON} \
"

require scp-firmware-apollo-common.inc
