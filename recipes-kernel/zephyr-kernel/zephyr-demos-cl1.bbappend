#
# SPDX-License-Identifier: MIT
#

ZEPHYR_DEMOS_CL1_HSOC_APOLLO_REQUIRE ?= ""
ZEPHYR_DEMOS_CL1_HSOC_APOLLO_REQUIRE:apollo-fvp = "zephyr-demos-cl1-apollo-fvp.inc"
ZEPHYR_DEMOS_CL1_HSOC_APOLLO_REQUIRE:apollo-qvp = "zephyr-demos-cl1-apollo-qvp.inc"

require ${ZEPHYR_DEMOS_CL1_HSOC_APOLLO_REQUIRE}
