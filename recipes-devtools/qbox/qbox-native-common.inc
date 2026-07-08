inherit cmake externalsrc pkgconfig python3native native

EXTERNALSRC_BUILD ?= "${WORKDIR}/build"
S = "${EXTERNALSRC}"
B = "${EXTERNALSRC_BUILD}"

QBOX_NATIVE_COMMON_DEPENDS ?= "libslirp-native \
                              pixman-native \
                              python3-setuptools-native \
                              python3-wheel-native \
                              zlib-native"
DEPENDS:append = " ${QBOX_NATIVE_COMMON_DEPENDS}"
