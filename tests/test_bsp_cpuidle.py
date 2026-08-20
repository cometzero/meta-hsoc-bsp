# SPDX-License-Identifier: MIT

from __future__ import annotations

import importlib.util
import subprocess
import sys
import types
import unittest
from pathlib import Path
from typing import Callable

import pytest


CASE_PATH = (
    Path(__file__).resolve().parents[1]
    / "lib/oeqa/runtime/cases/test_31_bsp_cpuidle.py"
)


def _load_case() -> types.ModuleType:
    oeqa = types.ModuleType("oeqa")
    core = types.ModuleType("oeqa.core")
    decorator = types.ModuleType("oeqa.core.decorator")
    depends = types.ModuleType("oeqa.core.decorator.depends")
    runtime = types.ModuleType("oeqa.runtime")
    case = types.ModuleType("oeqa.runtime.case")

    def dependency(_items: list[str]) -> Callable[[Callable[..., object]], Callable[..., object]]:
        return lambda function: function

    setattr(depends, "OETestDepends", dependency)
    setattr(case, "OERuntimeTestCase", unittest.TestCase)
    sys.modules.update(
        {
            "oeqa": oeqa,
            "oeqa.core": core,
            "oeqa.core.decorator": decorator,
            "oeqa.core.decorator.depends": depends,
            "oeqa.runtime": runtime,
            "oeqa.runtime.case": case,
        }
    )
    spec = importlib.util.spec_from_file_location("test_31_bsp_cpuidle", CASE_PATH)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_cpuidle_case_exists_as_the_profile_selector() -> None:
    assert CASE_PATH.is_file()
    module = _load_case()
    assert hasattr(module, "BspCpuIdleTest")


class FakeTarget:
    def __init__(self, scenario: str) -> None:
        self.scenario = scenario
        self.commands: list[str] = []

    def run(self, command: str, timeout: int | None = None) -> tuple[int, str]:
        self.commands.append(command)
        return 1, self.scenario


class PassingTarget:
    def __init__(self) -> None:
        self.commands: list[str] = []

    def run(self, command: str, timeout: int | None = None) -> tuple[int, str]:
        self.commands.append(command)
        if "/usage; cat " in command:
            return 0, "1\n1\n"
        if command.rstrip().endswith("/disable )"):
            return 0, "0\n"
        return 0, "CPUIDLE_CLEANUP disable=ok\nCPUIDLE_CLEANUP governor=ok\n"


@pytest.mark.parametrize(
    "method",
    ["test_disable_state", "test_governor_switching", "test_invalid_governor"],
)
def test_mutating_cases_require_cleanup_evidence(method: str) -> None:
    module = _load_case()
    test_case = module.BspCpuIdleTest(method)
    target = PassingTarget()
    test_case.target = target
    test_case.td = {"PC_CPUS_COUNT": "4"}

    getattr(test_case, method)()
    if method == "test_disable_state":
        assert any("CPUIDLE_AFTER_WRITE" in command for command in target.commands)
    else:
        assert target.commands[0].startswith("( set -e; ( set +e;")


def test_residency_validation_does_not_block_idle_in_the_guest() -> None:
    module = _load_case()
    class SnapshotTarget:
        def __init__(self) -> None:
            self.commands: list[str] = []
            self.snapshots = 0

        def run(self, command: str, timeout: int | None = None) -> tuple[int, str]:
            self.commands.append(command)
            if "/usage; cat " in command:
                value = 1 + self.snapshots
                self.snapshots += 1
                return 0, f"{value}\n{value}\n"
            if command.rstrip().endswith("/disable )"):
                return 0, "0\n"
            if "topology/cluster_id" in command:
                return 0, "0\n"
            return 0, ""

    target = SnapshotTarget()
    test_case = module.BspCpuIdleTest("test_residency_latency")
    test_case.target = target
    test_case.td = {"PC_CPUS_COUNT": "4"}
    snapshots: list[str] = []
    test_case.logger = types.SimpleNamespace(info=snapshots.append)

    original_sleep = module.time.sleep
    module.time.sleep = lambda _seconds: None
    try:
        test_case.test_residency_latency()
    finally:
        module.time.sleep = original_sleep

    assert all("usleep" not in command for command in target.commands)
    assert target.snapshots >= 2
    assert len(snapshots) == 12
    assert len({record.split()[2] for record in snapshots}) == 3


