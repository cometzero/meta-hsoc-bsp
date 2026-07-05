#
# SPDX-License-Identifier: MIT
#

SCP_FIRMWARE_HSOC_APOLLO_REQUIRE ?= ""
SCP_FIRMWARE_HSOC_APOLLO_REQUIRE:apollo-fvp = "scp-firmware-apollo-fvp.inc"
SCP_FIRMWARE_HSOC_APOLLO_REQUIRE:apollo-qvp = "scp-firmware-apollo-qvp.inc"

require ${SCP_FIRMWARE_HSOC_APOLLO_REQUIRE}
