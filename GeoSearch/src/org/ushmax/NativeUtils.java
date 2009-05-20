package org.ushmax;

import android.util.Log;

public class NativeUtils {
    static {
        try {
            Log.i("JNI", "Trying to load libnativeutils_jni.so");
            System.load("/data/data/org.alexvod.geosearch/libnativeutils_jni.so");
            Log.i("JNI", "Successfully loaded!");
            nativeLibraryAvailable = true;
        }
        catch (UnsatisfiedLinkError ule) {
            Log.e("JNI", "WARNING: Could not load libnativeutils_jni.so");
            nativeLibraryAvailable = false;
        }
    }

    private static native int nativeIndexOf(byte[] str, byte[] substr, int start);
    private static native void nativeReadIntArray(byte[] buffer, int offset, int[] dst, int size);
    private static native int nativeMakeSampledPosVector(byte[] content, int[] dst, byte separator, int sample);

    public static boolean nativeLibraryAvailable;

    public static int indexOf(byte[] str, byte[] substr, int start) {
      if (str == null) throw new NullPointerException();
      if (substr == null) throw new NullPointerException();
      if (start < 0) throw new ArrayIndexOutOfBoundsException();
      return nativeIndexOf(str, substr, start);
    }

    public static void readIntArray(byte[] buffer, int offset, int[] dst, int size) {
      if (buffer == null) throw new NullPointerException();
      if (dst == null) throw new NullPointerException();
      if (size > dst.length) throw new ArrayIndexOutOfBoundsException();
      if (offset < 0) throw new ArrayIndexOutOfBoundsException();
      if (offset + (size << 2) > buffer.length) throw new ArrayIndexOutOfBoundsException();
      nativeReadIntArray(buffer, offset, dst, size);
    }
    
    public static void makeSampledPosVector(byte[] content, int[] dst, byte separator, int sample) {
      if (content == null) throw new NullPointerException();
      if (dst == null) throw new NullPointerException();
      if ((sample < 0) || (sample >= 30)) throw new IllegalArgumentException();
      int result = nativeMakeSampledPosVector(content, dst, separator, sample);
      if (result != 0) throw new ArrayIndexOutOfBoundsException();
    }
}
