LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
NativeUtils.cpp

LOCAL_C_INCLUDES += \
$(JNI_H_INCLUDE)

LOCAL_SHARED_LIBRARIES := \
    libcutils \

LOCAL_PRELINK_MODULE := false

LOCAL_MODULE:= libnativeutils_jni

include $(BUILD_SHARED_LIBRARY)
