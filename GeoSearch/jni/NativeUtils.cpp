#include <android/log.h>
#include <jni.h>
#include <stdlib.h>

#define LOG_TAG "NativeUtils"
#define LOGE(...) (__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

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

static void readIntArrayLE(jbyte* buffer, int offset, jint* dst, int size) {
  for (int i = 0; i < size; i++) {
    int b1 = buffer[offset] & 0xff; offset++;
    int b2 = buffer[offset] & 0xff; offset++;
    int b3 = buffer[offset] & 0xff; offset++;
    int b4 = buffer[offset] & 0xff; offset++;
    dst[i] = ((b4 << 24) + (b3 << 16) + (b2 << 8) + b1);
  }
}

static void writeIntArrayBE(jint* src, int start, int count, jbyte* dst, int offset) {
  for (int i = 0; i < count; i++) {
    int word = src[start + i];
    jbyte b4 = (jbyte) (word & 0xff); word >>= 8;
    jbyte b3 = (jbyte) (word & 0xff); word >>= 8;
    jbyte b2 = (jbyte) (word & 0xff); word >>= 8;
    jbyte b1 = (jbyte) (word & 0xff);
    dst[offset++] = b1;
    dst[offset++] = b2;
    dst[offset++] = b3;
    dst[offset++] = b4;
  }
}

static void writeCharArrayBE(jchar* src, int start, int count, jbyte* dst, int offset) {
  for (int i = 0; i < count; i++) {
    int ch = src[start + i];
    jbyte b2 = (jbyte) (ch & 0xff); ch >>= 8;
    jbyte b1 = (jbyte) (ch & 0xff);
    dst[offset++] = b1;
    dst[offset++] = b2;
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
    unsigned int b1 = (unsigned char)buffer[offset]; offset++;
    unsigned int b2 = (unsigned char)buffer[offset]; offset++;
    dst[i] = ((b1 << 8) + b2);
  }
}

static void readCharArrayLE(jbyte* buffer, int offset, jchar* dst, int size) {
  for (int i = 0; i < size; i++) {
    int b1 = buffer[offset]; offset++;
    int b2 = buffer[offset]; offset++;
    dst[i] = ((b2 << 8) + b1);
  }
}

static jbyte getMinByteArray(jbyte* buffer, int size) {
  if (size == 0) return 0;
  jbyte minval = buffer[0];
  jbyte* ptr = buffer;
  jbyte* end = buffer + size;
  while (ptr < end) {
    jbyte value = *ptr;
    if (value < minval) {
      minval = value;
    }
    ptr++;
  }
  return minval;
}

static jbyte getMaxByteArray(jbyte* buffer, int size) {
  if (size == 0) return 0;
  jbyte maxval = buffer[0];
  jbyte* ptr = buffer;
  jbyte* end = buffer + size;
  while (ptr < end) {
    jbyte value = *ptr;
    if (value > maxval) {
      maxval = value;
    }
    ptr++;
  }
  return maxval;
}

