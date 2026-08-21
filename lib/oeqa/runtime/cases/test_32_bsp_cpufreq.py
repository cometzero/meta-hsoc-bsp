# SPDX-License-Identifier: MIT

from __future__ import annotations

from collections.abc import Mapping
from typing import Final, Protocol

from oeqa.core.decorator.depends import OETestDepends
from oeqa.runtime.case import OERuntimeTestCase


CPUFREQ_SYSFS: Final = "/sys/devices/system/cpu/cpufreq"
GOVERNORS: Final = "ondemand performance powersave schedutil"
FREQUENCIES: Final = "1800000 2000000 2500000"
MUTATION_EXIT_TRAP: Final = (
    "body_rc=$?; cleanup; cleanup_rc=$?; trap - EXIT; "
    "test \"$body_rc\" -eq 0 || exit \"$body_rc\"; exit \"$cleanup_rc\""
)
CPUFREQ_ADVERSARIAL_SCENARIOS: Final = (
    "missing-policy",
    "missing-opp",
    "missing-governor",
    "mutation-failure",
    "invalid-governor-accepted",
    "invalid-min-max-accepted",
    "cleanup-drift",
    "stale-evidence",
    "malformed-evidence",
)


class BspTarget(Protocol):
    def run(self, command: str, timeout: int | None = None) -> tuple[int, str]: ...


