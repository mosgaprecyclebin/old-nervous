LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := tracepath
LOCAL_SRC_FILES := tracepath-jni.c tracepath.c

include $(BUILD_SHARED_LIBRARY)
