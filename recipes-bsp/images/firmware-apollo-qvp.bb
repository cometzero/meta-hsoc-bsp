#
# SPDX-License-Identifier: MIT
#

APOLLO_FIRMWARE_MACHINE = "apollo-qvp"

require firmware-apollo-common.inc

DEPENDS += "python3-native"

HSOC_AUTO_SOLUTIONS_BASE ?= "${@os.path.realpath(os.path.join(d.getVar('HSOC_APOLLO_BASE'), '..'))}"
APOLLO_QVP_RSE_OTP_IMAGE_SIZE ?= "0x10000"
APOLLO_QVP_RSE_OTP_IMAGE_SIZE_BYTES = "${@str(int(d.getVar('APOLLO_QVP_RSE_OTP_IMAGE_SIZE'), 0))}"
APOLLO_QVP_RSE_OTP_IMAGE ?= "${RECIPE_SYSROOT}/firmware/rse-otp-image.img"
APOLLO_QVP_TFM_WORK_ROOT ?= "${TMPDIR}/work/${MACHINE_ARCH}${TARGET_VENDOR}-${TARGET_OS}/trusted-firmware-m"

do_generate_rse_otp_image[depends] += "\
    trusted-firmware-m:do_compile \
    trusted-firmware-m-scripts-native:do_populate_sysroot \
    python3-native:do_populate_sysroot \
"
do_generate_rse_otp_image[file-checksums] += "${HSOC_AUTO_SOLUTIONS_BASE}/scripts/setup/provision_rse_otp_image.py:True"
do_generate_rse_otp_image[dirs] += "${RECIPE_SYSROOT}/firmware"

do_generate_rse_otp_image() {
    provision_script="${HSOC_AUTO_SOLUTIONS_BASE}/scripts/setup/provision_rse_otp_image.py"
    native_python="${RECIPE_SYSROOT_NATIVE}/usr/bin/python3-native/python3"

    if [ ! -f "${provision_script}" ]; then
        bbfatal "RSE OTP provisioning script not found: ${provision_script}"
    fi
    if [ ! -x "${native_python}" ]; then
        bbfatal "Native Python not found: ${native_python}"
    fi

    tfm_build=""
    for candidate in "${APOLLO_QVP_TFM_WORK_ROOT}"/*/build; do
        if [ -f "${candidate}/CMakeCache.txt" ] && \
           [ -f "${candidate}/platform/target/common/config/otp_config.pickle" ]; then
            tfm_build="${candidate}"
        fi
    done
    if [ -z "${tfm_build}" ]; then
        bbfatal "Could not find trusted-firmware-m build outputs under ${APOLLO_QVP_TFM_WORK_ROOT}"
    fi

    install -d "$(dirname "${APOLLO_QVP_RSE_OTP_IMAGE}")"
    "${native_python}" "${provision_script}" \
        --root "${HSOC_AUTO_SOLUTIONS_BASE}" \
        --tfm-build-dir "${tfm_build}" \
        --output "${APOLLO_QVP_RSE_OTP_IMAGE}" \
        --size "${APOLLO_QVP_RSE_OTP_IMAGE_SIZE}"

    expected="${APOLLO_QVP_RSE_OTP_IMAGE_SIZE_BYTES}"
    actual="$(stat -c '%s' "${APOLLO_QVP_RSE_OTP_IMAGE}")"
    if [ "${actual}" != "${expected}" ]; then
        bbfatal "Generated RSE OTP image is ${actual} bytes, expected ${expected}: ${APOLLO_QVP_RSE_OTP_IMAGE}"
    fi
}
addtask generate_rse_otp_image after do_prepare_recipe_sysroot before do_genimage

do_genimage:append() {
    install -m 0644 "${APOLLO_QVP_RSE_OTP_IMAGE}" "${B}/rse-otp-image.img"
}
