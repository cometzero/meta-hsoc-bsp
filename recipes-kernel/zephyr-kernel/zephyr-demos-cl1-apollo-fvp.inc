#
# SPDX-License-Identifier: MIT
#

ZEPHYR_BOARD:apollo-fvp = "apollo_fvp_safety_island_c1"

ZEPHYR_DEMOS_CL1_APOLLO_GIT_DEPS = "cmsis libmetal open-amp"
ZEPHYR_DEMOS_CL1_APOLLO_MODULE_SUFFIXES = "\
    modules/hal/cmsis \
    modules/hal/libmetal \
    modules/lib/open-amp \
"

python () {
    required_git_deps = set((d.getVar("ZEPHYR_DEMOS_CL1_APOLLO_GIT_DEPS") or "").split())
    src_uri = []
    for uri in (d.getVar("SRC_URI") or "").split():
        is_git_dependency = (
            uri.startswith(("git://", "gitsm://")) and
            "destsuffix=git/" in uri and
            "type=" not in uri
        )
        if uri.startswith(("git://", "gitsm://")) and "destsuffix=git/" in uri:
            name = "default"
            for parm in uri.split(";"):
                if parm.startswith("name="):
                    name = parm.split("=", 1)[1]
                    break
            if name not in required_git_deps:
                continue
        if is_git_dependency:
            uri = f"{uri};type=git-dependency"
        src_uri.append(uri)
    d.setVar("SRC_URI", " ".join(src_uri))
}

inherit externalsrc

EXTERNALSRC:pn-zephyr-demos-cl1:apollo-fvp = "${HSOC_APOLLO_ZEPHYRPROJECT_SRC}"
EXTERNALSRC_BUILD:pn-zephyr-demos-cl1:apollo-fvp = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS:pn-zephyr-demos-cl1:apollo-fvp = ""

ZEPHYR_SRC_DIR:apollo-fvp = "${ZEPHYR_SAFETY_ISLAND_MODULE}/apps/sample"
HSOC_APOLLO_ZEPHYR_HSOC_MODULE:apollo-fvp = "${HSOC_APOLLO_ZEPHYR_HSOC_SRC}"

OVERLAY_HIPC:apollo-fvp = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}/overlays/hipc/${BOARD}"
OVERLAY_PFDI:apollo-fvp = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}/overlays/pfdi/${BOARD}"
OVERLAY_PFDI_AGENT:apollo-fvp = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}/overlays/pfdi_agent/${BOARD}"
OVERLAY_ZPERF:apollo-fvp = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}/overlays/zperf/${BOARD}"

ZEPHYR_MODULES:append:apollo-fvp = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}\;"

python () {
    import os

    source_root = d.getVar("S")
    unpack_root = os.path.join(d.getVar("UNPACKDIR"), "git")
    safety_island = d.getVar("ZEPHYR_SAFETY_ISLAND_MODULE")
    hsoc_module = d.getVar("HSOC_APOLLO_ZEPHYR_HSOC_MODULE")
    required_suffixes = tuple(
        os.path.join(unpack_root, suffix)
        for suffix in (d.getVar("ZEPHYR_DEMOS_CL1_APOLLO_MODULE_SUFFIXES") or "").split()
    )
    required_modules = required_suffixes + (safety_island, hsoc_module)
    modules = d.getVar("ZEPHYR_MODULES") or ""
    modules = modules.replace(f"{source_root}/", f"{unpack_root}/")
    modules = modules.replace(os.path.join(unpack_root, "safety_island"), safety_island)
    if hsoc_module:
        modules = f"{modules}\\;{hsoc_module}"

    filtered_modules = []
    seen_modules = set()
    for module in modules.split("\\;"):
        module = module.strip()
        if not module:
            continue
        if module in required_modules and module not in seen_modules:
            filtered_modules.append(module)
            seen_modules.add(module)

    d.setVar("ZEPHYR_MODULES", "\\;".join(filtered_modules) + "\\;")
}

python () {
    checksums = " ${HSOC_APOLLO_ZEPHYR_SRC}:True ${ZEPHYR_SAFETY_ISLAND_MODULE}:True ${HSOC_APOLLO_ZEPHYR_HSOC_SRC}:True"
    d.appendVarFlag("do_configure", "file-checksums", checksums)
    d.appendVarFlag("do_compile", "file-checksums", checksums)
}
