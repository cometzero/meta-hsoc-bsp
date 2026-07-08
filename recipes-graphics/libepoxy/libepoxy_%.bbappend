# qbox-libqemu-native enables QEMU OpenGL for a host-side native tool.
# Keep the feature gate local to native builds instead of adding opengl to
# target DISTRO_FEATURES.
DISTRO_FEATURES:append:class-native = " opengl"
PACKAGECONFIG:class-native = "egl"
