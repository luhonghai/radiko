LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := audio-tools

LOCAL_SRC_FILES := OSLESMediaPlayer.c


LOCAL_CFLAGS := -DHAVE_CONFIG_H -DFPM_ARM -ffast-math -O3

LOCAL_LDLIBS    += -lOpenSLES -llog

include $(BUILD_SHARED_LIBRARY)
