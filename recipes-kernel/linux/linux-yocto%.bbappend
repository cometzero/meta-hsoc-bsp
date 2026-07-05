#
# SPDX-License-Identifier: MIT
#

LINUX_HSOC_APOLLO_REQUIRE ?= ""
LINUX_HSOC_APOLLO_REQUIRE:apollo-fvp = "linux-yocto-apollo-fvp.inc"
LINUX_HSOC_APOLLO_REQUIRE:apollo-qvp = "linux-yocto-apollo-qvp.inc"

require ${LINUX_HSOC_APOLLO_REQUIRE}
