# SPDX-License-Identifier: MIT

SUMMARY = "Opt-in GIC-720AE Linux interrupt selftest"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://gic720ae_test.c;beginline=1;endline=1;md5=fcab174c20ea2e2bc0be64b493708266"

FILESEXTRAPATHS:prepend := "${HSOC_APOLLO_LINUX_SRC}/tools/testing/selftests/irq/gic720ae:"

SRC_URI = " \
    file://Makefile \
    file://gic720ae_test.c \
    file://run.sh \
"

S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"

inherit module

require recipes-kernel/module-signing/apollo-external-module-signing.inc

EXTRA_OEMAKE += "INSTALL_MOD_DIR=extra"
INHIBIT_PACKAGE_STRIP:apollo-qvp = "${@'1' if d.getVar('MODSIGN_ENABLED') == '1' else '0'}"
EXCLUDE_FROM_WORLD = "1"

do_install:append() {
    install -d ${D}${libexecdir}/gic720ae-selftest
    install -m 0755 ${S}/run.sh ${D}${libexecdir}/gic720ae-selftest/run.sh
}

FILES:${PN} += "${libexecdir}/gic720ae-selftest/run.sh"
RDEPENDS:${PN} += "kernel-module-gic720ae-test"
