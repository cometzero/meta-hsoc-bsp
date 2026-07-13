#
# SPDX-License-Identifier: MIT
#

OPTEE_OS_HSOC_APOLLO_REQUIRE ?= ""
OPTEE_OS_HSOC_APOLLO_REQUIRE:apollo-fvp = "optee-os-apollo-common.inc"
OPTEE_OS_HSOC_APOLLO_REQUIRE:apollo-qvp = "optee-os-apollo-common.inc"

require ${OPTEE_OS_HSOC_APOLLO_REQUIRE}
