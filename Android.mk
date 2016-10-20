LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/src/main/res
LOCAL_MANIFEST_FILE := src/main/AndroidManifest.xml

LOCAL_MODULE_TAGS := tests

LOCAL_SRC_FILES := $(call all-java-files-under, src/main/java)
LOCAL_STATIC_JAVA_LIBRARIES := \
		android-support-v4 \
		json-smart \
		systembartint \

LOCAL_PACKAGE_NAME := Synapse

#LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

#LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)

##################################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
		json-smart:libs/json-smart-1.2.jar \
		systembartint:libs/systembartint-1.0.3.jar \

include $(BUILD_MULTI_PREBUILT)
