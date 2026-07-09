SUMMARY = "Apollo QVP native QBox provider"
DESCRIPTION = "Builds the Apollo QVP host-side QBox provider from local qbox-platform, qbox, and QEMU/libqemu source trees and installs it into the native sysroot."
HOMEPAGE = "https://github.com/quic/qbox"
LICENSE = "BSD-3-Clause & GPL-2.0-only & LGPL-2.1-only & MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3d73035ac3b78bacc2fa00f27073ad9d \
                    file://${HSOC_APOLLO_QBOX_SRC}/LICENSE;md5=3d73035ac3b78bacc2fa00f27073ad9d \
                    file://${HSOC_APOLLO_QEMU_SRC}/LICENSE;md5=6541297aed25bfd5e8d893ea097e838c \
                    file://${HSOC_APOLLO_QEMU_SRC}/COPYING;md5=a3b50d8b88dcc0eb3d7d39b760b9e821 \
                    file://${HSOC_APOLLO_QEMU_SRC}/COPYING.LIB;md5=f4457173749eb816989d739d14ba7c13 \
                    file://${UNPACKDIR}/CPM-${CPM_VERSION}.cmake;beginline=5;endline=24;md5=9dd5d132b3fe59521a1d0dc7cd7a8d0d"

require qbox-native-common.inc

EXTERNALSRC = "${HSOC_APOLLO_QBOX_PLATFORM_SRC}"
SRC_URI += "file://CPM-${CPM_VERSION}.cmake"

QBOX_APOLLO_BUILD_TARGET ?= "apollo_fvp_full_system"
QBOX_APOLLO_RUN_UNIT_TESTS ?= "0"
QBOX_APOLLO_UNIT_TEST_TARGET ?= "qbox_platform_systemc_component_tests"
QBOX_APOLLO_UNIT_TEST_LABEL ?= "qbox-platform-systemc-components"
CPM_VERSION = "0.40.5"
CPM_SOURCE_FILE = "${UNPACKDIR}/CPM-${CPM_VERSION}.cmake"
CPM_SHA256 = "c46b876ae3b9f994b4f05a4c15553e0485636862064f1fcc9d8b4f832086bc5d"

QBOX_APOLLO_REQUIRED_TARGETS = ""
PACKAGECONFIG:append = "${@bb.utils.contains('QBOX_APOLLO_RUN_UNIT_TESTS', '1', ' unit-tests', '', d)}"
PACKAGECONFIG[unit-tests] = "-DBUILD_TESTING=ON,-DBUILD_TESTING=OFF"

DEPENDS = "qbox-libqemu-native \
           asio-native \
           elfutils-native \
           git-native \
           libzip-native"

EXTRA_OECMAKE += "-DQBOX_CORE_SOURCE_DIR=${HSOC_APOLLO_QBOX_SRC} \
                  -DFETCHCONTENT_FULLY_DISCONNECTED=OFF \
                  -DCPM_SOURCE_FILE=${CPM_SOURCE_FILE} \
                  -DQBOX_USE_SYSTEM_LIBQEMU=ON \
                  -DENABLE_PYTHON_BINDER=OFF \
                  -DGS_ENABLE_VIRCLRENDERER=OFF \
                  -DGS_ENABLE_VIRGLRENDERER=OFF \
                  -DPython3_INCLUDE_DIR=${PYTHON_INCLUDE_DIR} \
                  -DPython3_LIBRARY=${PYTHON_LIBRARY} \
                  -DQBOX_APOLLO_BUILD_TARGET=${QBOX_APOLLO_BUILD_TARGET}"

OECMAKE_TARGET_COMPILE = "${QBOX_APOLLO_BUILD_TARGET}"
do_configure[network] = "1"

QBOX_APOLLO_REQUIRED_TARGETS_SOURCE ?= "${EXTERNALSRC}/CMakeLists.txt"
QBOX_APOLLO_MODULEDIR ?= "${libdir}/qbox/modules"
QBOX_APOLLO_DATADIR ?= "${datadir}/qbox"
QBOX_APOLLO_PLATFORMDIR ?= "${QBOX_APOLLO_DATADIR}/platforms/apollo"

