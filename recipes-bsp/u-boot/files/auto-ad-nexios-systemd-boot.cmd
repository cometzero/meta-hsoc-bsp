# SPDX-License-Identifier: MIT

if test "${aanx_slot}" = "A"; then
    setenv aanx_systemd_uki EFI/Linux/nexios-a.efi
    setenv aanx_systemd_entry =H6e006500780069006f0073002d0061002e006500660069000000
    setenv aanx_fallback_systemd_uki EFI/Linux/nexios-b.efi
    setenv aanx_fallback_systemd_entry =H6e006500780069006f0073002d0062002e006500660069000000
else
    setenv aanx_systemd_uki EFI/Linux/nexios-b.efi
    setenv aanx_systemd_entry =H6e006500780069006f0073002d0062002e006500660069000000
    setenv aanx_fallback_systemd_uki EFI/Linux/nexios-a.efi
    setenv aanx_fallback_systemd_entry =H6e006500780069006f0073002d0061002e006500660069000000
fi

if test -e virtio 0:${aanx_boot_part} ${aanx_systemd_uki}; then
    echo auto-ad-nexios: chainloading systemd-boot for slot ${aanx_slot}
    if env set -e -guid 4a67b082-0a4c-41cf-b6c7-440b29bb8c4f -nv -bs -rt LoaderEntryOneShot ${aanx_systemd_entry}; then
        if load virtio 0:${aanx_boot_part} ${loadaddr} EFI/BOOT/bootaa64.efi; then
            bootefi ${loadaddr}
        fi
    fi
fi

if test "${aanx_fallback_valid}" = "1"; then
    echo auto-ad-nexios: trying fallback ${aanx_fallback_uki}
    if test -e virtio 0:${aanx_boot_part} ${aanx_fallback_systemd_uki}; then
        if env set -e -guid 4a67b082-0a4c-41cf-b6c7-440b29bb8c4f -nv -bs -rt LoaderEntryOneShot ${aanx_fallback_systemd_entry}; then
            if load virtio 0:${aanx_boot_part} ${loadaddr} EFI/BOOT/bootaa64.efi; then
                bootefi ${loadaddr}
            fi
        fi
    fi
fi
