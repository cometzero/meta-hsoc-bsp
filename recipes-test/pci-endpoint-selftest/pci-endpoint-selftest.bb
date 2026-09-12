# SPDX-License-Identifier: MIT

SUMMARY = "Linux PCI endpoint kselftest"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://pci_endpoint_test.c;beginline=1;endline=1;md5=50d2ba0afecd20f74c12a4bdbcfcfe61"

FILESEXTRAPATHS:prepend := "${HSOC_APOLLO_LINUX_SRC}/tools/testing/selftests/pci_endpoint:${HSOC_APOLLO_LINUX_SRC}/tools/testing/selftests:${HSOC_APOLLO_LINUX_SRC}/include/uapi/linux:"

SRC_URI = " \
    file://Makefile;subdir=linux/tools/testing/selftests/pci_endpoint \
    file://pci_endpoint_test.c;subdir=linux/tools/testing/selftests/pci_endpoint \
    file://lib.mk;subdir=linux/tools/testing/selftests \
    file://kselftest.h;subdir=linux/tools/testing/selftests \
    file://kselftest_harness.h;subdir=linux/tools/testing/selftests \
    file://pcitest.h;subdir=linux/include/uapi/linux \
"

S = "${UNPACKDIR}/linux/tools/testing/selftests/pci_endpoint"

EXTRA_OEMAKE = " \
    CC='${CC}' \
    USERCFLAGS='${CFLAGS} ${CPPFLAGS}' \
    USERLDFLAGS='${LDFLAGS}' \
    OUTPUT='${B}' \
"

do_compile() {
    oe_runmake -C ${S}
}

do_install() {
    install -Dm 0755 ${B}/pci_endpoint_test \
        ${D}${bindir}/pci_endpoint_test
}

COMPATIBLE_MACHINE = "^apollo-qvp$"
PACKAGE_ARCH = "${MACHINE_ARCH}"
EXCLUDE_FROM_WORLD = "1"
