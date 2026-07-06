SUMMARY = "Apollo QVP native QBox runtime bundle"
DESCRIPTION = "Builds the Apollo QVP host-side QBox runtime from local qbox-platform, qbox, and QEMU/libqemu source trees and deploys a runnable native bundle."
HOMEPAGE = "https://github.com/quic/qbox"
LICENSE = "BSD-3-Clause & GPL-2.0-only & LGPL-2.1-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3d73035ac3b78bacc2fa00f27073ad9d \
                    file://${HSOC_APOLLO_QBOX_SRC}/LICENSE;md5=3d73035ac3b78bacc2fa00f27073ad9d \
                    file://${HSOC_APOLLO_QEMU_SRC}/LICENSE;md5=6541297aed25bfd5e8d893ea097e838c \
                    file://${HSOC_APOLLO_QEMU_SRC}/COPYING;md5=a3b50d8b88dcc0eb3d7d39b760b9e821 \
                    file://${HSOC_APOLLO_QEMU_SRC}/COPYING.LIB;md5=f4457173749eb816989d739d14ba7c13"

inherit cmake externalsrc deploy pkgconfig python3native native

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
           asio-native \
           elfutils-native \
           git-native \
           jpeg-native \
           libslirp-native \
           libsdl2-native \
           libzip-native \
           ninja-native \
           pixman-native \
           pkgconfig-native \
           python3-native \
           python3-setuptools-native \
           python3-wheel-native \
           zlib-native"

PACKAGECONFIG ??= "sdl \
                   ${@bb.utils.contains('DISTRO_FEATURES', 'opengl', 'opengl', '', d)}"

PACKAGECONFIG[gtk] = "-DLIBQEMU_ENABLE_GTK=ON,-DLIBQEMU_ENABLE_GTK=OFF,gtk+3 gettext-native"
PACKAGECONFIG[opengl] = "-DLIBQEMU_ENABLE_OPENGL=ON,-DLIBQEMU_ENABLE_OPENGL=OFF,libepoxy"
PACKAGECONFIG[sdl] = "-DLIBQEMU_ENABLE_SDL=ON,-DLIBQEMU_ENABLE_SDL=OFF,libsdl2"
PACKAGECONFIG[sdl-image] = "-DLIBQEMU_ENABLE_SDL_IMAGE=ON,-DLIBQEMU_ENABLE_SDL_IMAGE=OFF,libsdl2-image"
PACKAGECONFIG[vnc] = "-DLIBQEMU_ENABLE_VNC=ON,-DLIBQEMU_ENABLE_VNC=OFF"
PACKAGECONFIG[vnc-jpeg] = "-DLIBQEMU_ENABLE_VNC_JPEG=ON,-DLIBQEMU_ENABLE_VNC_JPEG=OFF,jpeg"

EXTRA_OECMAKE += "-DQBOX_CORE_SOURCE_DIR=${HSOC_APOLLO_QBOX_SRC} \
                  -DQBOX_QEMU_SOURCE_DIR=${HSOC_APOLLO_QEMU_SRC} \
                  -DQEMU_SOURCE_DIR=${HSOC_APOLLO_QEMU_SRC} \
                  -DFETCHCONTENT_FULLY_DISCONNECTED=OFF \
                  -DFETCHCONTENT_SOURCE_DIR_QEMU=${HSOC_APOLLO_QEMU_SRC} \
                  -DFETCHCONTENT_SOURCE_DIR_LIBQEMU=${HSOC_APOLLO_QEMU_SRC} \
                  -DLIBQEMU_GIT=file://${HSOC_APOLLO_QEMU_SRC} \
                  -DLIBQEMU_BUILD_ALWAYS=OFF \
                  -DLIBQEMU_PYTHON=${PYTHON} \
                  -DGS_ENABLE_VIRCLRENDERER=OFF \
                  -DGS_ENABLE_VIRGLRENDERER=OFF \
                  -DPython3_INCLUDE_DIR=${PYTHON_INCLUDE_DIR} \
                  -DPython3_LIBRARY=${PYTHON_LIBRARY} \
                  -DQBOX_APOLLO_BUILD_TARGET=${QBOX_APOLLO_BUILD_TARGET}"

