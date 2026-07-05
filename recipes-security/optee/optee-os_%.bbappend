#
# SPDX-License-Identifier: MIT
#

OPTEE_OS_HSOC_APOLLO_REQUIRE ?= ""
OPTEE_OS_HSOC_APOLLO_REQUIRE:apollo-fvp = "optee-os-apollo-fvp.inc"
OPTEE_OS_HSOC_APOLLO_REQUIRE:apollo-qvp = "optee-os-apollo-qvp.inc"

require ${OPTEE_OS_HSOC_APOLLO_REQUIRE}
