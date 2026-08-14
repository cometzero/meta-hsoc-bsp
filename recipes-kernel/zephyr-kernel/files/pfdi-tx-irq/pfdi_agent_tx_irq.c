/*
 * SPDX-License-Identifier: MIT
 */

#include <zephyr/device.h>
#include <zephyr/devicetree.h>
#include <zephyr/drivers/mbox.h>
#include <zephyr/init.h>
#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(pfdi_tx_irq);

#define PFDI_AGENT_NODE DT_NODELABEL(pfdi_agent)
#define PFDI_AGENT_MBOX_COUNT 4U

#define PFDI_MBOX_DT_SPEC_BY_IDX(idx)                                         \
	{                                                                      \
		.dev = DEVICE_DT_GET(                                            \
			DT_PHANDLE_BY_IDX(PFDI_AGENT_NODE, mboxes, idx)),          \
		.channel_id = DT_PHA_BY_IDX_OR(                                   \
			PFDI_AGENT_NODE, mboxes, idx, channel, 0),                  \
	}

BUILD_ASSERT(DT_PROP_LEN(PFDI_AGENT_NODE, mboxes) == PFDI_AGENT_MBOX_COUNT,
	     "PFDI TX IRQ integration expects four mailbox channels");

static const struct mbox_dt_spec channels[] = {
	PFDI_MBOX_DT_SPEC_BY_IDX(0),
	PFDI_MBOX_DT_SPEC_BY_IDX(1),
	PFDI_MBOX_DT_SPEC_BY_IDX(2),
	PFDI_MBOX_DT_SPEC_BY_IDX(3),
};

static int pfdi_tx_irq_init(void)
{
	for (size_t channel = 0; channel < ARRAY_SIZE(channels); channel++) {
		int ret;

		if (!mbox_is_ready_dt(&channels[channel])) {
			LOG_ERR("PFDI mailbox %zu is not ready", channel);
			return -ENODEV;
		}

		ret = mbox_set_enabled_dt(&channels[channel], true);
		if (ret < 0) {
			LOG_ERR("Failed to enable PFDI TX IRQ %zu: %d", channel,
				ret);
			return ret;
		}
	}

	LOG_INF("Enabled PFDI TX IRQs for %zu channels", ARRAY_SIZE(channels));

	return 0;
}

SYS_INIT(pfdi_tx_irq_init, APPLICATION, CONFIG_APPLICATION_INIT_PRIORITY);
