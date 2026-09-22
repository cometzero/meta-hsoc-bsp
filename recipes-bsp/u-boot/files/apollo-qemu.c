// SPDX-License-Identifier: GPL-2.0+
/* Standalone Apollo QEMU board: QEMU supplies the device tree in x0. */
#include <config.h>
#include <cpu_func.h>
#include <fdtdec.h>
#include <init.h>
#include <virtio.h>
#include <asm/global_data.h>
#include <asm/armv8/mmu.h>
#include <asm/system.h>
#include <dm/ofnode.h>
#include <linux/ioport.h>

DECLARE_GLOBAL_DATA_PTR;

/* Device space followed by the RAM banks decoded from QEMU's device tree. */
static struct mm_region apollo_mem_map[CONFIG_NR_DRAM_BANKS + 2] = {
	{
		.virt = 0,
		.phys = 0,
		.size = 0x80000000,
		.attrs = PTE_BLOCK_MEMTYPE(MT_DEVICE_NGNRNE) |
			 PTE_BLOCK_NON_SHARE | PTE_BLOCK_PXN | PTE_BLOCK_UXN,
	},
};

struct mm_region *mem_map = apollo_mem_map;

int board_fdt_blob_setup(void **fdtp)
{
	*fdtp = (void *)get_prev_bl_fdt_addr();
	return fdt_check_header(*fdtp);
}

int dram_init(void)
{
	struct resource res;
	ofnode mem = ofnode_null();
	int bank, reg = 0, ret;

	mem = ofnode_by_prop_value(mem, "device_type", "memory", 7);
	if (!ofnode_valid(mem))
		return -EINVAL;

	for (bank = 0; bank < CONFIG_NR_DRAM_BANKS; bank++) {
		ret = ofnode_read_resource(mem, reg++, &res);
		if (ret) {
			mem = ofnode_by_prop_value(mem, "device_type", "memory", 7);
			if (!ofnode_valid(mem))
				break;
			reg = 1;
			ret = ofnode_read_resource(mem, 0, &res);
			if (ret)
				return ret;
		}
		if (!bank) {
			gd->ram_base = res.start;
			gd->ram_size = resource_size(&res);
		}
		apollo_mem_map[bank + 1] = (struct mm_region) {
			.virt = res.start,
			.phys = res.start,
			.size = resource_size(&res),
			.attrs = PTE_BLOCK_MEMTYPE(MT_NORMAL) | PTE_BLOCK_INNER_SHARE,
		};
	}
	return 0;
}

int dram_init_banksize(void)
{
	return fdtdec_setup_memory_banksize();
}

void enable_caches(void)
{
	icache_enable();
	dcache_enable();
}

int board_late_init(void)
{
	virtio_init();
	return 0;
}
