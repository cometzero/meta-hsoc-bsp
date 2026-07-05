SUMMARY = "Apollo QVP native QBox runtime bundle"
DESCRIPTION = "Builds the Apollo QVP host-side QBox runtime from local qbox-platform, qbox, and QEMU/libqemu source trees and deploys a runnable native bundle."
HOMEPAGE = "https://github.com/quic/qbox"
LICENSE = "BSD-3-Clause & GPL-2.0-only & LGPL-2.1-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3d73035ac3b78bacc2fa00f27073ad9d \
                    file://${HSOC_APOLLO_QBOX_SRC}/LICENSE;md5=3d73035ac3b78bacc2fa00f27073ad9d \
                    file://${HSOC_APOLLO_QEMU_SRC}/LICENSE;md5=6541297aed25bfd5e8d893ea097e838c \
                    file://${HSOC_APOLLO_QEMU_SRC}/COPYING;md5=a3b50d8b88dcc0eb3d7d39b760b9e821 \
                    file://${HSOC_APOLLO_QEMU_SRC}/COPYING.LIB;md5=f4457173749eb816989d739d14ba7c13"

inherit cmake externalsrc deploy native

EXTERNALSRC = "${HSOC_APOLLO_QBOX_PLATFORM_SRC}"
EXTERNALSRC_BUILD = "${WORKDIR}/build"
S = "${EXTERNALSRC}"
B = "${EXTERNALSRC_BUILD}"

QBOX_APOLLO_BUILD_TARGET ?= "apollo_fvp_full_system"

QBOX_APOLLO_REQUIRED_TARGETS = "platforms-vp \
    keep_alive \
    addrtr \
    router \
    gs_memory \
    host_scr \
    loader \
    char_backend_file \
    char_backend_stdio \
    uart-pl011 \
    global_peripheral_initiator \
    cpu_arm_cortexA720AE \
    cpu_arm_cortexR82 \
    arm_gicv3 \
    arm_gicv3_its \
    qemu_gpex \
    virtio_mmio_blk \
    virtio_mmio_net \
    virtio_mmio_rng \
    arm_smmuv3 \
    mmu720ae \
    reset_gpio \
    pl031 \
    sbsa_gwdt \
    cpu_arm_cortexM55 \
    nvic_armv7m \
    ApolloRseCPU \
    qemu_cc3xx \
    qemu_arm_arch_timer_mmio \
    qemu_hexagon_qtimer \
    mhu320ae \
    gicx00_multiview \
    gic720ae_messreg \
    zena_fmu \
    zena_ssu \
    host_cmn_cyprus \
    host_gtimer \
    host_ni710ae_nci \
    host_ppu \
    cc3xx \
    dma350 \
    rse_atu \
    rse_integrity_checker \
    rse_kmu \
    rse_lcm \
    rse_protection_ctrl \
    rse_sam \
    strata_flash_j3 \
    host_smcf_mgi \
    host_system_pll \
    reset_fanout \
    rse_sysctrl"

DEPENDS = "qbox-libqemu-native \
           elfutils-native \
           git-native \
           libslirp-native \
           libzip-native \
           ninja-native \
           pixman-native \
           pkgconfig-native \
           python3-native \
           zlib-native"

EXTRA_OECMAKE += "-DQBOX_CORE_SOURCE_DIR=${HSOC_APOLLO_QBOX_SRC} \
                  -DQBOX_QEMU_SOURCE_DIR=${HSOC_APOLLO_QEMU_SRC} \
                  -DQEMU_SOURCE_DIR=${HSOC_APOLLO_QEMU_SRC} \
                  -DFETCHCONTENT_SOURCE_DIR_QEMU=${HSOC_APOLLO_QEMU_SRC} \
                  -DFETCHCONTENT_SOURCE_DIR_LIBQEMU=${HSOC_APOLLO_QEMU_SRC} \
                  -DLIBQEMU_GIT=file://${HSOC_APOLLO_QEMU_SRC} \
                  -DLIBQEMU_BUILD_ALWAYS=OFF \
                  -DQBOX_APOLLO_BUILD_TARGET=${QBOX_APOLLO_BUILD_TARGET}"

