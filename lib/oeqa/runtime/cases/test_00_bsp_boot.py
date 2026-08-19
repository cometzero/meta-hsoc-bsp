# SPDX-License-Identifier: MIT

from oeqa.runtime.case import OERuntimeTestCase
from typing import Protocol


class BspTarget(Protocol):
    def run(
        self,
        command: str,
        timeout: int | None = None,
    ) -> tuple[int, str]: ...


class BspBootTest(OERuntimeTestCase):
    target: BspTarget

    def test_bsp_boot(self):
        status, output = self.target.run(
            "test -r /etc/nexios-bsp-machine && "
            "test -r /etc/nexios-bsp-cpus && "
            "printf 'BSP machine=%s cpus=%s\\n' "
            "\"$(cat /etc/nexios-bsp-machine)\" "
            "\"$(cat /etc/nexios-bsp-cpus)\""
        )

        self.assertEqual(status, 0, msg=output)
        self.assertRegex(output, r"BSP machine=apollo-(?:fvp|qvp) cpus=4")