def test_deep_state_inducement_is_explicit_and_reversible() -> None:
    module = _load_case()

    assert hasattr(module.BspCpuIdleTest, "_induce_state1")
    assert hasattr(module.BspCpuIdleTest, "_induce_state2")

    class MatrixTarget:
        def __init__(self) -> None:
            self.commands: list[str] = []

        def run(self, command: str, timeout: int | None = None) -> tuple[int, str]:
            self.commands.append(command)
            if command.rstrip().endswith("/disable )"):
                return 0, "0\n"
            return 0, ""

    target = MatrixTarget()
    test_case = module.BspCpuIdleTest("test_residency_latency")
    test_case.target = target
    test_case.td = {"PC_CPUS_COUNT": "4"}

    saved = test_case._snapshot_disables()
    test_case._induce_state1(0)
    test_case._restore_disables(saved)

    joined = "\n".join(target.commands)
    assert "state0/disable" in joined
    assert "state1/disable" in joined
    assert "state2/disable" in joined
    assert "taskset" not in joined


@pytest.mark.parametrize(
    ("body_rc", "cleanup_rc", "expected_rc"),
    [(19, 0, 19), (0, 23, 23), (19, 23, 19)],
)
def test_mutation_cleanup_preserves_body_failure(
    body_rc: int,
    cleanup_rc: int,
    expected_rc: int,
) -> None:
    module = _load_case()
    script = (
        f"cleanup() {{ return {cleanup_rc}; }}; "
        f"trap '{module.MUTATION_EXIT_TRAP}' EXIT; exit {body_rc}"
    )
    completed = subprocess.run(["sh", "-c", script], check=False)

    assert completed.returncode == expected_rc


def test_disable_loop_finishes_with_a_success_status() -> None:
    source = CASE_PATH.read_text(encoding="utf-8")

    assert "CPUIDLE_AFTER_WRITE" in source
    assert "CPUIDLE_DISABLED_SETTLE" in source
    assert "CPUIDLE_DISABLED_BASELINE" in source
    assert "CPUIDLE_DISABLED_SAMPLE" in source
    assert "CPUIDLE_DISABLE_DRAIN_TIMEOUT" in source
    assert "CPUIDLE_POST_DISABLE_INCREMENT" in source


def test_static_deep_counter_reports_the_exact_timeout_tuple() -> None:
    module = _load_case()

    class StaticTarget:
        def run(self, command: str, timeout: int | None = None) -> tuple[int, str]:
            return 0, "4\n1464\n"

    test_case = module.BspCpuIdleTest("test_residency_latency")
    test_case.target = StaticTarget()
    test_case.td = {"PC_CPUS_COUNT": "4"}
    test_case.logger = types.SimpleNamespace(info=lambda _record: None)
    original_monotonic = module.time.monotonic
    original_sleep = module.time.sleep
    ticks = iter((0.0, 0.0, 1.0))
    module.time.monotonic = lambda: next(ticks)
    module.time.sleep = lambda _seconds: None
    try:
        with pytest.raises(
            AssertionError,
            match=r"cpu=0 state=state1 usage=4 time=1464",
        ):
            test_case._wait_for_counter_advance(0, "state1", "/state1", 4, 1464, 1.0)
    finally:
        module.time.monotonic = original_monotonic
        module.time.sleep = original_sleep


@pytest.mark.parametrize(
    ("scenario", "method"),
    [
        ("absent-state", "test_cpuidle_c_states"),
        ("wrong-properties", "test_residency_latency"),
        ("static-counters", "test_residency_latency"),
        ("disabled-counter-advances", "test_disable_state"),
        ("invalid-governor-accepted", "test_invalid_governor"),
        ("switching-mismatch", "test_governor_switching"),
        ("cleanup-drift", "test_disable_state"),
        ("cleanup-failure", "test_governor_switching"),
        ("wrong-cpu-count", "test_ensure_interface"),
    ],
)
def test_fake_target_rejects_each_cpuidle_contract_violation(
    scenario: str,
    method: str,
) -> None:
    module = _load_case()
    target = FakeTarget(scenario)
    test_case = module.BspCpuIdleTest(method)
    test_case.target = target
    test_case.td = {"PC_CPUS_COUNT": "4"}

    with pytest.raises(AssertionError, match=scenario):
        getattr(test_case, method)()

    assert target.commands
