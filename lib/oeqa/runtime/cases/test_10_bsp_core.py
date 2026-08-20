# SPDX-License-Identifier: MIT

from collections.abc import Mapping
import re
from typing import Protocol

from oeqa.core.decorator.depends import OETestDepends
from oeqa.runtime.case import OERuntimeTestCase


class BspTarget(Protocol):
    DEFAULT_CONSOLE: str

    def run(
        self,
        command: str,
        timeout: int | None = None,
    ) -> tuple[int, str]: ...

    def expect(self, terminal: str, pattern: str, timeout: int) -> int: ...


class BSPCoreTest(OERuntimeTestCase):
    target: BspTarget
    td: Mapping[str, str]

    def _expect(self, terminal: str, marker: str, message: str) -> None:
        result = self.target.expect(terminal, marker, timeout=180)
        self.assertEqual(result, 0, message)

    def _run(self, command: str, timeout: int = 180) -> str:
        status, output = self.target.run(command, timeout=timeout)
        self.assertEqual(status, 0, output)
        return output

    def _cpu_count(self) -> int:
        return int(self.td.get("PC_CPUS_COUNT", "4"))

    @OETestDepends(["test_00_bsp_boot.BspBootTest.test_bsp_boot"])
    def test_firmware_boot_chain(self):
        for terminal, marker, message in (
            ("rse", r"Init SCMI comm to SCP succeeded", "RSE-to-SCP handshake failed"),
            ("rse", r"RSE to SCP SCMI power on AP succeeded", "RSE did not power on AP"),
            ("rse", r"MeasuredBoot: Extending measurement for sw_type: BL2", "RSE measured boot is incomplete"),
            ("scp", r"CMN Discovery complete", "SCP did not discover CMN"),
            ("tf-a", r"Loading SP: SE Proxy", "SE Proxy secure partition was not loaded"),
            ("tf-a", r"Loading SP: SMM Gateway", "SMM Gateway secure partition was not loaded"),
            ("tf-a", r"I/TC: Primary CPU switching to normal world boot", "TF-A normal-world handoff failed"),
        ):
            self._expect(terminal, marker, message)
        handoff = self._run(
            "dmesg | grep -F 'efi: EFI v2.11 by Das U-Boot'"
        )
        self.assertIn("efi: EFI v2.11 by Das U-Boot", handoff)
        for cpu in range(1, self._cpu_count()):
            self._expect(
                "tf-a",
                rf"I/TC: Secondary CPU {cpu} switching to normal world boot",
                f"TF-A did not start configured CPU {cpu}",
            )

    @OETestDepends(["test_00_bsp_boot.BspBootTest.test_bsp_boot"])
    def test_safety_island_cl1(self):
        self._expect(
            "safety_island_c1",
            r"Booting Zephyr OS build",
            "SI CL1 Zephyr did not boot",
        )
        for cpu in range(1, 4):
            self._expect(
                "safety_island_c1",
                rf"Secondary CPU core {cpu} \(MPID:0x10{cpu}00\) is up",
                f"SI CL1 secondary CPU {cpu} is missing",
            )

    @OETestDepends(["test_00_bsp_boot.BspBootTest.test_bsp_boot"])
    def test_linux_topology_and_devices(self):
        cpu_count = self._cpu_count()
        dt_cpus = self._run(
            "find /sys/firmware/devicetree/base/cpus -maxdepth 1 -name 'cpu@*' | wc -l"
        )
        self.assertEqual(int(dt_cpus), cpu_count)
        self.assertEqual(int(self._run("nproc --all")), cpu_count)
        for cpu in range(cpu_count):
            self.assertEqual(
                self._run(f"cat /sys/devices/system/cpu/cpu{cpu}/cache/index3/size"),
                "4096K",
            )
            self.assertEqual(
                self._run(
                    f"cat /sys/devices/system/cpu/cpu{cpu}/cache/index3/shared_cpu_list"
                ),
                f"0-{cpu_count - 1}",
            )
        for event in ("event=0x002A", "event=0x002B"):
            output = self._run(
                f"perf stat -e arm_dsu_0/{event}/ -- "
                "dd if=/dev/zero of=/dev/null bs=1M count=64",
                timeout=500,
            )
            self.assertRegex(output, rf"[0-9]+\s+arm_dsu_0/{event}/")
        for command in (
            "test -e /dev/rtc0 && hwclock",
            "test -e /dev/watchdog0",
            "grep -qw virtio_rng.0 /sys/devices/virtual/misc/hw_random/rng_available",
            "grep -qw virtio_rng.0 /sys/devices/virtual/misc/hw_random/rng_current",
            "hexdump -n 32 /dev/hwrng | grep -q '[0-9a-f]'",
        ):
            self._run(command)
        secondary_cpus = " ".join(str(cpu) for cpu in range(1, cpu_count))
        self._run(
            "sh -c '"
            f'cpus="{secondary_cpus}"; rc=0; '
            "restore() { restore_rc=0; for cpu in $cpus; do "
            "echo 1 > /sys/devices/system/cpu/cpu$cpu/online || restore_rc=1; "
            "done; return $restore_rc; }; "
            'trap "restore || rc=1; trap - EXIT; exit \\$rc" EXIT; '
            "for cpu in $cpus; do "
            "echo 0 > /sys/devices/system/cpu/cpu$cpu/online || { rc=1; break; }; "
            'test "$(cat /sys/devices/system/cpu/cpu$cpu/online)" = 0 || '
            "{ rc=1; break; }; done; exit \"$rc\"'",
            timeout=500,
        )
        online = self._run("cat /sys/devices/system/cpu/online")
        self.assertRegex(online, re.compile(rf"^0-{cpu_count - 1}$"))
