#define LOG_TAG "NativeUtils"
#include "utils/Log.h"

#include "JNIHelp.h"
#include "jni.h"

static int searchSubstringForward(jbyte *str, int str_len, jbyte *substr, int substr_len, int start) {
  jbyte first_byte = substr[0];
  int len = str_len - substr_len;
  // Search for the first byte of substr.
  for (int i = start; i <= len; ++i) {
    if (str[i] != first_byte) continue;
    int i1 = i, i2 = 0;
    while (++i2 < substr_len && str[++i1] == substr[i2]) {
      // Intentionally empty
    }
    if (i2 == substr_len) {
      return i;
    }
  }
  return -1;
}

static void readIntArrayBE(jbyte* buffer, int offset, jint* dst, int size) {
  for (int i = 0; i < size; i++) {
    int b1 = buffer[offset] & 0xff; offset++;
    int b2 = buffer[offset] & 0xff; offset++;
    int b3 = buffer[offset] & 0xff; offset++;
    int b4 = buffer[offset] & 0xff; offset++;
    dst[i] = ((b1 << 24) + (b2 << 16) + (b3 << 8) + b4);
  }
}

static jint makeSampledPosVector(jbyte* content, int size, jint* dst,
                                 int dst_size, jbyte separator,
                                 int sample) {
  if (dst_size == 0) return 0;
  dst[0] = 0;
  if (dst_size == 1) return 0;
  int idx = 1;
  int sampling_mask = (1 << sample) - 1;
  for (int i = 1; i < size; i++) {
    if (content[i] != separator) continue;
    if ((idx & sampling_mask) == 0) {
      int store_idx = idx >> sample;
      if (store_idx >= dst_size) return -1;
      dst[store_idx] = i + 1;
    }
    idx++;
  }
  return 0;
}

static jint getMaxIntVectorDelta(jint* data, int size) {
  if (size < 1) return 0;
  int max_delta = data[1] - data[0];
  for (int i = 1; i < size; i++) {
    int delta = data[i] - data[i-1];
    if (delta > max_delta) {
      max_delta = delta;
    }
  }
  return max_delta;
}

static void readCharArrayBE(jbyte* buffer, int offset, jchar* dst, int size) {
  for (int i = 0; i < size; i++) {
    int b1 = buffer[offset]; offset++;
    int b2 = buffer[offset]; offset++;
    dst[i] = ((b1 << 8) + b2);
  }
}