class BspCpuFreqTest(OERuntimeTestCase):
    target: BspTarget
    td: Mapping[str, str]
    timeout = 300

    def _cpu_count(self) -> int:
        return self._positive_testdata("PC_CPUS_COUNT")

    def _cluster_size(self) -> int:
        cluster_size = self._positive_testdata("PC_CPUS_PER_CLUSTER_MAX")
        if self._cpu_count() % cluster_size:
            raise AssertionError("PC_CPUS_COUNT is not cluster aligned")
        return cluster_size

    def _positive_testdata(self, name: str) -> int:
        try:
            value = int(self.td[name])
        except (KeyError, ValueError) as error:
            raise AssertionError(f"invalid {name}") from error
        if value < 1:
            raise AssertionError(f"invalid {name}")
        return value

    def _policy_names(self) -> tuple[str, ...]:
        cluster_size = self._cluster_size()
        return tuple(
            f"policy{first_cpu}"
            for first_cpu in range(0, self._cpu_count(), cluster_size)
        )

    def _run_ok(self, command: str, timeout: int | None = None) -> str:
        wrapped = f"( set -e; {command} )"
        status, output = self.target.run(wrapped, timeout=timeout or self.timeout)
        self.assertEqual(status, 0, msg=f"command failed: {wrapped}\n{output}")
        return output

    def _policy_loop(self, body: str) -> str:
        policies = " ".join(self._policy_names())
        return (
            f"for policy in {policies}; do path={CPUFREQ_SYSFS}/$policy; "
            f"{body}; done"
        )

    def _mutate(self, body: str) -> str:
        policies = " ".join(self._policy_names())
        return self._run_ok(
            "( set +e; snapshots=$(for policy in "
            f"{policies}; do path={CPUFREQ_SYSFS}/$policy; "
            "printf '%s|%s|%s|%s\\n' \"$policy\" \"$(cat \"$path/scaling_governor\")\" "
            "\"$(cat \"$path/scaling_min_freq\")\" \"$(cat \"$path/scaling_max_freq\")\" "
            "|| exit 1; done) || exit 1; "
            "cleanup() { printf '%s\\n' \"$snapshots\" | "
            "while IFS='|' read -r policy governor minimum maximum; do "
            f"path={CPUFREQ_SYSFS}/$policy; "
            "printf '%s\\n' \"$maximum\" > \"$path/scaling_max_freq\" || return 1; "
            "printf '%s\\n' \"$minimum\" > \"$path/scaling_min_freq\" || return 1; "
            "printf '%s\\n' \"$governor\" > \"$path/scaling_governor\" || return 1; "
            "test \"$(cat \"$path/scaling_governor\")\" = \"$governor\" || return 1; "
            "test \"$(cat \"$path/scaling_min_freq\")\" = \"$minimum\" || return 1; "
            "test \"$(cat \"$path/scaling_max_freq\")\" = \"$maximum\" || return 1; "
            "printf 'CPUFREQ_CLEANUP policy=%s governor=%s min=%s max=%s\\n' "
            "\"$policy\" \"$governor\" \"$minimum\" \"$maximum\"; done; }; "
            f"trap '{MUTATION_EXIT_TRAP}' EXIT; {body} )"
        )

    @OETestDepends(["test_00_bsp_boot.BspBootTest.test_bsp_boot"])
    def test_cpufreq_policy(self) -> None:
        policies = " ".join(self._policy_names())
        self._run_ok(
            f"test -d {CPUFREQ_SYSFS}; "
            f"test \"$(nproc --all)\" = \"{self._cpu_count()}\"; "
            f"test \"$(cat /etc/nexios-bsp-cpus)\" = \"{self._cpu_count()}\"; "
            "expected_policies=$(printf '%s\\n' " + policies + " | sort -V -u | xargs); "
            "actual=$(for path in " + CPUFREQ_SYSFS + "/policy*; do "
            "test -d \"$path\" && basename \"$path\"; done | sort -V -u | xargs); "
            "printf 'CPUFREQ_POLICIES expected=%s actual=%s\\n' \"$expected_policies\" \"$actual\"; "
            "test \"$actual\" = \"$expected_policies\"; "
            + self._policy_loop(
                "test -r \"$path/scaling_available_governors\"; "
                "test -r \"$path/scaling_available_frequencies\"; "
                "test -r \"$path/scaling_driver\"; "
                "test -r \"$path/affected_cpus\"; "
                "test -r \"$path/scaling_min_freq\"; "
                "test -r \"$path/scaling_max_freq\"; "
                "test -r \"$path/scaling_cur_freq\"; "
                f"expected_governors=$(printf '%s\\n' {GOVERNORS} | sort -u | xargs); "
                "governors=$(tr ' ' '\\n' < \"$path/scaling_available_governors\" | sort -u | xargs); "
                f"expected_frequencies=$(printf '%s\\n' {FREQUENCIES} | sort -n -u | xargs); "
                "frequencies=$(tr ' ' '\\n' < \"$path/scaling_available_frequencies\" | sort -n -u | xargs); "
                "driver=$(cat \"$path/scaling_driver\"); "
                "affected=$(cat \"$path/affected_cpus\" | xargs); "
                "minimum=$(cat \"$path/scaling_min_freq\"); "
                "maximum=$(cat \"$path/scaling_max_freq\"); "
                "current=$(cat \"$path/scaling_cur_freq\"); "
                "printf 'CPUFREQ_POLICY policy=%s governors=%s frequencies=%s driver=%s affected=%s min=%s max=%s current=%s\\n' "
                "\"$policy\" \"$governors\" \"$frequencies\" \"$driver\" \"$affected\" \"$minimum\" \"$maximum\" \"$current\"; "
                "test \"$governors\" = \"$expected_governors\"; "
                "test \"$frequencies\" = \"$expected_frequencies\""
            )
        )

    @OETestDepends(["test_32_bsp_cpufreq.BspCpuFreqTest.test_cpufreq_policy"])
    def test_cpufreq_default_governors(self) -> None:
        self._run_ok(self._policy_loop("test \"$(cat \"$path/scaling_governor\")\" = schedutil"))

    @OETestDepends(["test_32_bsp_cpufreq.BspCpuFreqTest.test_cpufreq_policy"])
    def test_cpufreq_set_governors(self) -> None:
        output = self._mutate(
            self._policy_loop(
                f"for governor in {GOVERNORS}; do "
                "printf '%s\\n' \"$governor\" > \"$path/scaling_governor\" || exit 1; "
                "test \"$(cat \"$path/scaling_governor\")\" = \"$governor\" || exit 1; done"
            )
        )
        self.assertEqual(output.count("CPUFREQ_CLEANUP policy="), len(self._policy_names()))

    @OETestDepends(["test_32_bsp_cpufreq.BspCpuFreqTest.test_cpufreq_policy"])
    def test_cpufreq_scaling_driver(self) -> None:
        self._run_ok(self._policy_loop("test \"$(cat \"$path/scaling_driver\")\" = scmi"))

    @OETestDepends(["test_32_bsp_cpufreq.BspCpuFreqTest.test_cpufreq_policy"])
    def test_current_frequency_per_governor(self) -> None:
        output = self._mutate(
            self._policy_loop(
                f"for governor in {GOVERNORS}; do "
                "printf '%s\\n' \"$governor\" > \"$path/scaling_governor\" || exit 1; "
                "frequency=$(cat \"$path/scaling_cur_freq\") || exit 1; "
                f"case \" {FREQUENCIES} \" in *\" $frequency \"*) ;; *) exit 1;; esac; done"
            )
        )
        self.assertEqual(output.count("CPUFREQ_CLEANUP policy="), len(self._policy_names()))

    @OETestDepends(["test_32_bsp_cpufreq.BspCpuFreqTest.test_cpufreq_policy"])
    def test_cpufreq_affected_cpus_per_policy(self) -> None:
        cluster_size = self._cluster_size()
        for first_cpu, policy in zip(range(0, self._cpu_count(), cluster_size), self._policy_names()):
            expected = " ".join(str(cpu) for cpu in range(first_cpu, first_cpu + cluster_size))
            self._run_ok(
                f"test \"$(cat {CPUFREQ_SYSFS}/{policy}/affected_cpus | xargs)\" = \"{expected}\""
            )

    @OETestDepends(["test_32_bsp_cpufreq.BspCpuFreqTest.test_cpufreq_policy"])
    def test_update_invalid_governor(self) -> None:
        output = self._mutate(
            self._policy_loop(
                "saved=$(cat \"$path/scaling_governor\") || exit 1; "
                "if printf '%s\\n' invalid-governor-name > \"$path/scaling_governor\"; then exit 1; fi; "
                "test \"$(cat \"$path/scaling_governor\")\" = \"$saved\" || exit 1"
            )
        )
        self.assertEqual(output.count("CPUFREQ_CLEANUP policy="), len(self._policy_names()))

    @OETestDepends(["test_32_bsp_cpufreq.BspCpuFreqTest.test_cpufreq_policy"])
    def test_update_scaling_min_frequencies(self) -> None:
        output = self._mutate(
            self._policy_loop(
                f"for frequency in {FREQUENCIES}; do maximum=$(cat \"$path/scaling_max_freq\") || exit 1; "
                "if test \"$frequency\" -le \"$maximum\"; then "
                "printf '%s\\n' \"$frequency\" > \"$path/scaling_min_freq\" || exit 1; "
                "test \"$(cat \"$path/scaling_min_freq\")\" = \"$frequency\" || exit 1; fi; done"
            )
        )
        self.assertEqual(output.count("CPUFREQ_CLEANUP policy="), len(self._policy_names()))

    @OETestDepends(["test_32_bsp_cpufreq.BspCpuFreqTest.test_cpufreq_policy"])
    def test_update_scaling_max_frequencies(self) -> None:
        output = self._mutate(
            self._policy_loop(
                f"for frequency in {FREQUENCIES}; do minimum=$(cat \"$path/scaling_min_freq\") || exit 1; "
                "if test \"$frequency\" -ge \"$minimum\"; then "
                "printf '%s\\n' \"$frequency\" > \"$path/scaling_max_freq\" || exit 1; "
                "test \"$(cat \"$path/scaling_max_freq\")\" = \"$frequency\" || exit 1; fi; done"
            )
        )
        self.assertEqual(output.count("CPUFREQ_CLEANUP policy="), len(self._policy_names()))

    @OETestDepends(["test_32_bsp_cpufreq.BspCpuFreqTest.test_cpufreq_policy"])
    def test_update_min_max_scaling_frequencies_negative(self) -> None:
        output = self._mutate(
            self._policy_loop(
                "minimum=$(cat \"$path/scaling_min_freq\") || exit 1; "
                "maximum=$(cat \"$path/scaling_max_freq\") || exit 1; "
                "invalid_min=$((maximum + 100000)); "
                "if printf '%s\\n' \"$invalid_min\" > \"$path/scaling_min_freq\"; then "
                "test \"$(cat \"$path/scaling_min_freq\")\" -le \"$(cat \"$path/scaling_max_freq\")\" || exit 1; fi; "
                "invalid_max=$((minimum - 100000)); "
                "if printf '%s\\n' \"$invalid_max\" > \"$path/scaling_max_freq\"; then "
                "test \"$(cat \"$path/scaling_max_freq\")\" -ge \"$(cat \"$path/scaling_min_freq\")\" || exit 1; fi"
            )
        )
        self.assertEqual(output.count("CPUFREQ_CLEANUP policy="), len(self._policy_names()))
