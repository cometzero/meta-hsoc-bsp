#
# SPDX-License-Identifier: MIT
#

inherit externalsrc

TFM_PLATFORM:pn-trusted-firmware-m:apollo-fvp = "arm/rse/automotive_rd/apollo-fvp"

EXTERNALSRC:pn-trusted-firmware-m:apollo-fvp = "${HSOC_APOLLO_TFM_SRC}"
EXTERNALSRC_BUILD:pn-trusted-firmware-m:apollo-fvp = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS:pn-trusted-firmware-m:apollo-fvp = ""

TRUSTED_FIRMWARE_M_HSOC_APOLLO_BL1_KEYS_DIR = "${B}/externalsrc-keys"

# devtool modify keeps non-primary TF-M repositories as fetch dependencies
# while the patched TF-M source tree is provided by EXTERNALSRC.
TRUSTED_FIRMWARE_M_HSOC_APOLLO_GIT_DEPENDENCIES = "\
    ${SRC_URI_TRUSTED_FIRMWARE_M_EXTRAS};branch=${SRCBRANCH_tfm-extras};name=tfm-extras;destsuffix=git/tfm-extras \
    ${SRC_URI_TRUSTED_FIRMWARE_M_TESTS};branch=${SRCBRANCH_tfm-tests};name=tfm-tests;destsuffix=git/tf-m-tests \
    ${SRC_URI_TRUSTED_FIRMWARE_M_CMSIS};branch=${SRCBRANCH_cmsis};name=cmsis;destsuffix=git/cmsis \
    ${SRC_URI_TRUSTED_FIRMWARE_M_MBEDTLS};branch=${SRCBRANCH_mbedtls};name=mbedtls;destsuffix=git/mbedtls \
    ${SRC_URI_TRUSTED_FIRMWARE_M_MCUBOOT};branch=${SRCBRANCH_mcuboot};name=mcuboot;destsuffix=git/mcuboot \
    ${SRC_URI_TRUSTED_FIRMWARE_M_QCBOR};branch=${SRCBRANCH_qcbor};name=qcbor;destsuffix=git/qcbor \
    ${SRC_URI_TRUSTED_FIRMWARE_M_PSA_ADAC};branch=${SRCBRANCH_tfm-psa-adac};name=tfm-psa-adac;destsuffix=git/tfm-psa-adac \
    ${SRC_URI_TRUSTED_FIRMWARE_M_T_COSE};branch=${SRCBRANCH_tcose};name=tcose;destsuffix=git/t_cose \
"

SRC_URI:remove:pn-trusted-firmware-m:apollo-fvp = "${TRUSTED_FIRMWARE_M_HSOC_APOLLO_GIT_DEPENDENCIES}"
SRC_URI:append:pn-trusted-firmware-m:apollo-fvp = " \
    ${SRC_URI_TRUSTED_FIRMWARE_M_EXTRAS};branch=${SRCBRANCH_tfm-extras};name=tfm-extras;destsuffix=git/tfm-extras;type=git-dependency \
    ${SRC_URI_TRUSTED_FIRMWARE_M_TESTS};branch=${SRCBRANCH_tfm-tests};name=tfm-tests;destsuffix=git/tf-m-tests;type=git-dependency \
    ${SRC_URI_TRUSTED_FIRMWARE_M_CMSIS};branch=${SRCBRANCH_cmsis};name=cmsis;destsuffix=git/cmsis;type=git-dependency \
    ${SRC_URI_TRUSTED_FIRMWARE_M_MBEDTLS};branch=${SRCBRANCH_mbedtls};name=mbedtls;destsuffix=git/mbedtls;type=git-dependency \
    ${SRC_URI_TRUSTED_FIRMWARE_M_MCUBOOT};branch=${SRCBRANCH_mcuboot};name=mcuboot;destsuffix=git/mcuboot;type=git-dependency \
    ${SRC_URI_TRUSTED_FIRMWARE_M_QCBOR};branch=${SRCBRANCH_qcbor};name=qcbor;destsuffix=git/qcbor;type=git-dependency \
    ${SRC_URI_TRUSTED_FIRMWARE_M_PSA_ADAC};branch=${SRCBRANCH_tfm-psa-adac};name=tfm-psa-adac;destsuffix=git/tfm-psa-adac;type=git-dependency \
    ${SRC_URI_TRUSTED_FIRMWARE_M_T_COSE};branch=${SRCBRANCH_tcose};name=tcose;destsuffix=git/t_cose;type=git-dependency \
"