OECMAKE_TARGET_COMPILE = "${QBOX_APOLLO_BUILD_TARGET}"
do_install[noexec] = "1"

do_configure:prepend() {
    if [ ! -f "${EXTERNALSRC}/CMakeLists.txt" ]; then
        bbfatal "HSOC_APOLLO_QBOX_PLATFORM_SRC is not a qbox-platform source tree: ${EXTERNALSRC}"
    fi
    if [ ! -f "${HSOC_APOLLO_QBOX_SRC}/CMakeLists.txt" ]; then
        bbfatal "HSOC_APOLLO_QBOX_SRC is not a qbox source tree: ${HSOC_APOLLO_QBOX_SRC}"
    fi
    if [ ! -f "${HSOC_APOLLO_QEMU_SRC}/CMakeLists.txt" ] || [ ! -x "${HSOC_APOLLO_QEMU_SRC}/configure" ]; then
        bbfatal "HSOC_APOLLO_QEMU_SRC is not a QEMU/libqemu source tree: ${HSOC_APOLLO_QEMU_SRC}"
    fi
}

python do_deploy() {
import json
import shutil
import stat
from pathlib import Path


build_dir = Path(d.getVar("B")).resolve()
bundle_dir = Path(d.getVar("DEPLOYDIR")).resolve() / "qbox-apollo-qvp"
platform_src = Path(d.getVar("HSOC_APOLLO_QBOX_PLATFORM_SRC")).resolve()
core_src = Path(d.getVar("HSOC_APOLLO_QBOX_SRC")).resolve()
qemu_src = Path(d.getVar("HSOC_APOLLO_QEMU_SRC")).resolve()
required_targets = d.getVar("QBOX_APOLLO_REQUIRED_TARGETS").split()

search_roots = [build_dir]
required_entries = []
optional_entries = []


def fail(message):
    bb.fatal(f"qbox-apollo-qvp-native: {message}")


def find_one(name, roots):
    matches = []
    for root in roots:
        if not root.exists():
            continue
        matches.extend(path for path in root.rglob(name) if path.is_file())
    matches = sorted(set(matches), key=lambda path: (len(path.parts), str(path)))
    return matches[0] if matches else None


def find_artifact(names, roots):
    for name in names:
        found = find_one(name, roots)
        if found is not None:
            return found
    return None


def copy_file(src, relpath, category, required=True, executable=False, **extra):
    if src is None:
        if required:
            fail(f"missing required artifact for {relpath}")
        return None
    dest = bundle_dir / relpath
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dest)
    if executable:
        mode = dest.stat().st_mode
        dest.chmod(mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    entry = {
        "category": category,
        "source_path": str(src),
        "bundle_path": str(dest),
        "relative_path": str(relpath),
    }  # keep indented so BitBake does not see a task terminator
    entry.update(extra)
    if required:
        required_entries.append(entry)
    else:
        optional_entries.append(entry)
    return dest


def copy_tree(src, relpath, category, required=True):
    if not src.exists():
        if required:
            fail(f"missing required directory: {src}")
        return
    dest = bundle_dir / relpath
    if dest.exists():
        shutil.rmtree(dest)
    shutil.copytree(src, dest)
    for path in sorted(dest.rglob("*")):
        if path.is_file():
            source_path = src / path.relative_to(dest)
            entry = {
                "category": category,
                "source_path": str(source_path),
                "bundle_path": str(path),
                "relative_path": str(path.relative_to(bundle_dir)),
            }  # keep indented so BitBake does not see a task terminator
            if required:
                required_entries.append(entry)
            else:
                optional_entries.append(entry)


if bundle_dir.exists():
    shutil.rmtree(bundle_dir)
bundle_dir.mkdir(parents=True)

copy_file(
    find_one("platforms-vp", search_roots),
    Path("platforms-vp"),
    "executable",
    executable=True,
    target_name="platforms-vp",
    actual_filename="platforms-vp",
)

for target in required_targets:
    if target == "platforms-vp":
        continue
    artifact = find_artifact((f"{target}.so", f"lib{target}.so"), search_roots)
    copy_file(
        artifact,
        Path("lib") / artifact.name if artifact is not None else Path("lib") / f"{target}.so",
        "module",
        target_name=target,
        actual_filename=artifact.name if artifact is not None else None,
    )

copy_file(find_one("libqbox.so", search_roots), Path("lib/libqbox.so"), "shared-library")
copy_file(
    find_one("libqemu-system-aarch64.so", search_roots),
    Path("lib/libqemu-system-aarch64.so"),
    "shared-library",
)
copy_file(find_one("liblua.so", search_roots), Path("lib/liblua.so"), "shared-library", required=False)

copy_tree(platform_src / "platforms" / "apollo", Path("platforms/apollo"), "lua-config")
copy_tree(platform_src / "fw", Path("fw"), "lua-config", required=False)

for rel_share in (Path("install/share"), Path("qbox-core/install/share"), Path("share")):
    share_dir = build_dir / rel_share
    if share_dir.exists():
        copy_tree(share_dir, Path("share"), "share", required=False)
        break

env_file = bundle_dir / "qbox-apollo-qvp-env.sh"
dollar = chr(36)
env_file.write_text(
    "\n".join(
        [
            "#!/bin/sh",
            'QBOX_APOLLO_QVP_BUNDLE_DIR="' + dollar + "{"
            + 'QBOX_APOLLO_QVP_BUNDLE_DIR:-$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)}"',
            "export QBOX_APOLLO_QVP_BUNDLE_DIR",
            'export QBOX_CONF="' + dollar + "{"
            + "QBOX_CONF:-" + dollar + "{QBOX_APOLLO_QVP_BUNDLE_DIR}/platforms/apollo/apollo-qvp.lua}" + '"',
            'export PATH="' + dollar + "{" + "QBOX_APOLLO_QVP_BUNDLE_DIR}:" + dollar + '{PATH}"',
            'export LD_LIBRARY_PATH="' + dollar + "{"
            + "QBOX_APOLLO_QVP_BUNDLE_DIR}/lib" + dollar
            + "{LD_LIBRARY_PATH:+:" + dollar + '{LD_LIBRARY_PATH}}"',
            "",
        ]
    ),
    encoding="utf-8",
)
env_file.chmod(env_file.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
required_entries.append(
    dict(
        category="environment",
        source_path="generated",
        bundle_path=str(env_file),
        relative_path=str(env_file.relative_to(bundle_dir)),
    )
)

manifest = dict(
    bundle="qbox-apollo-qvp",
    recipe=d.getVar("PN"),
    version=d.getVar("PV"),
    machine=d.getVar("MACHINE"),
    aggregate_target=d.getVar("QBOX_APOLLO_BUILD_TARGET"),
    deploy_visible_names=["apollo-qvp", "qbox-apollo-qvp"],
    sources=dict(
        qbox_platform=str(platform_src),
        qbox_core=str(core_src),
        qemu=str(qemu_src),
    ),
    licenses=dict(
        qbox_platform=str(platform_src / "LICENSE"),
        qbox_core=str(core_src / "LICENSE"),
        qemu_license=str(qemu_src / "LICENSE"),
        qemu_copying=str(qemu_src / "COPYING"),
        qemu_copying_lib=str(qemu_src / "COPYING.LIB"),
    ),
    build_dir=str(build_dir),
    required_targets=required_targets,
    required_artifacts=required_entries,
    optional_artifacts=optional_entries,
)

manifest_path = bundle_dir / "qbox-apollo-qvp-manifest.json"
manifest_path.write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")

if not (bundle_dir / "platforms/apollo/apollo-qvp.lua").is_file():
    fail("missing deployed Apollo QVP Lua config: platforms/apollo/apollo-qvp.lua")
}

addtask deploy after do_compile before do_build
