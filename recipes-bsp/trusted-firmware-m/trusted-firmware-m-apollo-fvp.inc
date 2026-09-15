#
# SPDX-License-Identifier: MIT
#

TFM_PLATFORM:pn-trusted-firmware-m:apollo-fvp = "arm/rse/automotive_rd/apollo-fvp"

# Match SCP's opt-in CL1 isolation in disposable AP suspend experiments.
SCP_APOLLO_FVP_ISOLATE_CL1 ?= "0"
EXTRA_OECMAKE:append:pn-trusted-firmware-m:apollo-fvp = " \
    -DAPOLLO_FVP_ISOLATE_CL1=${SCP_APOLLO_FVP_ISOLATE_CL1} \
"

require trusted-firmware-m-apollo-common.inc