extern "C" {

/*
 * Class:     org.nativeutils.NativeUtils
 * Method:    nativeIndexOf
 * Signature: ([B[BI)I
 */
JNIEXPORT jint JNICALL Java_org_nativeutils_NativeUtils_nativeIndexOf(
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
 * Class:     org.nativeutils.NativeUtils
 * Method:    nativeReadIntArrayBE
 * Signature: ([BI[II)V
 */
JNIEXPORT void JNICALL Java_org_nativeutils_NativeUtils_nativeReadIntArrayBE(
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
 * Class:     org.nativeutils.NativeUtils
 * Method:    nativeReadIntArrayLE
 * Signature: ([BI[II)V
 */
JNIEXPORT void JNICALL Java_org_nativeutils_NativeUtils_nativeReadIntArrayLE(
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

  readIntArrayLE(buf_ptr, offset, dst_ptr, size);

  env->ReleasePrimitiveArrayCritical(buffer, buf_ptr, JNI_ABORT);
  env->ReleasePrimitiveArrayCritical(dst, dst_ptr, 0);
}

/*
 * Class:     org.nativeutils.NativeUtils
 * Method:    nativeWriteIntArrayBE
 * Signature: ([III[BI)V
 */
JNIEXPORT void JNICALL Java_org_nativeutils_NativeUtils_nativeWriteIntArrayBE(
    JNIEnv *env, jclass clazz , jintArray src, jint start, jint count,
    jbyteArray dst, jint offset) {
  jint *src_ptr = (jint*)env->GetPrimitiveArrayCritical(src, 0);
  if (src_ptr == 0) {
    LOGE("Cannot get src array");
    return;
  }
  
  jbyte *dst_ptr = (jbyte*)env->GetPrimitiveArrayCritical(dst, 0);
  if (dst_ptr == 0) {
    LOGE("Cannot get dst array");
    env->ReleasePrimitiveArrayCritical(src, src_ptr, JNI_ABORT);
    return;
  }

  writeIntArrayBE(src_ptr, start, count, dst_ptr, offset);

  env->ReleasePrimitiveArrayCritical(src, src_ptr, JNI_ABORT);
  env->ReleasePrimitiveArrayCritical(dst, dst_ptr, 0);
}

/*
 * Class:     org.nativeutils.NativeUtils
 * Method:    nativeWriteCharArrayBE
 * Signature: ([CII[BI)V
 */
JNIEXPORT void JNICALL Java_org_nativeutils_NativeUtils_nativeWriteCharArrayBE(
    JNIEnv *env, jclass clazz , jcharArray src, jint start, jint count,
    jbyteArray dst, jint offset) {
  jchar *src_ptr = (jchar*)env->GetPrimitiveArrayCritical(src, 0);
  if (src_ptr == 0) {
    LOGE("Cannot get src array");
    return;
  }
  
  jbyte *dst_ptr = (jbyte*)env->GetPrimitiveArrayCritical(dst, 0);
  if (dst_ptr == 0) {
    LOGE("Cannot get dst array");
    env->ReleasePrimitiveArrayCritical(src, src_ptr, JNI_ABORT);
    return;
  }

  writeCharArrayBE(src_ptr, start, count, dst_ptr, offset);

  env->ReleasePrimitiveArrayCritical(src, src_ptr, JNI_ABORT);
  env->ReleasePrimitiveArrayCritical(dst, dst_ptr, 0);
}

/*
 * Class:     org.nativeutils.NativeUtils
 * Method:    makeSampledPosVector
 * Signature: ([B[IBI)I
 */

JNIEXPORT jint JNICALL Java_org_nativeutils_NativeUtils_makeSampledPosVector(
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
 * Class:     org.nativeutils.NativeUtils
 * Method:    getMaxIntVectorDelta
 * Signature: ([I)I
 */

JNIEXPORT jint JNICALL Java_org_nativeutils_NativeUtils_getMaxIntVectorDelta(
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
 * Class:     org.nativeutils.NativeUtils
 * Method:    nativeReadCharArrayBE
 * Signature: ([BI[CI)V
 */
JNIEXPORT void JNICALL Java_org_nativeutils_NativeUtils_nativeReadCharArrayBE(
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

/*
 * Class:     org.nativeutils.NativeUtils
 * Method:    nativeReadCharArrayLE
 * Signature: ([BI[CI)V
 */
JNIEXPORT void JNICALL Java_org_nativeutils_NativeUtils_nativeReadCharArrayLE(
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

  readCharArrayLE(buf_ptr, offset, dst_ptr, size);

  env->ReleasePrimitiveArrayCritical(buffer, buf_ptr, JNI_ABORT);
  env->ReleasePrimitiveArrayCritical(dst, dst_ptr, 0);
}

/*
 * Class:     org.nativeutils.NativeUtils
 * Method:    getMinByteArray
 * Signature: ([B)B
 */
JNIEXPORT jbyte JNICALL Java_org_nativeutils_NativeUtils_nativeGetMinByteArray(
    JNIEnv *env, jclass clazz , jbyteArray data) {
  int data_len = env->GetArrayLength(data);
  
  jbyte *data_ptr = (jbyte*)env->GetPrimitiveArrayCritical(data, 0);
  if (data_ptr == 0) {
    LOGE("Cannot get data array");
    return -1;
  }
  
  jbyte result = getMinByteArray(data_ptr, data_len);

  env->ReleasePrimitiveArrayCritical(data, data_ptr, JNI_ABORT);

  return result;
}

/*
 * Class:     org.nativeutils.NativeUtils
 * Method:    getMaxByteArray
 * Signature: ([B)B
 */
JNIEXPORT jbyte JNICALL Java_org_nativeutils_NativeUtils_nativeGetMaxByteArray(
    JNIEnv *env, jclass clazz , jbyteArray data) {
  int data_len = env->GetArrayLength(data);
  
  jbyte *data_ptr = (jbyte*)env->GetPrimitiveArrayCritical(data, 0);
  if (data_ptr == 0) {
    LOGE("Cannot get data array");
    return -1;
  }
  
  jbyte result = getMaxByteArray(data_ptr, data_len);

  env->ReleasePrimitiveArrayCritical(data, data_ptr, JNI_ABORT);

  return result;
}

}

/*
 * JNI registration
 */
static JNINativeMethod gMethods[] = {
  /* name, signature, funcPtr */
  { "nativeIndexOf", "([B[BI)I",
    (void*)Java_org_nativeutils_NativeUtils_nativeIndexOf },
  { "nativeReadIntArrayBE", "([BI[II)V",
    (void*)Java_org_nativeutils_NativeUtils_nativeReadIntArrayBE },
  { "nativeReadIntArrayLE", "([BI[II)V",
    (void*)Java_org_nativeutils_NativeUtils_nativeReadIntArrayLE },
  { "nativeWriteIntArrayBE", "([III[BI)V",
    (void*)Java_org_nativeutils_NativeUtils_nativeWriteIntArrayBE },
  { "nativeMakeSampledPosVector", "([B[IBI)I",
    (void*)Java_org_nativeutils_NativeUtils_makeSampledPosVector },
  { "nativeGetMaxIntVectorDelta", "([I)I",
    (void*)Java_org_nativeutils_NativeUtils_getMaxIntVectorDelta },
  { "nativeReadCharArrayBE", "([BI[CI)V",
    (void*)Java_org_nativeutils_NativeUtils_nativeReadCharArrayBE },
  { "nativeReadCharArrayLE", "([BI[CI)V",
    (void*)Java_org_nativeutils_NativeUtils_nativeReadCharArrayLE },
  { "nativeWriteCharArrayBE", "([CII[BI)V",
    (void*)Java_org_nativeutils_NativeUtils_nativeWriteCharArrayBE },
  { "nativeGetMinByteArray", "([B)B",
    (void*)Java_org_nativeutils_NativeUtils_nativeGetMinByteArray },
  { "nativeGetMaxByteArray", "([B)B",
    (void*)Java_org_nativeutils_NativeUtils_nativeGetMaxByteArray },
};

static int register_org_nativeutils_NativeUtils(JNIEnv* env)
{
  jclass clazz;

  clazz = env->FindClass("org/nativeutils/NativeUtils");
  if (clazz == NULL) {
    LOGE("Native registration unable to find class");
    return -1;
  }
  
  return env->RegisterNatives(clazz, gMethods, 11);
}

extern "C" jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
  JNIEnv* env = NULL;
  jint result = -1;

  if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
    LOGE("GetEnv failed!");
    return result;
  }
  if (!env) {
    LOGE("Could not retrieve the env!");
    return result;
  }

  register_org_nativeutils_NativeUtils(env);

  return JNI_VERSION_1_4;
}
