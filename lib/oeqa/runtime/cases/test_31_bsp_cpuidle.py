# SPDX-License-Identifier: MIT

from collections.abc import Mapping
import time
from typing import Final, Protocol

from oeqa.core.decorator.depends import OETestDepends
from oeqa.runtime.case import OERuntimeTestCase


CPU_SYSFS: Final = "/sys/devices/system/cpu"
CPUIDLE_SYSFS: Final = f"{CPU_SYSFS}/cpuidle"
DISABLE_POLL_ATTEMPTS: Final = 2
ADVANCEMENT_POLL_ATTEMPTS: Final = 3
POLL_INTERVAL_SECONDS: Final = 1.0
STATE0_ADVANCEMENT_TIMEOUT_SECONDS: Final = 30.0
STATE1_ADVANCEMENT_TIMEOUT_SECONDS: Final = 30.0
STATE2_ADVANCEMENT_TIMEOUT_SECONDS: Final = 90.0
DISABLE_DRAIN_TIMEOUT_SECONDS: Final = 5.0
DISABLE_POLL_INTERVAL_SECONDS: Final = 0.25
MUTATION_EXIT_TRAP: Final = (
    "body_rc=$?; cleanup; cleanup_rc=$?; trap - EXIT; "
    "test \"$body_rc\" -eq 0 || exit \"$body_rc\"; exit \"$cleanup_rc\""
)
STATES: Final = (
    ("state0", "WFI", "1", "1"),
    ("state1", "cpu-sleep", "4200", "4000"),
    ("state2", "cluster-sleep", "4500", "4200"),
)
CPUIDLE_ADVERSARIAL_SCENARIOS: Final = (
    "absent-state",
    "wrong-properties",
    "static-counters",
    "disabled-counter-advances",
    "invalid-governor-accepted",
    "switching-mismatch",
    "cleanup-drift",
    "cleanup-failure",
    "wrong-cpu-count",
)


class BspTarget(Protocol):
    def run(self, command: str, timeout: int | None = None) -> tuple[int, str]: ...