OECMAKE_TARGET_COMPILE = "${QBOX_APOLLO_BUILD_TARGET}"
do_install[noexec] = "1"
do_deploy[depends] += "firmware-apollo-qvp:do_deploy"

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
    import glob
    import json
    import shutil
    import stat
    import subprocess
    from pathlib import Path


    build_dir = Path(d.getVar("B")).resolve()
    bundle_dir = Path(d.getVar("DEPLOYDIR")).resolve() / "qbox-apollo-qvp"
    platform_src = Path(d.getVar("HSOC_APOLLO_QBOX_PLATFORM_SRC")).resolve()
    core_src = Path(d.getVar("HSOC_APOLLO_QBOX_SRC")).resolve()
    qemu_src = Path(d.getVar("HSOC_APOLLO_QEMU_SRC")).resolve()
    deploy_dir = Path(d.getVar("DEPLOY_DIR_IMAGE")).resolve()
    tmpdir = Path(d.getVar("TMPDIR")).resolve()
    machine = d.getVar("MACHINE")
    machine_arch = d.getVar("MACHINE_ARCH")
    target_work = tmpdir / "work" / f"{machine_arch}-poky-linux"
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


    def copy_runtime_libraries(patterns):
        matches = []
        for pattern in patterns:
            matches.extend(Path(path) for path in glob.glob(str(pattern)))
        for src in sorted(set(matches), key=lambda path: path.name):
            if src.is_file():
                copy_file(src, Path("lib") / src.name, "runtime-library")


    def first_glob(patterns):
        for pattern in patterns:
            matches = sorted(Path(path).resolve() for path in glob.glob(str(pattern)))
            for match in matches:
                if match.is_file():
                    return match
        return None


    def elf_arch(elf):
        if elf is None:
            return "unknown"
        try:
            proc = subprocess.run(
                ["file", str(elf)],
                check=False,
                stdout=subprocess.PIPE,
                stderr=subprocess.DEVNULL,
                text=True,
            )
        except OSError:
            return "unknown"
        output = proc.stdout
        if "ELF 32-bit" in output and "ARM" in output:
            return "arm"
        if "ELF 64-bit" in output and "aarch64" in output:
            return "aarch64"
        return "unknown"


    def elf_symbols(elf, names):
        if elf is None:
            return {}
        try:
            proc = subprocess.run(
                ["nm", "-n", "--defined-only", str(elf)],
                check=False,
                stdout=subprocess.PIPE,
                stderr=subprocess.DEVNULL,
                text=True,
            )
        except OSError:
            return {}
        if proc.returncode != 0:
            return {}
        wanted = set(names)
        found = {}
        for line in proc.stdout.splitlines():
            parts = line.split()
            if len(parts) < 3 or parts[2] not in wanted:
                continue
            try:
                found.setdefault(parts[2], int(parts[0], 16))
            except ValueError:
                continue
        return found


    def write_debug_manifest():
        debug_dir = bundle_dir / "debug"
        debug_dir.mkdir(parents=True, exist_ok=True)
        rse_target = "RD_ASD.css.smb.rseil.rse.cpu"
        ap_target = "RD_ASD.css.app00.cluster.cpu0"
        si_cl0_target = "RD_ASD.css.smb.si.cluster0.cpu0"
        si_cl1_target = "RD_ASD.css.smb.si.cluster1.cpu0"
        component_specs = [
            (
                "tfm-bl1_1",
                "TF-M BL1_1",
                "rse",
                rse_target,
                (
                    target_work / "trusted-firmware-m/*/build/bin/bl1_1.elf",
                    target_work / "trusted-firmware-m/*/build/api_ns/bin/bl1_1.elf",
                ),
                ("Reset_Handler", "_start", "main"),
            ),
            (
                "tfm-bl1_2",
                "TF-M BL1_2",
                "rse",
                rse_target,
                (
                    target_work / "trusted-firmware-m/*/build/bin/bl1_2.elf",
                    target_work / "trusted-firmware-m/*/build/api_ns/bin/bl1_2.elf",
                ),
                ("Reset_Handler", "_start", "main", "pq_crypto_verify"),
            ),
            (
                "tfm-bl2",
                "TF-M BL2",
                "rse",
                rse_target,
                (
                    target_work / "trusted-firmware-m/*/build/bin/bl2.elf",
                    target_work / "trusted-firmware-m/*/build/api_ns/bin/bl2.elf",
                ),
                (
                    "Reset_Handler",
                    "_start",
                    "main",
                    "boot_go_for_image_id",
                    "boot_load_image_to_sram",
                    "bootutil_img_validate",
                    "bootutil_img_hash",
                    "bootutil_verify_sig",
                ),
            ),
            (
                "tfm-s",
                "TF-M secure runtime",
                "rse",
                rse_target,
                (
                    target_work / "trusted-firmware-m/*/build/bin/tfm_s.elf",
                    target_work / "trusted-firmware-m/*/build/api_ns/bin/tfm_s.elf",
                ),
                ("tfm_core_init", "main", "Reset_Handler", "_start"),
            ),
            (
                "tfa-bl2",
                "TF-A BL2",
                "tf_a",
                ap_target,
                (
                    target_work / f"trusted-firmware-a/*/build/{machine_arch}/debug/bl2/bl2.elf",
                    deploy_dir / f"bl2-{machine_arch}.elf",
                ),
                ("bl2_main", "_start"),
            ),
            (
                "tfa-bl31",
                "TF-A BL31",
                "tf_a",
                ap_target,
                (
                    target_work / f"trusted-firmware-a/*/build/{machine_arch}/debug/bl31/bl31.elf",
                ),
                ("bl31_main", "_start"),
            ),
            (
                "scp-si0",
                "SCP-firmware SI0 RAMFW",
                "safety_island_cl0",
                si_cl0_target,
                (deploy_dir / "si0_ramfw.elf",),
                ("arch_exception_reset", "platform_init_hook", "fwk_arch_init"),
            ),
            (
                "si-cl1-zephyr",
                "Safety Island CL1 Zephyr demo",
                "safety_island_cl1",
                si_cl1_target,
                (deploy_dir / "zephyr-demos-cl1.elf",),
                ("z_cstart", "main"),
            ),
        ]

        components = {}
        missing = []
        for name, label, domain, target, patterns, symbols in component_specs:
            elf = first_glob(patterns)
            if elf is None:
                missing.append(name)
                continue
            resolved = elf_symbols(elf, symbols)
            selected = {
                symbol: f"0x{resolved[symbol]:x}"
                for symbol in symbols
                if symbol in resolved
            }
            components[name] = {
                "label": label,
                "domain": domain,
                "target": target,
                "elf": str(elf),
                "arch": elf_arch(elf),
                "default_symbol": next(iter(selected), None),
                "symbols": selected,
            }

        manifest = {
            "workspace": str(Path(d.getVar("TOPDIR")).resolve().parent),
            "machine": machine,
            "local_build_dir": str(bundle_dir),
            "out_dir": str(debug_dir),
            "components": components,
            "missing": missing,
        }
        manifest_path = debug_dir / "symbols.json"
        manifest_path.write_text(
            json.dumps(manifest, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        required_entries.append(
            {
                "category": "debug",
                "source_path": "generated",
                "bundle_path": str(manifest_path),
                "relative_path": str(manifest_path.relative_to(bundle_dir)),
            }
        )


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
    copy_runtime_libraries(
        (
            build_dir / "_deps/fmt-build/libfmt.so*",
            build_dir / "_deps/report-build/libreporting.so*",
            build_dir / "_deps/rpclib-build/librpc.so*",
            build_dir / "_deps/systemccci-build/configuration/src/libcci-config.so*",
            build_dir / "_deps/systemclanguage-build/src/libsystemc.so*",
        )
    )

    copy_tree(platform_src / "platforms" / "apollo", Path("platforms/apollo"), "lua-config")
    copy_tree(platform_src / "fw", Path("fw"), "lua-config", required=False)
    write_debug_manifest()

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
