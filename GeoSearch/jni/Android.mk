LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= NativeUtils.cpp
LOCAL_MODULE:= nativeutils
LOCAL_LDLIBS:= -L$(SYSROOT)/usr/lib -llog 

include $(BUILD_SHARED_LIBRARY)
