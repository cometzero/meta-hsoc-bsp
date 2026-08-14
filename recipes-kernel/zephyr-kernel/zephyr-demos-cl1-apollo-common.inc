#
# SPDX-License-Identifier: MIT
#

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:apollo-qvp = " file://pfdi-tx-irq"

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

EXTERNALSRC:pn-zephyr-demos-cl1 = "${HSOC_APOLLO_ZEPHYRPROJECT_SRC}"
EXTERNALSRC_BUILD:pn-zephyr-demos-cl1 = "${WORKDIR}/build"
EXTERNALSRC_SYMLINKS:pn-zephyr-demos-cl1 = ""

ZEPHYR_SRC_DIR = "${ZEPHYR_SAFETY_ISLAND_MODULE}/apps/sample"
HSOC_APOLLO_ZEPHYR_HSOC_MODULE = "${HSOC_APOLLO_ZEPHYR_HSOC_SRC}"
PFDI_TX_IRQ_MODULE = "${UNPACKDIR}/pfdi-tx-irq"

OVERLAY_HIPC = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}/overlays/hipc/${BOARD}"
OVERLAY_PFDI = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}/overlays/pfdi/${BOARD}"
OVERLAY_PFDI_AGENT = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}/overlays/pfdi_agent/${BOARD}"
OVERLAY_ZPERF = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}/overlays/zperf/${BOARD}"

ZEPHYR_MODULES:append = "${HSOC_APOLLO_ZEPHYR_HSOC_MODULE}\;"
ZEPHYR_MODULES:append:apollo-qvp = "${PFDI_TX_IRQ_MODULE}\;"

python () {
    import os

    source_root = d.getVar("S")
    unpack_root = os.path.join(d.getVar("UNPACKDIR"), "git")
    safety_island = d.getVar("ZEPHYR_SAFETY_ISLAND_MODULE")
    hsoc_module = d.getVar("HSOC_APOLLO_ZEPHYR_HSOC_MODULE")
    pfdi_tx_irq_module = d.getVar("PFDI_TX_IRQ_MODULE")
    module_separator = r"\;"
    required_suffixes = tuple(
        os.path.join(unpack_root, suffix)
        for suffix in (d.getVar("ZEPHYR_DEMOS_CL1_APOLLO_MODULE_SUFFIXES") or "").split()
    )
    required_modules = required_suffixes + (
        safety_island,
        hsoc_module,
        pfdi_tx_irq_module,
    )
    modules = d.getVar("ZEPHYR_MODULES") or ""
    modules = modules.replace(f"{source_root}/", f"{unpack_root}/")
    modules = modules.replace(os.path.join(unpack_root, "safety_island"), safety_island)
    if hsoc_module:
        modules = f"{modules}{module_separator}{hsoc_module}"

    filtered_modules = []
    seen_modules = set()
    for module in modules.split(module_separator):
        module = module.strip()
        if not module:
            continue
        if module in required_modules and module not in seen_modules:
            filtered_modules.append(module)
            seen_modules.add(module)

    d.setVar("ZEPHYR_MODULES", module_separator.join(filtered_modules) + module_separator)
}

python () {
    checksums = " ${HSOC_APOLLO_ZEPHYR_SRC}:True ${ZEPHYR_SAFETY_ISLAND_MODULE}:True ${HSOC_APOLLO_ZEPHYR_HSOC_SRC}:True"
    d.appendVarFlag("do_configure", "file-checksums", checksums)
    d.appendVarFlag("do_compile", "file-checksums", checksums)
}