do_configure:prepend() {
    if [ ! -f "${EXTERNALSRC}/CMakeLists.txt" ]; then
        bbfatal "HSOC_APOLLO_QBOX_PLATFORM_SRC is not a qbox-platform source tree: ${EXTERNALSRC}"
    fi
    if [ ! -s "${CPM_SOURCE_FILE}" ]; then
        bbfatal "CPM.cmake is not available: ${CPM_SOURCE_FILE}"
    fi
    if ! echo "${CPM_SHA256}  ${CPM_SOURCE_FILE}" | sha256sum -c - >/dev/null; then
        bbfatal "CPM.cmake checksum mismatch: ${CPM_SOURCE_FILE}"
    fi
    if [ ! -f "${HSOC_APOLLO_QBOX_SRC}/CMakeLists.txt" ]; then
        bbfatal "HSOC_APOLLO_QBOX_SRC is not a qbox source tree: ${HSOC_APOLLO_QBOX_SRC}"
    fi
    if [ ! -f "${HSOC_APOLLO_QEMU_SRC}/CMakeLists.txt" ] || [ ! -x "${HSOC_APOLLO_QEMU_SRC}/configure" ]; then
        bbfatal "HSOC_APOLLO_QEMU_SRC is not a QEMU/libqemu source tree: ${HSOC_APOLLO_QEMU_SRC}"
    fi
}

qbox_apollo_install_file() {
    src="$1"
    dest="$2"
    mode="$3"

    if [ ! -f "$src" ]; then
        bbfatal "qbox-apollo-qvp-native: missing required Apollo QBox artifact: $src"
    fi

    install -d "$(dirname "$dest")"
    install -m "$mode" "$src" "$dest"
}

qbox_apollo_find_in_build() {
    name="$1"
    find "${B}" -type f -name "$name" | sort | head -n 1
}

qbox_apollo_install_named_artifact() {
    name="$1"
    dest="$2"
    mode="$3"
    artifact="$(qbox_apollo_find_in_build "$name")"

    if [ -z "$artifact" ]; then
        bbfatal "qbox-apollo-qvp-native: missing required Apollo QBox artifact '${name}' under ${B}"
    fi

    qbox_apollo_install_file "$artifact" "$dest" "$mode"
}

qbox_apollo_install_module() {
    target="$1"
    artifact=""

    for candidate in "${B}/${target}.so" "${B}/lib${target}.so"; do
        if [ -f "$candidate" ]; then
            artifact="$candidate"
            break
        fi
    done

    if [ -z "$artifact" ]; then
        artifact="$(find "${B}" -type f \( -name "${target}.so" -o -name "lib${target}.so" \) | sort | head -n 1)"
    fi

    if [ -z "$artifact" ]; then
        bbfatal "qbox-apollo-qvp-native: missing required Apollo QBox target '${target}' under ${B}"
    fi

    qbox_apollo_install_file "$artifact" "${D}${QBOX_APOLLO_MODULEDIR}/$(basename "$artifact")" 0644
}

qbox_apollo_install_runtime_library_dir() {
    srcdir="$1"

    if [ ! -d "$srcdir" ]; then
        bbfatal "qbox-apollo-qvp-native: missing required runtime library directory: $srcdir"
    fi

    install -d "${D}${libdir}"
    find "$srcdir" -maxdepth 1 \( -type f -o -type l \) -name "lib*.so*" | sort | while read -r lib; do
        base="$(basename "$lib")"
        if [ -L "$lib" ]; then
            ln -sf "$(readlink "$lib")" "${D}${libdir}/$base"
        else
            install -m 0644 "$lib" "${D}${libdir}/$base"
        fi
    done
}

qbox_apollo_install_runtime_libraries() {
    qbox_apollo_install_runtime_library_dir "${B}/_deps/report-build"
    qbox_apollo_install_runtime_library_dir "${B}/_deps/fmt-build"
    qbox_apollo_install_runtime_library_dir "${B}/_deps/systemccci-build/configuration/src"
    qbox_apollo_install_runtime_library_dir "${B}/_deps/systemccci-build/inspection/src"
    qbox_apollo_install_runtime_library_dir "${B}/_deps/systemclanguage-build/src"
    qbox_apollo_install_runtime_library_dir "${B}/_deps/rpclib-build"

    for required in libreporting.so libfmt.so.9 libcci-config.so.1.0 libsystemc.so.3.0 librpc.so; do
        if [ ! -e "${D}${libdir}/$required" ]; then
            bbfatal "qbox-apollo-qvp-native: missing installed runtime library: ${libdir}/$required"
        fi
    done
}

qbox_apollo_install_data_tree() {
    srcdir="$1"
    destdir="$2"

    if [ ! -d "$srcdir" ]; then
        bbfatal "qbox-apollo-qvp-native: missing required Apollo QBox data directory: $srcdir"
    fi

    find "$srcdir" -type d | while read -r dir; do
        rel="${dir#$srcdir}"
        install -d "${destdir}${rel}"
    done

    find "$srcdir" -type f | while read -r file; do
        rel="${file#$srcdir/}"
        install -d "$(dirname "${destdir}/${rel}")"
        install -m 0644 "$file" "${destdir}/${rel}"
    done
}

