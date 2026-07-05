#
# SPDX-License-Identifier: MIT
#

U_BOOT_HSOC_APOLLO_REQUIRE ?= ""
U_BOOT_HSOC_APOLLO_REQUIRE:apollo-fvp = "u-boot-apollo-fvp.inc"
U_BOOT_HSOC_APOLLO_REQUIRE:apollo-qvp = "u-boot-apollo-qvp.inc"

require ${U_BOOT_HSOC_APOLLO_REQUIRE}
