#
# SPDX-FileCopyrightText: <text>Copyright 2026 Arm Limited and/or its
# affiliates <open-source-office@arm.com></text>
#
# SPDX-License-Identifier: MIT
#
from pathlib import Path
import re
from typing import Mapping, Protocol, TextIO

from oeqa.core.decorator.depends import OETestDepends
from oeqa.runtime.case import OERuntimeTestCase


class LogTerminal(Protocol):
    logfile: TextIO


class ScpTarget(Protocol):
    @property
    def terminals(self) -> Mapping[str, LogTerminal]: ...

    def transition(self, state: str) -> None: ...
    def sendcontrol(self, terminal: str, control: str) -> None: ...
    def sendline(self, terminal: str, line: str = "") -> None: ...
    def expect(self, terminal: str, pattern: str, timeout: int) -> int: ...


class SmcfBspTest(OERuntimeTestCase):
    target: ScpTarget
    console = "scp"
    timeout = 120

    def setUp(self):
        super().setUp()
        self.target.transition("on")

    def _run_smcf(self):
        self.target.sendcontrol(self.console, "e")
        self.target.expect(
            self.console,
            r"\[CLI_DEBUGGER_MODULE\]\s+Entering CLI",
            timeout=self.timeout,
        )
        self.target.expect(self.console, r">", timeout=self.timeout)
        self.target.sendline(self.console, "test smcf")
        self.target.sendcontrol(self.console, "d")
        self.target.expect(
            self.console,
            r"\[CLI_DEBUGGER_MODULE\]\s+Exiting CLI",
            timeout=self.timeout,
        )
        self.target.expect(
            self.console,
            r"\[INTEGRATION_TEST\]\s+Start:\s*smcf",
            timeout=self.timeout,
        )
        self.target.expect(
            self.console,
            r"(?P<tests>[1-9]\d*)\s+Tests\s+0\s+Failures\s+0\s+Ignored",
            timeout=self.timeout,
        )
        self.target.expect(self.console, r"\bOK\b", timeout=self.timeout)
        self.target.expect(
            self.console,
            r"\[INTEGRATION_TEST\]\s+End:\s*smcf",
            timeout=self.timeout,
        )

    def _console_log(self) -> str:
        try:
            logfile = self.target.terminals[self.console].logfile
        except KeyError as error:
            raise AssertionError("SCP console is unavailable") from error
        log_name = getattr(logfile, "name", "")
        if not isinstance(log_name, str) or not log_name:
            raise AssertionError("SCP console log is unavailable")
        try:
            return Path(log_name).read_text(encoding="utf-8", errors="replace")
        except OSError as error:
            raise AssertionError("SCP console log is unreadable") from error

    @OETestDepends(["test_00_bsp_boot.BspBootTest.test_bsp_boot"])
    def test_01_smcf_client_start(self):
        self.target.expect(
            self.console,
            r"\[SMCF_CLIENT\]\s+start data_sampling for MGI\[\d+\]",
            timeout=self.timeout,
        )

    @OETestDepends(["test_21_bsp_smcf.SmcfBspTest.test_01_smcf_client_start"])
    def test_02_execute_smcf_test(self):
        self._run_smcf()

    @OETestDepends(["test_21_bsp_smcf.SmcfBspTest.test_02_execute_smcf_test"])
    def test_03_run_smcf_3x(self):
        for _ in range(3):
            self._run_smcf()

    @OETestDepends(["test_21_bsp_smcf.SmcfBspTest.test_03_run_smcf_3x"])
    def test_04_smcf_client_sensor_monitor(self):
        sensor_sample = (
            r"\[SMCF_CLIENT\]\s+Values for MGI\s+"
            r"[A-Z0-9_]+\s+MLI\s+\d+\s+\(Sensor\)\s*$\n"
            r".*\[SMCF_CLIENT\]\s+Value\[\d+\]\s+data\s+=\s+0x[0-9a-fA-F]+"
        )
        if not re.search(sensor_sample, self._console_log(), re.MULTILINE):
            raise AssertionError("SCP log has no documented SMCF sensor value")
