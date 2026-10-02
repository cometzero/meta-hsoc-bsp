#
# SPDX-License-Identifier: MIT
#

# Apollo QVP timing policy for QBox co-simulation.
# Online diagnostic intervals in milliseconds. Keep the FVP defaults separate.
PFDI_AP_INTERVAL_MS ?= "3000"
PFDI_SI_CL1_PERIOD_MS ?= "3000"

# Monitor deadlines are C microsecond constants, not diagnostic intervals.
SCP_PFDI_OOR_PERIOD_US ?= "10000000UL"
SCP_PFDI_BOOT_TIMEOUT_US ?= "180000000UL"
SCP_PFDI_ONLINE_TIMEOUT_US ?= "60000000UL"
SCP_SICL1_PFDI_OOR_PERIOD_US ?= "1000000UL"
SCP_SICL1_PFDI_BOOT_TIMEOUT_US ?= "10000000UL"
SCP_SICL1_PFDI_ONLINE_TIMEOUT_US ?= "25000000UL"
