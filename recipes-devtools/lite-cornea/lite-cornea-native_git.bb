SUMMARY = "GDB remote bridge for the Arm Fast Models Iris interface"
DESCRIPTION = "lite-cornea translates GDB remote serial protocol requests into Arm Iris API operations."
HOMEPAGE = "https://github.com/Linaro/lite-cornea"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=86d3f3a95c324c9479bd8986968f4327"

PV = "0.1.0+git"

SRC_URI = "git://github.com/Linaro/lite-cornea.git;protocol=https;branch=main"
SRCREV = "eebf9124814b7fad02920989d9d635891e22c1e1"

PREMIRRORS:prepend = " \
    https://crates.io/api/v1/crates/([^/]+)/([^/]+)/download \
    https://static.crates.io/crates/\1/\1-\2.crate \
"

S = "${WORKDIR}/git"

inherit cargo cargo-update-recipe-crates native

require lite-cornea-crates.inc
