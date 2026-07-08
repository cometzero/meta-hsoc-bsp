# Provide virtual/egl-native for qbox-libqemu-native OpenGL support without
# enabling opengl in target DISTRO_FEATURES.
DISTRO_FEATURES:append:class-native = " opengl"
PACKAGECONFIG:class-native = "opengl egl gallium"
PACKAGECONFIG:remove:class-native = "gallium-llvm r600"
