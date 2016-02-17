# Build both ARMv5TE and ARMv7-A machine code.
APP_ABI := armeabi armeabi-v7a arm64-v8a x86 x86_64 mips mips64

APP_MODULES = audio-flac audio-native
APP_OPTIM = release
APP_PLATFORM = android-19  # mkfifo isn't backwards compatible in 21+

AUDIO_NATIVE_FLAGS = \
	-Ijni/config \
	-Ijni/ogg/include \
	-DVERSION=\"1.3\" \
	-Ijni/flac/include \
	-Ijni/flac/src/libFLAC/include

APP_CFLAGS += $(AUDIO_NATIVE_FLAGS)
APP_CXXFLAGS += $(AUDIO_NATIVE_FLAGS)
