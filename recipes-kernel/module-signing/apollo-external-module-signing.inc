#
# SPDX-License-Identifier: MIT
#

MODSIGN_ENABLED = "${@bb.utils.contains('DISTRO_FEATURES', 'modsign', '1', '0', d)}"

# Kbuild signs external modules during modules_install. Stripping after that
# invalidates the signature when CONFIG_MODULE_SIG_FORCE=y.
INHIBIT_PACKAGE_STRIP:apollo-fvp = "${@'1' if d.getVar('MODSIGN_ENABLED') == '1' else '0'}"
