#
# SPDX-License-Identifier: MIT
#

import os
import re

from wic import WicError
from wic.pluginbase import PluginMgr, SourcePlugin


class BootimgEFISlotPlugin(SourcePlugin):
    name = "bootimg-efi-slot"

    @staticmethod
    def _bootimg_efi_plugin():
        plugin = PluginMgr.get_plugins("source").get("bootimg-efi")
        if plugin is None:
            raise WicError("bootimg-efi-slot requires the bootimg-efi source plugin")
        return plugin

    @staticmethod
    def _slot_workdir(part, cr_workdir):
        slot = part.label or part.mountpoint or str(part.lineno)
        slot = re.sub(r"[^A-Za-z0-9_.-]+", "-", slot).strip("-")
        if not slot:
            slot = str(part.lineno)
        workdir = os.path.join(cr_workdir, "bootimg-efi-%s" % slot)
        os.makedirs(workdir, exist_ok=True)
        return workdir

    @classmethod
    def do_configure_partition(cls, part, source_params, creator, cr_workdir,
                               oe_builddir, bootimg_dir, kernel_dir,
                               native_sysroot):
        cls._bootimg_efi_plugin().do_configure_partition(
            part,
            source_params,
            creator,
            cls._slot_workdir(part, cr_workdir),
            oe_builddir,
            bootimg_dir,
            kernel_dir,
            native_sysroot,
        )

    @classmethod
    def do_stage_partition(cls, part, source_params, creator, cr_workdir,
                           oe_builddir, bootimg_dir, kernel_dir,
                           native_sysroot):
        cls._bootimg_efi_plugin().do_stage_partition(
            part,
            source_params,
            creator,
            cls._slot_workdir(part, cr_workdir),
            oe_builddir,
            bootimg_dir,
            kernel_dir,
            native_sysroot,
        )

    @classmethod
    def do_prepare_partition(cls, part, source_params, creator, cr_workdir,
                             oe_builddir, bootimg_dir, kernel_dir, rootfs_dir,
                             native_sysroot):
        cls._bootimg_efi_plugin().do_prepare_partition(
            part,
            source_params,
            creator,
            cls._slot_workdir(part, cr_workdir),
            oe_builddir,
            bootimg_dir,
            kernel_dir,
            rootfs_dir,
            native_sysroot,
        )

    @classmethod
    def do_post_partition(cls, part, source_params, creator, cr_workdir,
                          oe_builddir, bootimg_dir, kernel_dir, rootfs_dir,
                          native_sysroot):
        cls._bootimg_efi_plugin().do_post_partition(
            part,
            source_params,
            creator,
            cls._slot_workdir(part, cr_workdir),
            oe_builddir,
            bootimg_dir,
            kernel_dir,
            rootfs_dir,
            native_sysroot,
        )
