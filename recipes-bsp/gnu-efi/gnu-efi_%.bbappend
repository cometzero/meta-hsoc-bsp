# SPDX-License-Identifier: MIT
# EFI firmware does not establish the Linux userspace SVE context. The
# Cortex-A720 tune otherwise vectorizes RtZeroMem with SVE. Use the baseline
# AArch64 ISA; -mgeneral-regs-only cannot compile GNU-EFI's FloatToString API.
CC:append:apollo-qvp:class-target = " -march=armv8-a"
PACKAGE_ARCH:apollo-qvp:class-target = "${MACHINE_ARCH}"

# This upstream Makefile has no compiler-command dependency tracking and builds
# in-tree. Discard old objects when the configuration/compiler flags change.
do_configure:append:apollo-qvp:class-target() {
    oe_runmake clean
}