extern "C" {

/*
 * Class:     org.ushmax.NativeUtils
 * Method:    nativeIndexOf
 * Signature: ([B[BI)I
 */
JNIEXPORT jint JNICALL Java_org_ushmax_NativeUtils_nativeIndexOf(
   JNIEnv *env, jclass clazz , jbyteArray str, jbyteArray substr, jint start) {
  jboolean is_copy;

  int str_len = env->GetArrayLength(str);
  int substr_len = env->GetArrayLength(substr);

  jbyte *str_ptr = (jbyte*)env->GetPrimitiveArrayCritical(str, &is_copy);
  if (str_ptr == 0) {
    LOGE("Cannot get str array");
    return -1;
  }
  
  jbyte *substr_ptr = (jbyte*)env->GetPrimitiveArrayCritical(substr, 0);
  if (substr_ptr == 0) {
    LOGE("Cannot get substr array");
    env->ReleasePrimitiveArrayCritical(str, str_ptr, JNI_ABORT);
    return -1;
  }

  int result = searchSubstringForward(str_ptr, str_len, substr_ptr, substr_len, start);

  env->ReleasePrimitiveArrayCritical(substr, substr_ptr, JNI_ABORT);
  env->ReleasePrimitiveArrayCritical(str, str_ptr, JNI_ABORT);

  return result;
}

/*
 * Class:     org.ushmax.NativeUtils
 * Method:    nativeReadIntArrayBE
 * Signature: ([BI[II)V
 */
JNIEXPORT void JNICALL Java_org_ushmax_NativeUtils_nativeReadIntArrayBE(
    JNIEnv *env, jclass clazz , jbyteArray buffer, jint offset,
    jintArray dst, jint size) {
  jbyte *buf_ptr = (jbyte*)env->GetPrimitiveArrayCritical(buffer, 0);
  if (buf_ptr == 0) {
    LOGE("Cannot get buf array");
    return;
  }
  
  jint *dst_ptr = (jint*)env->GetPrimitiveArrayCritical(dst, 0);
  if (dst_ptr == 0) {
    LOGE("Cannot get dst array");
    env->ReleasePrimitiveArrayCritical(buffer, buf_ptr, JNI_ABORT);
    return;
  }

  readIntArrayBE(buf_ptr, offset, dst_ptr, size);

  env->ReleasePrimitiveArrayCritical(buffer, buf_ptr, JNI_ABORT);
  env->ReleasePrimitiveArrayCritical(dst, dst_ptr, 0);
}

/*
 * Class:     org.ushmax.NativeUtils
 * Method:    makeSampledPosVector
 * Signature: ([B[IBI)I
 */

JNIEXPORT jint JNICALL Java_org_ushmax_NativeUtils_makeSampledPosVector(
    JNIEnv *env, jclass clazz, jbyteArray content, jintArray dst,
    jbyte separator, jint sample) {

  int content_len = env->GetArrayLength(content);
  int dst_len = env->GetArrayLength(dst);
  
  jbyte *content_ptr = (jbyte*)env->GetPrimitiveArrayCritical(content, 0);
  if (content_ptr == 0) {
    LOGE("Cannot get content array");
    return -1;
  }
  
  jint *dst_ptr = (jint*)env->GetPrimitiveArrayCritical(dst, 0);
  if (dst_ptr == 0) {
    LOGE("Cannot get dst array");
    env->ReleasePrimitiveArrayCritical(content, content_ptr, JNI_ABORT);
    return -1;
  }

  jint result = makeSampledPosVector(content_ptr, content_len, dst_ptr,
                                     dst_len, separator, sample);

  env->ReleasePrimitiveArrayCritical(content, content_ptr, JNI_ABORT);
  env->ReleasePrimitiveArrayCritical(dst, dst_ptr, 0);

  return result;
}

/*
 * Class:     org.ushmax.NativeUtils
 * Method:    getMaxIntVectorDelta
 * Signature: ([I)I
 */

JNIEXPORT jint JNICALL Java_org_ushmax_NativeUtils_getMaxIntVectorDelta(
    JNIEnv *env, jclass clazz, jintArray data) {

  int data_len = env->GetArrayLength(data);
  
  jint *data_ptr = (jint*)env->GetPrimitiveArrayCritical(data, 0);
  if (data_ptr == 0) {
    LOGE("Cannot get data array");
    return -1;
  }
  
  jint result = getMaxIntVectorDelta(data_ptr, data_len);

  env->ReleasePrimitiveArrayCritical(data, data_ptr, JNI_ABORT);

  return result;
}

/*
 * Class:     org.ushmax.NativeUtils
 * Method:    nativeReadCharArrayBE
 * Signature: ([BI[CI)V
 */
JNIEXPORT void JNICALL Java_org_ushmax_NativeUtils_nativeReadCharArrayBE(
    JNIEnv *env, jclass clazz , jbyteArray buffer, jint offset,
    jcharArray dst, jint size) {
  jbyte *buf_ptr = (jbyte*)env->GetPrimitiveArrayCritical(buffer, 0);
  if (buf_ptr == 0) {
    LOGE("Cannot get buf array");
    return;
  }
  
  jchar *dst_ptr = (jchar*)env->GetPrimitiveArrayCritical(dst, 0);
  if (dst_ptr == 0) {
    LOGE("Cannot get dst array");
    env->ReleasePrimitiveArrayCritical(buffer, buf_ptr, JNI_ABORT);
    return;
  }

  readCharArrayBE(buf_ptr, offset, dst_ptr, size);

  env->ReleasePrimitiveArrayCritical(buffer, buf_ptr, JNI_ABORT);
  env->ReleasePrimitiveArrayCritical(dst, dst_ptr, 0);
}

}

/*
 * JNI registration
 */
static JNINativeMethod gMethods[] = {
  /* name, signature, funcPtr */
  { "nativeIndexOf", "([B[BI)I",
    (void*)Java_org_ushmax_NativeUtils_nativeIndexOf },
  { "nativeReadIntArrayBE", "([BI[II)V",
    (void*)Java_org_ushmax_NativeUtils_nativeReadIntArrayBE },
  { "nativeMakeSampledPosVector", "([B[IBI)I",
    (void*)Java_org_ushmax_NativeUtils_makeSampledPosVector },
  { "nativeGetMaxIntVectorDelta", "([I)I",
    (void*)Java_org_ushmax_NativeUtils_getMaxIntVectorDelta },
  { "nativeReadCharArrayBE", "([BI[CI)V",
    (void*)Java_org_ushmax_NativeUtils_nativeReadCharArrayBE },
};

static int register_org_ushmax_NativeUtils(JNIEnv* env)
{
  jclass clazz;

  clazz = env->FindClass("org/ushmax/NativeUtils");
  if (clazz == NULL) {
    fprintf(stderr, "Native registration unable to find class");
    return -1;
  }
  
  return env->RegisterNatives(clazz, gMethods, 5);
}

extern "C" jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
  JNIEnv* env = NULL;
  jint result = -1;

  if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
    LOGE("GetEnv failed!");
    return result;
  }
  LOG_ASSERT(env, "Could not retrieve the env!");

  register_org_ushmax_NativeUtils(env);

  return JNI_VERSION_1_4;
}
