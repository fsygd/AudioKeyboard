LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
         $(call all-subdir-java-files) \
         com/example/fansy/audiokeyboard/IPinyinDecoderService.aidl

LOCAL_MODULE := com.example.fansy.audiokeyboard.lib

include $(BUILD_STATIC_JAVA_LIBRARY)