class BspCpuIdleTest(OERuntimeTestCase):
    target: BspTarget
    td: Mapping[str, str]
    timeout = 300

    def _cpu_count(self) -> int:
        raw_count = self.td.get("PC_CPUS_COUNT", "4")
        try:
            cpu_count = int(raw_count)
        except ValueError as error:
            raise AssertionError("invalid PC_CPUS_COUNT") from error
        if cpu_count < 1:
            raise AssertionError("invalid PC_CPUS_COUNT")
        return cpu_count

    def _run_ok(self, command: str, timeout: int | None = None) -> str:
        command = f"( set -e; {command} )"
        status, output = self.target.run(command, timeout=timeout or self.timeout)
        self.assertEqual(status, 0, msg=f"command failed: {command}\n{output}")
        return output

    def _for_each_state(self, body: str) -> str:
        return (
            "for cpu in $(seq 0 "
            f"{self._cpu_count() - 1}"
            "); do for entry in "
            "'state0:WFI:1:1' 'state1:cpu-sleep:4200:4000' "
            "'state2:cluster-sleep:4500:4200'; do "
            "IFS=: read -r state name residency latency <<EOF\n$entry\nEOF\n"
            f"base={CPU_SYSFS}/cpu$cpu/cpuidle/$state; {body}; "
            "done; done"
        )

    @OETestDepends(["test_00_bsp_boot.BspBootTest.test_bsp_boot"])
    def test_ensure_interface(self) -> None:
        cpu_count = self._cpu_count()
        self._run_ok(
            f"test -d {CPUIDLE_SYSFS}; test -r {CPUIDLE_SYSFS}/available_governors; "
            f"test -r {CPUIDLE_SYSFS}/current_governor_ro; "
            f"test -r {CPUIDLE_SYSFS}/current_governor; "
            f"test \"$(nproc --all)\" = \"{cpu_count}\"; "
            f"test \"$(cat /etc/nexios-bsp-cpus)\" = \"{cpu_count}\"; "
            + self._for_each_state(
                "test -d \"$base\"; test -r \"$base/name\"; "
                "test -r \"$base/residency\"; test -r \"$base/latency\"; "
                "test -r \"$base/usage\"; test -r \"$base/time\"; "
                "test -r \"$base/disable\""
            )
        )

    @OETestDepends(["test_31_bsp_cpuidle.BspCpuIdleTest.test_ensure_interface"])
    def test_cpuidle_c_states(self) -> None:
        self._run_ok(
            self._for_each_state(
                "test \"$(cat \"$base/name\")\" = \"$name\""
            )
        )

    @OETestDepends(["test_31_bsp_cpuidle.BspCpuIdleTest.test_cpuidle_c_states"])
    def test_default_status(self) -> None:
        self._run_ok(
            self._for_each_state(
                "if test -e \"$base/default_status\"; then "
                "test \"$(cat \"$base/default_status\")\" = enabled; fi"
            )
        )

    @OETestDepends(["test_31_bsp_cpuidle.BspCpuIdleTest.test_default_status"])
    def test_disable_state(self) -> None:
        for cpu in range(self._cpu_count()):
            for state, _name, _residency, _latency in STATES:
                self._verify_disabled_state(cpu, state)

    def _verify_disabled_state(self, cpu: int, state: str) -> None:
        base = self._state_base(cpu, state)
        original = self._run_ok(f"cat {base}/disable").strip()
        body_failed = False
        try:
            self._set_disable(cpu, state, "1")
            self._run_ok(
                f"printf 'CPUIDLE_AFTER_WRITE cpu={cpu} state={state} disable=%s\\n' "
                f"\"$(cat {base}/disable)\""
            )
            stable = self._drain_disabled_state(cpu, state)
            self._observe_disabled_state(cpu, state, stable)
        except AssertionError:
            body_failed = True
            raise
        finally:
            try:
                self._set_disable(cpu, state, original)
                print(f"CPUIDLE_CLEANUP cpu={cpu} state={state} disable={original}")
            except AssertionError:
                if not body_failed:
                    raise

    def _drain_disabled_state(self, cpu: int, state: str) -> tuple[int, int]:
        base = self._state_base(cpu, state)
        previous = self._state_counters(base)
        deadline = time.monotonic() + DISABLE_DRAIN_TIMEOUT_SECONDS
        while True:
            if time.monotonic() >= deadline:
                raise AssertionError(
                    f"CPUIDLE_DISABLE_DRAIN_TIMEOUT cpu={cpu} state={state} "
                    f"usage={previous[0]} time={previous[1]}"
                )
            time.sleep(DISABLE_POLL_INTERVAL_SECONDS)
            current = self._state_counters(base)
            self._run_ok(
                f"printf 'CPUIDLE_DISABLED_SETTLE cpu={cpu} state={state} "
                f"usage_before={previous[0]} usage_after={current[0]} "
                f"time_before={previous[1]} time_after={current[1]}\\n'"
            )
            if current == previous:
                self._run_ok(
                    f"printf 'CPUIDLE_DISABLED_BASELINE cpu={cpu} state={state} "
                    f"usage={current[0]} time={current[1]}\\n'"
                )
                return current
            previous = current

    def _observe_disabled_state(
        self,
        cpu: int,
        state: str,
        baseline: tuple[int, int],
    ) -> None:
        base = self._state_base(cpu, state)
        for sample in range(2):
            time.sleep(DISABLE_POLL_INTERVAL_SECONDS)
            current = self._state_counters(base)
            self._run_ok(
                f"printf 'CPUIDLE_DISABLED_SAMPLE cpu={cpu} state={state} sample={sample} "
                f"usage_before={baseline[0]} usage_after={current[0]} "
                f"time_before={baseline[1]} time_after={current[1]}\\n'"
            )
            if current != baseline:
                raise AssertionError(
                    f"CPUIDLE_POST_DISABLE_INCREMENT cpu={cpu} state={state} "
                    f"usage_before={baseline[0]} usage_after={current[0]} "
                    f"time_before={baseline[1]} time_after={current[1]}"
                )

    @OETestDepends(["test_31_bsp_cpuidle.BspCpuIdleTest.test_disable_state"])
    def test_residency_latency(self) -> None:
        for cpu in range(self._cpu_count()):
            self._validate_state(cpu, "state0", "1", "1")
            self._require_advance(cpu, "state0", STATE0_ADVANCEMENT_TIMEOUT_SECONDS)
            saved = self._snapshot_disables()
            try:
                self._induce_state1(cpu)
                self._validate_state(cpu, "state1", "4200", "4000")
                self._require_advance(cpu, "state1", STATE1_ADVANCEMENT_TIMEOUT_SECONDS)
            finally:
                self._restore_disables(saved)
            saved = self._snapshot_disables()
            try:
                siblings = self._cluster_siblings(cpu)
                self._induce_state2(siblings)
                self._validate_state(cpu, "state2", "4500", "4200")
                self._require_advance(cpu, "state2", STATE2_ADVANCEMENT_TIMEOUT_SECONDS)
            finally:
                self._restore_disables(saved)

    def _state_base(self, cpu: int, state: str) -> str:
        return f"{CPU_SYSFS}/cpu{cpu}/cpuidle/{state}"

    def _validate_state(
        self,
        cpu: int,
        state: str,
        residency: str,
        latency: str,
    ) -> None:
        base = self._state_base(cpu, state)
        self._run_ok(
            f"test \"$(cat {base}/residency)\" = \"{residency}\"; "
            f"test \"$(cat {base}/latency)\" = \"{latency}\"; "
            f"test \"$(cat {base}/disable)\" = 0"
        )

    def _require_advance(self, cpu: int, state: str, timeout_seconds: float) -> None:
        base = self._state_base(cpu, state)
        before_usage, before_time = self._state_counters(base)
        self._wait_for_counter_advance(
            cpu,
            state,
            base,
            before_usage,
            before_time,
            timeout_seconds,
        )

    def _snapshot_disables(self) -> tuple[tuple[int, str, str], ...]:
        snapshots: list[tuple[int, str, str]] = []
        for cpu in range(self._cpu_count()):
            for state, _name, _residency, _latency in STATES:
                value = self._run_ok(f"cat {self._state_base(cpu, state)}/disable").strip()
                if value not in {"0", "1"}:
                    raise AssertionError(f"invalid disable value cpu={cpu} state={state}: {value!r}")
                snapshots.append((cpu, state, value))
        return tuple(snapshots)

    def _set_disable(self, cpu: int, state: str, value: str) -> None:
        base = self._state_base(cpu, state)
        self._run_ok(
            f"printf '%s\\n' {value} > {base}/disable; "
            f"test \"$(cat {base}/disable)\" = {value}"
        )

    def _restore_disables(self, snapshots: tuple[tuple[int, str, str], ...]) -> None:
        for cpu, state, value in reversed(snapshots):
            self._set_disable(cpu, state, value)
        print("CPUIDLE_CLEANUP disable-matrix=ok")

    def _induce_state1(self, cpu: int) -> None:
        self._set_disable(cpu, "state0", "1")
        self._set_disable(cpu, "state1", "0")
        self._set_disable(cpu, "state2", "1")

    def _cluster_siblings(self, cpu: int) -> tuple[int, ...]:
        cluster = self._run_ok(f"cat {CPU_SYSFS}/cpu{cpu}/topology/cluster_id").strip()
        siblings: list[int] = []
        for candidate in range(self._cpu_count()):
            current = self._run_ok(
                f"cat {CPU_SYSFS}/cpu{candidate}/topology/cluster_id"
            ).strip()
            if current == cluster:
                siblings.append(candidate)
        if not siblings:
            raise AssertionError(f"no cluster siblings for cpu={cpu}")
        return tuple(siblings)

    def _induce_state2(self, siblings: tuple[int, ...]) -> None:
        for cpu in siblings:
            self._set_disable(cpu, "state0", "1")
            self._set_disable(cpu, "state1", "1")
            self._set_disable(cpu, "state2", "0")

    def _state_counters(self, base: str) -> tuple[int, int]:
        output = self._run_ok(f"cat {base}/usage; cat {base}/time")
        lines = tuple(line.strip() for line in output.splitlines() if line.strip())
        if len(lines) != 2:
            raise AssertionError(f"invalid cpuidle counters at {base}: {output!r}")
        try:
            return int(lines[0]), int(lines[1])
        except ValueError as error:
            raise AssertionError(f"invalid cpuidle counters at {base}: {output!r}") from error

    def _wait_for_counter_advance(
        self,
        cpu: int,
        state: str,
        base: str,
        before_usage: int,
        before_time: int,
        timeout_seconds: float,
    ) -> None:
        deadline = time.monotonic() + timeout_seconds
        while True:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                raise AssertionError(
                    f"cpuidle counters did not advance cpu={cpu} state={state} "
                    f"usage={before_usage} time={before_time}"
                )
            time.sleep(min(POLL_INTERVAL_SECONDS, remaining))
            after_usage, after_time = self._state_counters(base)
            snapshot = (
                f"CPUIDLE_SNAPSHOT cpu={cpu} state={state} "
                f"usage_before={before_usage} usage_after={after_usage} "
                f"time_before={before_time} time_after={after_time}"
            )
            print(snapshot)
            self.logger.info(snapshot)
            if after_usage > before_usage and after_time > before_time:
                return

    @OETestDepends(["test_31_bsp_cpuidle.BspCpuIdleTest.test_residency_latency"])
    def test_governors(self) -> None:
        self._run_ok(
            f"available=$(cat {CPUIDLE_SYSFS}/available_governors); "
            f"current_ro=$(cat {CPUIDLE_SYSFS}/current_governor_ro); "
            f"current=$(cat {CPUIDLE_SYSFS}/current_governor); "
            "case \" $available \" in *\" $current_ro \"*) ;; *) exit 1;; esac; "
            "test \"$current\" = \"$current_ro\""
        )

    @OETestDepends(["test_31_bsp_cpuidle.BspCpuIdleTest.test_governors"])
    def test_governor_switching(self) -> None:
        output = self._run_ok(
            "( set +e; path=" + CPUIDLE_SYSFS + "/current_governor; saved=$(cat \"$path\"); "
            "printf 'CPUIDLE_BEFORE governor=%s\\n' \"$saved\"; "
            "cleanup() { printf '%s\\n' \"$saved\" > \"$path\" || return 1; "
            "test \"$(cat \"$path\")\" = \"$saved\" && "
            "test \"$(cat " + CPUIDLE_SYSFS + "/current_governor_ro)\" = \"$saved\" && printf 'CPUIDLE_CLEANUP governor=ok\\n'; }; "
            "trap '" + MUTATION_EXIT_TRAP + "' EXIT; "
            "for governor in $(cat " + CPUIDLE_SYSFS + "/available_governors); do "
            "printf '%s\\n' \"$governor\" > \"$path\" || exit 1; "
            "actual=$(cat \"$path\"); actual_ro=$(cat " + CPUIDLE_SYSFS + "/current_governor_ro); "
            "printf 'CPUIDLE_GOVERNOR_AFTER_WRITE requested=%s current=%s current_ro=%s\\n' \"$governor\" \"$actual\" \"$actual_ro\"; "
            "test \"$actual\" = \"$governor\" || exit 1; "
            "test \"$actual_ro\" = \"$governor\" || exit 1; done )"
        )
        self.assertIn("CPUIDLE_CLEANUP governor=ok", output)

    @OETestDepends(["test_31_bsp_cpuidle.BspCpuIdleTest.test_governor_switching"])
    def test_invalid_governor(self) -> None:
        output = self._run_ok(
            "( set +e; path=" + CPUIDLE_SYSFS + "/current_governor; saved=$(cat \"$path\"); "
            "printf 'CPUIDLE_BEFORE governor=%s\\n' \"$saved\"; "
            "cleanup() { printf '%s\\n' \"$saved\" > \"$path\" || return 1; "
            "test \"$(cat \"$path\")\" = \"$saved\" && "
            "test \"$(cat " + CPUIDLE_SYSFS + "/current_governor_ro)\" = \"$saved\" && printf 'CPUIDLE_CLEANUP governor=ok\\n'; }; "
            "trap '" + MUTATION_EXIT_TRAP + "' EXIT; "
            "printf '%s\\n' invalid-governor-name > \"$path\" && exit 1; "
            "actual=$(cat \"$path\"); actual_ro=$(cat " + CPUIDLE_SYSFS + "/current_governor_ro); "
            "printf 'CPUIDLE_GOVERNOR_AFTER_INVALID current=%s current_ro=%s\\n' \"$actual\" \"$actual_ro\"; "
            "test \"$actual\" = \"$saved\" || exit 1; "
            "test \"$actual_ro\" = \"$saved\" || exit 1 )"
        )
        self.assertIn("CPUIDLE_CLEANUP governor=ok", output)
