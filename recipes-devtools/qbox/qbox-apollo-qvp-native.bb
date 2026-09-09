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

QBOX_CORE_TREE_HASH_FILES = "${@srctree_hash_files(d, d.getVar('HSOC_APOLLO_QBOX_SRC'))}"

python __anonymous() {
    qbox_core_hash_files = d.getVar('QBOX_CORE_TREE_HASH_FILES')
    d.appendVarFlag('do_configure', 'file-checksums', ' ' + qbox_core_hash_files)
    d.appendVarFlag('do_compile', 'file-checksums', ' ' + qbox_core_hash_files)
}

QBOX_APOLLO_BUILD_TARGET ?= "apollo_fvp_full_system"
QBOX_APOLLO_RUN_UNIT_TESTS ?= "1"
QBOX_APOLLO_UNIT_TEST_TARGET ?= "qbox_platform_unit_tests"
QBOX_APOLLO_UNIT_TEST_LABEL ?= "^qbox-platform-"
QBOX_APOLLO_UNIT_TEST_EXCLUDE_REGEX ?= ""
QBOX_APOLLO_UNIT_TEST_TIMEOUT ?= "5"
# Space-separated directories relative to the QBox core tests/ directory.
# Only these suites are configured and built; the regex narrows execution.
QBOX_CORE_TEST_DIRS ?= "components sync utils qbox"
QBOX_CORE_TEST_REGEX ?= ".*"
# Measured >=5s (or timed out at 5s) in the default profile on 2026-09-09.
# Keep fast AArch64 halt/timer/UART/SMMU/shutdown tests enabled. This is an
# explicit exclusion, not a PASS for the known post-simulation hangs.
QBOX_CORE_TEST_SLOW_REGEX ?= "^(router-cache-bench-enhanced|aarch64-start-in-reset-release-test)$|^(aarch64-(simple-write-test|dmi-test(-concurrent-inval|-async-inval)?|ld-st-excl-fail-test|write_read)|reset-test-(system|cpu)):sync-pol=multithread-freerunning:num-cpu=(1|2|4):icount=false:threading=MULTI:accel=tcg:time_sync_strategy=quantum_keeper$"
# Intermittent post-sc_stop hangs; longer successful repeats do not resolve
# the recorded failures. See doc/qbox/unit-test-stability-2026-09-09.md.
QBOX_CORE_TEST_UNSTABLE_REGEX ?= "^(aarch64-managed-uart-fifo-closed-writer|aarch64-managed-timer-wfi-timer-wake)$"
QBOX_CORE_TEST_EXCLUDE_REGEX ?= "${@'|'.join(pattern for pattern in (d.getVar('QBOX_CORE_TEST_SLOW_REGEX'), d.getVar('QBOX_CORE_TEST_UNSTABLE_REGEX')) if pattern)}"
QBOX_CPU_TEST_ARCHS ?= "aarch64"
# Bound the default native build to the Apollo scheduling profile. The
# standalone QBox defaults and additional matrix axes remain selectable.
QBOX_CPU_TEST_SYNC_POLICY_COMBINATION ?= "multithread-freerunning"
QBOX_CPU_TEST_NUM_CPU_COMBINATION ?= "1 2 4"
QBOX_ENABLE_MCIPS_TESTS ?= "OFF"
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
                  -DCMAKE_BUILD_TYPE=RelWithDebInfo \
                  -DFETCHCONTENT_FULLY_DISCONNECTED=OFF \
                  -DCPM_SOURCE_FILE=${CPM_SOURCE_FILE} \
                  -DQBOX_USE_SYSTEM_LIBQEMU=ON \
                  -DENABLE_PYTHON_BINDER=OFF \
                  -DGS_ENABLE_VIRCLRENDERER=OFF \
                  -DGS_ENABLE_VIRGLRENDERER=OFF \
                  -DQBOX_CORE_TEST_DIRS='${@';'.join(d.getVar('QBOX_CORE_TEST_DIRS').split())}' \
                  -DQBOX_CPU_TEST_ARCHS='${@';'.join(d.getVar('QBOX_CPU_TEST_ARCHS').split())}' \
                  -DQBOX_CPU_TEST_SYNC_POLICY_COMBINATION='${@';'.join(d.getVar('QBOX_CPU_TEST_SYNC_POLICY_COMBINATION').split())}' \
                  -DQBOX_CPU_TEST_NUM_CPU_COMBINATION='${@';'.join(d.getVar('QBOX_CPU_TEST_NUM_CPU_COMBINATION').split())}' \
                  -DQBOX_ENABLE_MCIPS_TESTS=${QBOX_ENABLE_MCIPS_TESTS} \
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

    : > "${T}/qbox-platform-unit-tests.excluded.list"
    : > "${T}/qbox-core-unit-tests.excluded.list"
    bbnote "Building Apollo QBox unit test target: ${QBOX_APOLLO_UNIT_TEST_TARGET}"
    cmake_runcmake_build --target ${QBOX_APOLLO_UNIT_TEST_TARGET}

    list_log="${T}/qbox-platform-unit-tests.list"
    set --
    if [ -n "${QBOX_APOLLO_UNIT_TEST_EXCLUDE_REGEX}" ]; then
        bbnote "Excluded platform tests: ${QBOX_APOLLO_UNIT_TEST_EXCLUDE_REGEX}"
        ctest --test-dir "${B}" -N -L "${QBOX_APOLLO_UNIT_TEST_LABEL}" \
            -R "${QBOX_APOLLO_UNIT_TEST_EXCLUDE_REGEX}" \
            > "${T}/qbox-platform-unit-tests.excluded.list"
        set -- -E "${QBOX_APOLLO_UNIT_TEST_EXCLUDE_REGEX}"
    fi
    bbnote "Listing Apollo QBox unit tests with label: ${QBOX_APOLLO_UNIT_TEST_LABEL}"
    ctest --test-dir "${B}" -N -L "${QBOX_APOLLO_UNIT_TEST_LABEL}" "$@" \
        --no-tests=error > "$list_log"
    cat "$list_log"

    bbnote "Running Apollo QBox unit tests with label: ${QBOX_APOLLO_UNIT_TEST_LABEL}"
    ctest --test-dir "${B}" -L "${QBOX_APOLLO_UNIT_TEST_LABEL}" "$@" \
        --no-tests=error --output-on-failure --timeout ${QBOX_APOLLO_UNIT_TEST_TIMEOUT} \
        --output-log "${T}/qbox-platform-unit-tests.log" \
        --output-junit "${T}/qbox-platform-unit-tests.xml"

    if [ -n "${QBOX_CORE_TEST_DIRS}" ]; then
        bbnote "Building selected QBox core suites: ${QBOX_CORE_TEST_DIRS}"
        cmake_runcmake_build --target qbox_core_unit_tests
        bbnote "Running selected QBox core tests: ${QBOX_CORE_TEST_REGEX}"
        set --
        if [ -n "${QBOX_CORE_TEST_EXCLUDE_REGEX}" ]; then
            bbnote "Excluded core tests: ${QBOX_CORE_TEST_EXCLUDE_REGEX}"
            ctest --test-dir "${B}/tests/core" -N -R "${QBOX_CORE_TEST_EXCLUDE_REGEX}" \
                > "${T}/qbox-core-unit-tests.excluded.list"
            set -- -E "${QBOX_CORE_TEST_EXCLUDE_REGEX}"
        fi
        ctest --test-dir "${B}/tests/core" -N -R "${QBOX_CORE_TEST_REGEX}" "$@" \
            --no-tests=error > "${T}/qbox-core-unit-tests.list"
        cat "${T}/qbox-core-unit-tests.list"
        ctest --test-dir "${B}/tests/core" -R "${QBOX_CORE_TEST_REGEX}" "$@" \
            --no-tests=error --output-on-failure --timeout ${QBOX_APOLLO_UNIT_TEST_TIMEOUT} \
            --output-log "${T}/qbox-core-unit-tests.log" \
            --output-junit "${T}/qbox-core-unit-tests.xml"
    else
        bbnote "QBox core tests explicitly disabled by empty QBOX_CORE_TEST_DIRS"
    fi
}
do_check[doc] = "Build and run Apollo QBox native unit tests with CTest"
# Monitor API tests start a loopback HTTP server in the test process.
do_check[network] = "1"
do_check[dirs] = "${B}"
addtask check after do_compile before do_install
