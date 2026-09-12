#
# SPDX-License-Identifier: MIT
#

TFM_PLATFORM:pn-trusted-firmware-m:apollo-qvp = "arm/rse/automotive_rd/apollo-qvp"

require trusted-firmware-m-apollo-common.inc

# Opt-in firmware-stage timer qualification; leave normal boot unchanged.
APOLLO_TFM_TIMER_TEST ?= "OFF"
EXTRA_OECMAKE:append:pn-trusted-firmware-m:apollo-qvp = " -DAPOLLO_TIMER_TEST=${APOLLO_TFM_TIMER_TEST}"
