#
# SPDX-License-Identifier: MIT
#

TRUSTED_FIRMWARE_M_HSOC_APOLLO_REQUIRE ?= ""
TRUSTED_FIRMWARE_M_HSOC_APOLLO_REQUIRE:apollo-fvp = "trusted-firmware-m-apollo-fvp.inc"
TRUSTED_FIRMWARE_M_HSOC_APOLLO_REQUIRE:apollo-qvp = "trusted-firmware-m-apollo-qvp.inc"

require ${TRUSTED_FIRMWARE_M_HSOC_APOLLO_REQUIRE}
