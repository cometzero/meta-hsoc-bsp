# SPDX-License-Identifier: MIT

if(CONFIG_PFDI_AGENT)
  zephyr_library()
  zephyr_library_sources(pfdi_agent_tx_irq.c)
endif()
