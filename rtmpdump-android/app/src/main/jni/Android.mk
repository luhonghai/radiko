LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := cryptox
LOCAL_SRC_FILES := lib/libcryptox.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := sslx
LOCAL_SRC_FILES := lib/libsslx.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := rtmp

LOCAL_SHARED_LIBRARIES := sslx cryptox
PATH_RTMP := rtmpdump/librtmp

LIB_OPENSSL=-lssl -lcrypto

LOCAL_CFLAGS := -DRTMPDUMP_VERSION=v2.4 -DCRYPTO=OPENSSL
LOCAL_LDLIBS := -llog -lz
LOCAL_SRC_FILES := log.c \
thread.c \
$(PATH_RTMP)/rtmp.c \
$(PATH_RTMP)/amf.c \
$(PATH_RTMP)/hashswf.c \
$(PATH_RTMP)/parseurl.c
					
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/rtmpdump
include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := rtmpdump
LOCAL_SRC_FILES := rtmpdump-jni.c
LOCAL_SHARED_LIBRARIES := rtmp
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_CFLAGS := -DRTMPDUMP_VERSION=v2.4 -DCRYPTO=OPENSSL
LOCAL_MODULE := rtmpsuck
LOCAL_SRC_FILES := rtmpsuck-jni.c
LOCAL_SHARED_LIBRARIES := rtmp
include $(BUILD_SHARED_LIBRARY)