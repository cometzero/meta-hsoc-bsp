# SPDX-License-Identifier: MIT

import re
from typing import Protocol

from oeqa.core.decorator.depends import OETestDepends
from oeqa.runtime.case import OERuntimeTestCase


class BspTarget(Protocol):
    def run(
        self,
        command: str,
        timeout: int | None = None,
    ) -> tuple[int, str]: ...

    def expect(
        self,
        terminal: str,
        pattern: str | re.Pattern[str],
        timeout: int,
    ) -> int: ...


class PFDIBspTest(OERuntimeTestCase):
    target: BspTarget
    cpu_count = 4
    primary_console = "default"
    scp_console = "scp"

    def _run_ok(self, command, timeout=120):
        status, output = self.target.run(command, timeout=timeout)
        self.assertEqual(status, 0, msg=f"command failed: {command}\n{output}")
        return output

    @OETestDepends(["test_00_bsp_boot.BspBootTest.test_bsp_boot"])
    def test_01_prerequisites(self):
        cpu_devices = " ".join(
            f"/dev/cpu/{cpu}/pfdi" for cpu in range(self.cpu_count)
        )
        output = self._run_ok(
            "for path in "
            f"{cpu_devices}; do test -c \"$path\" || exit 1; done; "
            "test -x /usr/bin/pfdi-cli; "
            "test -x /usr/bin/pfdi-sample-app; "
            "test -r /etc/pfdi/pfdi_test_config_0.pack; "
            "printf 'PFDI prerequisites OK\\n'"
        )

        self.assertIn("PFDI prerequisites OK", output)

    @OETestDepends(
        ["test_64_bsp_pfdi.PFDIBspTest.test_01_prerequisites"]
    )
    def test_02_service(self):
        output = self._run_ok(
            "pidof pfdi-sample-app; "
            "grep -F 'Loading config V1.0: running 4 tasks every 60 ms' "
            "/run/pfdi-sample-app.log"
        )

        self.assertIn("Loading config V1.0: running 4 tasks every 60 ms", output)
        self.assertNotRegex(output, r"(?i)(\[error\]|permission denied|timeout)")

    @OETestDepends(["test_64_bsp_pfdi.PFDIBspTest.test_02_service"])
    def test_03_cli(self):
        self.assertIn("libPFDI version: 1.0", self._run_ok("pfdi-cli --info"))
        firmware_info = self._run_ok("pfdi-cli --pfdi_info 0")
        self.assertRegex(
            firmware_info,
            r"Stub firmware detected|PFDI firmware version",
        )
        for cpu in range(self.cpu_count):
            with self.subTest(cpu=cpu, operation="count"):
                count = self._run_ok(f"pfdi-cli --count {cpu}")
                self.assertRegex(
                    count,
                    rf"CPU{cpu}: Firmware reports 41 available diagnostic tests",
                )
            with self.subTest(cpu=cpu, operation="oor"):
                result = self._run_ok(f"pfdi-cli --result {cpu}", timeout=300)
                self.assertRegex(
                    result,
                    rf"CPU{cpu}: Out of Reset \(OoR\) test OK",
                )

    @OETestDepends(["test_64_bsp_pfdi.PFDIBspTest.test_03_cli"])
    def test_04_online(self):
        output = self._run_ok(
            "pfdi-sample-app -ivc /etc/pfdi/pfdi_test_config_0.pack "
            "-m single",
            timeout=300,
        )

        for cpu in range(self.cpu_count):
            self.assertRegex(
                output,
                rf"CPU{cpu}: PFDI Online \(OnL\) test \(0 - 40\) OK",
            )

    @OETestDepends(["test_64_bsp_pfdi.PFDIBspTest.test_04_online"])
    def test_05_monitoring_started(self):
        for cpu in range(self.cpu_count):
            self.target.expect(
                self.scp_console,
                rf"Started PFDI monitoring for AP cluster 0 core {cpu}",
                timeout=120,
            )

    @OETestDepends(
        ["test_64_bsp_pfdi.PFDIBspTest.test_05_monitoring_started"]
    )
    def test_90_force_error(self):
        for cpu in range(self.cpu_count):
            output = self._run_ok(f"pfdi-cli --force_error {cpu} RUN ERROR")
            self.assertRegex(output, rf"CPU{cpu}: injected force error")
            wait_for_failure = (
                "attempt=0; "
                f"until grep -F 'CPU{cpu}: PFDI Online (OnL) test failed: "
                "Input/output error (errno=5)' /run/pfdi-sample-app.log; do "
                "attempt=$((attempt + 1)); "
                "test \"$attempt\" -lt 180 || exit 1; "
                "sleep 1; done"
            )
            self._run_ok(wait_for_failure, timeout=190)

    @OETestDepends(["test_64_bsp_pfdi.PFDIBspTest.test_90_force_error"])
    def test_91_fault_propagation(self):
        for cpu in range(self.cpu_count):
            self.target.expect(
                self.scp_console,
                re.compile(r"\[FMU\] (?:Non-critical|Critical) fault received:"),
                timeout=180,
            )
            self.target.expect(
                self.scp_console,
                rf"\[SBISTC\] SBISTC_EQ_FAIL_CORE{cpu} detected",
                timeout=180,
            )
            self.target.expect(
                self.scp_console,
                rf"\[PFDI_MONITOR\] Onl PFDI for AP cluster 0 core {cpu} "
                r"failed, stopping PFDI monitoring",
                timeout=180,
            )