EXTRA_OECMAKE:remove:pn-trusted-firmware-m:apollo-fvp = "-DMBEDCRYPTO_PATH=${S}/../mbedtls -DTFM_TEST_REPO_PATH=${S}/../tf-m-tests -DTFM_EXTRAS_REPO_PATH=${S}/../tfm-extras -DMCUBOOT_PATH=${S}/../mcuboot -DQCBOR_PATH=${S}/../qcbor -DCMSIS_PATH=${S}/../cmsis -DT_COSE_PATH=${S}/../t_cose"
EXTRA_OECMAKE:append:pn-trusted-firmware-m:apollo-fvp = " \
    -DMBEDCRYPTO_PATH=${UNPACKDIR}/git/mbedtls \
    -DTFM_TEST_REPO_PATH=${UNPACKDIR}/git/tf-m-tests \
    -DTFM_EXTRAS_REPO_PATH=${UNPACKDIR}/git/tfm-extras \
    -DMCUBOOT_PATH=${UNPACKDIR}/git/mcuboot \
    -DQCBOR_PATH=${UNPACKDIR}/git/qcbor \
    -DCMSIS_PATH=${UNPACKDIR}/git/cmsis \
    -DT_COSE_PATH=${UNPACKDIR}/git/t_cose \
    -DTFM_BL1_2_CM_SIGNING_KEY_PATH=${TRUSTED_FIRMWARE_M_HSOC_APOLLO_BL1_KEYS_DIR}/bl1_dummy_rotpk.pub \
    -DTFM_BL1_2_DM_SIGNING_KEY_PATH=${TRUSTED_FIRMWARE_M_HSOC_APOLLO_BL1_KEYS_DIR}/bl1_dummy_rotpk_1.pub \
"

LIC_FILES_CHKSUM:pn-trusted-firmware-m:apollo-fvp = " \
    file://license.rst;md5=07f368487da347f3c7bd0fc3085f3afa \
    file://${UNPACKDIR}/git/tf-m-tests/license.rst;md5=4481bae2221b0cfca76a69fb3411f390 \
    file://${UNPACKDIR}/git/mbedtls/LICENSE;md5=379d5819937a6c2f1ef1630d341e026d \
    file://${UNPACKDIR}/git/mcuboot/LICENSE;md5=b6ee33f1d12a5e6ee3de1e82fb51eeb8 \
    file://${UNPACKDIR}/git/tfm-psa-adac/license.rst;md5=07f368487da347f3c7bd0fc3085f3afa \
    file://${UNPACKDIR}/git/t_cose/LICENSE;md5=b2ebdbfb82602b97aa628f64cf4b65ad \
"

do_configure[depends] += "quilt-native:do_populate_sysroot"
do_configure[prefuncs] += "do_apply_local_src_patches"

do_configure:prepend:pn-trusted-firmware-m:apollo-fvp() {
    install -d ${TRUSTED_FIRMWARE_M_HSOC_APOLLO_BL1_KEYS_DIR}
    install -m 0600 ${S}/bl1/bl1_2/bl1_dummy_rotpk.prv \
        ${TRUSTED_FIRMWARE_M_HSOC_APOLLO_BL1_KEYS_DIR}/bl1_dummy_rotpk.prv
    install -m 0644 ${S}/bl1/bl1_2/bl1_dummy_rotpk.pub \
        ${TRUSTED_FIRMWARE_M_HSOC_APOLLO_BL1_KEYS_DIR}/bl1_dummy_rotpk.pub
    install -m 0600 ${S}/bl1/bl1_2/bl1_dummy_rotpk_1.prv \
        ${TRUSTED_FIRMWARE_M_HSOC_APOLLO_BL1_KEYS_DIR}/bl1_dummy_rotpk_1.prv
    install -m 0644 ${S}/bl1/bl1_2/bl1_dummy_rotpk_1.pub \
        ${TRUSTED_FIRMWARE_M_HSOC_APOLLO_BL1_KEYS_DIR}/bl1_dummy_rotpk_1.pub
}

do_apply_local_src_patches:apollo-fvp() {
    apply_local_src_patches \
        ${ZENA_CSS_BSP_LAYER}/recipes-bsp/trusted-firmware-m/files/tf-m-extras/fvp-rd-aspen \
        ${UNPACKDIR}/git/tfm-extras
    apply_local_src_patches ${S}/lib/ext/tf-m-extras ${UNPACKDIR}/git/tfm-extras
    apply_local_src_patches ${S}/lib/ext/qcbor ${UNPACKDIR}/git/qcbor
    apply_local_src_patches ${S}/lib/ext/mbedcrypto ${UNPACKDIR}/git/mbedtls
    apply_local_src_patches ${S}/lib/ext/t_cose ${UNPACKDIR}/git/t_cose
}
