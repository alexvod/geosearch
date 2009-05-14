#define LOG_TAG "FastIndexOf"
#include "utils/Log.h"

#include "JNIHelp.h"
#include "jni.h"

/* Header for class FastIndexOf */

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

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     org.alexvod.FastIndexOf
 * Method:    fastIndexOf
 * Signature: ([B[BI)I
 */
JNIEXPORT jint JNICALL Java_org_alexvod_FastIndexOf_fastIndexOf_impl(
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

#ifdef __cplusplus
}
#endif

/*
 * JNI registration
 */
static JNINativeMethod gMethods[] = {
  /* name, signature, funcPtr */
  { "fastIndexOf", "([B[BI)I", (void*)Java_org_alexvod_FastIndexOf_fastIndexOf_impl },
};

static int register_org_alexvod_FastIndexOf(JNIEnv* env)
{
  jclass clazz;

  clazz = env->FindClass("org/alexvod/FastIndexOf");
  if (clazz == NULL) {
    fprintf(stderr, "Native registration unable to find class");
    return -1;
  }
  
  return env->RegisterNatives(clazz, gMethods, 1);
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

  register_org_alexvod_FastIndexOf(env);

  return JNI_VERSION_1_4;
}
