#
# SPDX-License-Identifier: MIT
#

TRUSTED_FIRMWARE_A_HSOC_APOLLO_REQUIRE ?= ""
TRUSTED_FIRMWARE_A_HSOC_APOLLO_REQUIRE:apollo-fvp = "trusted-firmware-a-apollo-fvp.inc"
TRUSTED_FIRMWARE_A_HSOC_APOLLO_REQUIRE:apollo-qvp = "trusted-firmware-a-apollo-qvp.inc"

require ${TRUSTED_FIRMWARE_A_HSOC_APOLLO_REQUIRE}
