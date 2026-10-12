#
# SPDX-License-Identifier: MIT
#

ZEPHYR_BOARD = "apollo_qvp_safety_island_c1"

# Keep the QBox timing override after the board configuration.
EXTRA_OECMAKE:append = " -DCONFIG_PFDI_MGMT_PERIOD_MS=${PFDI_SI_CL1_PERIOD_MS}"

require zephyr-demos-cl1-apollo-common.inc

# The base zephyr-demos-cl1.bb appends feature overlays; QVP uses its board defaults.
EXTRA_OECMAKE:remove = "\
    -DDTC_OVERLAY_FILE='${OVERLAY_HIPC}.overlay${PFDI_AGENT_DTC_OVERLAY}' \
    -DOVERLAY_CONFIG='${OVERLAY_HIPC}.conf;${OVERLAY_ZPERF}.conf;${OVERLAY_PFDI}.conf${PFDI_AGENT_CONF_OVERLAY}' \
"
