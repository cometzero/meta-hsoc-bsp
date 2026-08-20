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

SCP_BOOT_ANCHOR = r"\[SI0_PLATFORM\] SCP started"

class LogTerminal(Protocol):
    logfile: TextIO


class SiPfdiTarget(Protocol):
    terminals: Mapping[str, LogTerminal]

    def transition(self, state: str) -> None: ...
    def expect(self, terminal: str, pattern: str, timeout: int) -> int: ...


class SiPfdiMonitorBspTest(OERuntimeTestCase):
    target: SiPfdiTarget
    td: Mapping[str, str]
    console = "scp"
    timeout = 120

    def setUp(self):
        super().setUp()
        self.target.transition("on")

    def _cpu_count(self) -> int:
        raw_count = self.td.get("SI_CL1_CPUS_COUNT", "")
        try:
            cpu_count = int(raw_count)
        except ValueError as error:
            raise AssertionError("invalid SI_CL1_CPUS_COUNT") from error
        if cpu_count < 1:
            raise AssertionError("invalid SI_CL1_CPUS_COUNT")
        return cpu_count

    def _console_log(self) -> str:
        try:
            logfile = self.target.terminals[self.console].logfile
        except KeyError as error:
            raise AssertionError("SCP console is unavailable") from error
        log_name = getattr(logfile, "name", "")
        if not isinstance(log_name, str) or not log_name:
            raise AssertionError("SCP console log is unavailable")
        try:
            logfile.flush()
        except ValueError:
            pass
        try:
            return Path(log_name).read_text(encoding="utf-8", errors="replace")
        except OSError as error:
            raise AssertionError("SCP console log is unreadable") from error

    def _require_once(self, log: str, pattern: str, label: str) -> None:
        matches = re.findall(pattern, log, flags=re.MULTILINE)
        if len(matches) != 1:
            raise AssertionError(f"{label}: expected once, found {len(matches)}")

    def _current_scp_segment(self, log: str) -> str:
        anchors = tuple(re.finditer(SCP_BOOT_ANCHOR, log))
        if not anchors:
            raise AssertionError("SCP boot anchor is unavailable")
        return log[anchors[-1].end():]

    @OETestDepends(["test_00_bsp_boot.BspBootTest.test_bsp_boot"])
    def test_si_pfdi_monitoring(self):
        cpu_count = self._cpu_count()
        self.target.transition("off")
        log = self._current_scp_segment(self._console_log())
        for core in range(cpu_count):
            self._require_once(
                log,
                rf"^.*\[PFDI_MONITOR\] Started PFDI monitoring for SI cluster 1 core {core}\s*$",
                f"SI cluster 1 core {core} start",
            )
            self._require_once(
                log,
                rf"^.*\[PFDI_MONITOR\] SI cluster 1 core {core} has been turned on, "
                r"switching on PFDI monitoring\s*$",
                f"SI cluster 1 core {core} power-on",
            )
        if re.search(r"\[PFDI_MONITOR\].*(?:timeout|fail(?:ed|ure)?)", log, re.IGNORECASE):
            raise AssertionError("SCP log reports a PFDI monitor timeout or failure")