qbox_apollo_required_targets() {
    awk '
        /^[[:space:]]*set[[:space:]]*\([[:space:]]*QBOX_APOLLO_REQUIRED_TARGETS/ {
            in_targets = 1
            next
        }
        in_targets && /^[[:space:]]*\)/ {
            exit
        }
        in_targets {
            sub(/#.*/, "")
            for (i = 1; i <= NF; i++) {
                print $i
            }
        }
    ' "${QBOX_APOLLO_REQUIRED_TARGETS_SOURCE}"
}

do_install() {
    if [ ! -f "${QBOX_APOLLO_REQUIRED_TARGETS_SOURCE}" ]; then
        bbfatal "qbox-apollo-qvp-native: missing Apollo target metadata source: ${QBOX_APOLLO_REQUIRED_TARGETS_SOURCE}"
    fi

    required_targets="$(qbox_apollo_required_targets)"
    if [ -z "$required_targets" ]; then
        bbfatal "qbox-apollo-qvp-native: no Apollo QBox required targets found in ${QBOX_APOLLO_REQUIRED_TARGETS_SOURCE}"
    fi

    qbox_apollo_install_named_artifact "platforms-vp" "${D}${bindir}/platforms-vp" 0755
    qbox_apollo_install_named_artifact "libqbox.so" "${D}${libdir}/libqbox.so" 0644

    liblua="$(qbox_apollo_find_in_build "liblua.so")"
    if [ -n "$liblua" ]; then
        qbox_apollo_install_file "$liblua" "${D}${libdir}/liblua.so" 0644
    fi
    qbox_apollo_install_runtime_libraries

    for target in $required_targets; do
        if [ "$target" = "platforms-vp" ]; then
            continue
        fi
        qbox_apollo_install_module "$target"
    done

    qbox_apollo_install_data_tree \
        "${HSOC_APOLLO_QBOX_PLATFORM_SRC}/platforms/apollo" \
        "${D}${QBOX_APOLLO_PLATFORMDIR}"

    if [ ! -f "${D}${QBOX_APOLLO_PLATFORMDIR}/apollo-qvp.lua" ]; then
        bbfatal "qbox-apollo-qvp-native: missing installed Apollo QVP Lua config: ${QBOX_APOLLO_PLATFORMDIR}/apollo-qvp.lua"
    fi

    if [ -d "${HSOC_APOLLO_QBOX_PLATFORM_SRC}/fw" ]; then
        qbox_apollo_install_data_tree \
            "${HSOC_APOLLO_QBOX_PLATFORM_SRC}/fw" \
            "${D}${QBOX_APOLLO_DATADIR}/fw"
    fi

    install -d "${D}${QBOX_APOLLO_DATADIR}"
    {
        echo "provider=${PN}"
        echo "version=${PV}"
        echo "machine=${MACHINE}"
        echo "aggregate_target=${QBOX_APOLLO_BUILD_TARGET}"
        echo "executable=${bindir}/platforms-vp"
        echo "library=${libdir}/libqbox.so"
        echo "module_dir=${QBOX_APOLLO_MODULEDIR}"
        echo "platform_config=${QBOX_APOLLO_PLATFORMDIR}/apollo-qvp.lua"
        echo "targets_source=${QBOX_APOLLO_REQUIRED_TARGETS_SOURCE}"
        printf "required_targets="
        printf "%s " $required_targets
        printf "\n"
    } > "${D}${QBOX_APOLLO_DATADIR}/qbox-apollo-qvp-provider.env"
}

do_check() {
    if ! ${@bb.utils.contains('PACKAGECONFIG', 'unit-tests', 'true', 'false', d)}; then
        bbnote "qbox-apollo-qvp-native unit tests disabled; set QBOX_APOLLO_RUN_UNIT_TESTS = \"1\" or enable PACKAGECONFIG unit-tests"
        return 0
    fi

    bbnote "Building Apollo QBox unit test target: ${QBOX_APOLLO_UNIT_TEST_TARGET}"
    cmake_runcmake_build --target ${QBOX_APOLLO_UNIT_TEST_TARGET}

    list_log="${T}/qbox-unit-tests.list"
    bbnote "Listing Apollo QBox unit tests with label: ${QBOX_APOLLO_UNIT_TEST_LABEL}"
    ctest --test-dir "${B}" -N -L "${QBOX_APOLLO_UNIT_TEST_LABEL}" | tee "$list_log"
    if ! grep -Eq 'Total Tests: [1-9][0-9]*' "$list_log"; then
        bbfatal "qbox-apollo-qvp-native: no CTest tests matched ${QBOX_APOLLO_UNIT_TEST_LABEL}"
    fi

    bbnote "Running Apollo QBox unit tests with label: ${QBOX_APOLLO_UNIT_TEST_LABEL}"
    ctest --test-dir "${B}" -L "${QBOX_APOLLO_UNIT_TEST_LABEL}" --output-on-failure
}
do_check[doc] = "Build and run Apollo QBox native unit tests with CTest"
do_check[dirs] = "${B}"
addtask check after do_compile before do_install
