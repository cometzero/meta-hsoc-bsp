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
    / "lib/oeqa/runtime/cases/test_32_bsp_cpufreq.py"
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
    spec = importlib.util.spec_from_file_location("test_32_bsp_cpufreq", CASE_PATH)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_cpufreq_case_exists_as_the_profile_selector() -> None:
    assert CASE_PATH.is_file()
    module = _load_case()
    assert hasattr(module, "BspCpuFreqTest")


def test_policy_contract_normalizes_unordered_sysfs_tokens() -> None:
    module = _load_case()
    test_case = module.BspCpuFreqTest("test_cpufreq_policy")
    test_case.td = {
        "PC_CPUS_COUNT": "4",
        "PC_CPUS_PER_CLUSTER_MAX": "4",
    }

    contract = test_case._policy_loop("body")
    source = CASE_PATH.read_text(encoding="utf-8")

    assert "policy0" in contract
    assert "sort -u" in source
    assert "sort -n -u" in source
    assert "CPUFREQ_POLICY" in source


@pytest.mark.parametrize(
    "method",
    [
        "test_cpufreq_set_governors",
        "test_current_frequency_per_governor",
        "test_update_invalid_governor",
        "test_update_scaling_min_frequencies",
        "test_update_scaling_max_frequencies",
        "test_update_min_max_scaling_frequencies_negative",
    ],
)
def test_mutating_cpufreq_cases_require_cleanup_evidence(method: str) -> None:
    module = _load_case()
    source = CASE_PATH.read_text(encoding="utf-8")

    assert "CPUFREQ_CLEANUP" in source
    assert "MUTATION_EXIT_TRAP" in source
    assert method in source
    assert hasattr(module.BspCpuFreqTest, method)


@pytest.mark.parametrize(
    ("body_rc", "cleanup_rc", "expected_rc"),
    [(19, 0, 19), (0, 23, 23), (19, 23, 19)],
)
def test_cpufreq_cleanup_preserves_body_failure(
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


@pytest.mark.parametrize(
    "scenario",
    [
        "missing-policy",
        "missing-opp",
        "missing-governor",
        "mutation-failure",
        "invalid-governor-accepted",
        "invalid-min-max-accepted",
        "cleanup-drift",
        "stale-evidence",
        "malformed-evidence",
    ],
)
def test_cpufreq_case_declares_all_adversarial_contracts(scenario: str) -> None:
    module = _load_case()

    assert scenario in module.CPUFREQ_ADVERSARIAL_SCENARIOS


class FailingTarget:
    def __init__(self, scenario: str) -> None:
        self.scenario = scenario
        self.commands: list[str] = []

    def run(self, command: str, timeout: int | None = None) -> tuple[int, str]:
        self.commands.append(command)
        return 1, self.scenario


@pytest.mark.parametrize(
    ("scenario", "method"),
    [
        ("missing-policy", "test_cpufreq_policy"),
        ("missing-opp", "test_cpufreq_policy"),
        ("missing-governor", "test_cpufreq_policy"),
        ("mutation-failure", "test_cpufreq_set_governors"),
        ("invalid-governor-accepted", "test_update_invalid_governor"),
        ("invalid-min-max-accepted", "test_update_min_max_scaling_frequencies_negative"),
        ("cleanup-drift", "test_update_scaling_max_frequencies"),
        ("stale-evidence", "test_current_frequency_per_governor"),
        ("malformed-evidence", "test_cpufreq_affected_cpus_per_policy"),
    ],
)
def test_cpufreq_case_rejects_each_adversarial_target_result(
    scenario: str,
    method: str,
) -> None:
    module = _load_case()
    target = FailingTarget(scenario)
    test_case = module.BspCpuFreqTest(method)
    test_case.target = target
    test_case.td = {
        "PC_CPUS_COUNT": "4",
        "PC_CPUS_PER_CLUSTER_MAX": "4",
    }

    with pytest.raises(AssertionError, match=scenario):
        getattr(test_case, method)()

    assert target.commands
