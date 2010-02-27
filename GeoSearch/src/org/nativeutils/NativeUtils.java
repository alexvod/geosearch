package org.nativeutils;

import android.util.Log;

public class NativeUtils {
  private static final String LOGTAG = "NativeUtils";
  // Update these constants when library is updated.
  
  static {
    loadNativeLibrary();
  }

  private static void loadNativeLibrary() {
    if (nativeLibraryAvailable) return;
    String libName = "nativeutils";
    try {
      Log.i(LOGTAG, "Trying to load " + libName);
      System.loadLibrary(libName);
      Log.i(LOGTAG, "Successfully loaded!");
      nativeLibraryAvailable = true;
    }
    catch (UnsatisfiedLinkError ule) {
      Log.e(LOGTAG, "WARNING: Could not load " + libName);
      nativeLibraryAvailable = false;
    }    
  }

  private static native int nativeIndexOf(byte[] str, byte[] substr, int start);
  private static native void nativeReadIntArrayBE(byte[] buffer, int offset, int[] dst, int size);
  private static native void nativeReadIntArrayLE(byte[] buffer, int offset, int[] dst, int size);
  private static native void nativeWriteIntArrayBE(int[] src, int start, int count, byte[] dst, int offset);
  private static native int nativeMakeSampledPosVector(byte[] content, int[] dst, byte separator, int sample);
  private static native int nativeGetMaxIntVectorDelta(int[] data);
  private static native void nativeReadCharArrayBE(byte[] buffer, int offset, char[] dst, int size);
  private static native void nativeReadCharArrayLE(byte[] buffer, int offset, char[] dst, int size);
  private static native void nativeWriteCharArrayBE(char[] src, int start, int count, byte[] dst, int offset);
  private static native byte nativeGetMinByteArray(byte[] data);
  private static native byte nativeGetMaxByteArray(byte[] data);

  public static boolean nativeLibraryAvailable;

  public static int indexOf(byte[] str, byte[] substr, int start) {
    if (str == null) throw new NullPointerException();
    if (substr == null) throw new NullPointerException();
    if (start < 0) throw new ArrayIndexOutOfBoundsException();
    return nativeIndexOf(str, substr, start);
  }

  public static void readIntArrayBE(byte[] buffer, int offset, int[] dst, int size) {
    if (buffer == null) throw new NullPointerException();
    if (dst == null) throw new NullPointerException();
    if (size > dst.length) throw new ArrayIndexOutOfBoundsException();
    if (offset < 0) throw new ArrayIndexOutOfBoundsException();
    if (offset + (size << 2) > buffer.length) throw new ArrayIndexOutOfBoundsException();
    nativeReadIntArrayBE(buffer, offset, dst, size);
  }

  public static void readIntArrayLE(byte[] buffer, int offset, int[] dst, int size) {
    if (buffer == null) throw new NullPointerException();
    if (dst == null) throw new NullPointerException();
    if (size > dst.length) throw new ArrayIndexOutOfBoundsException();
    if (offset < 0) throw new ArrayIndexOutOfBoundsException();
    if (offset + (size << 2) > buffer.length) throw new ArrayIndexOutOfBoundsException();
    nativeReadIntArrayLE(buffer, offset, dst, size);
  }
  
  public static void writeIntArrayBE(int[] src, int start, int count,
      byte[] dst, int offset) {
    if ((src == null) || (dst == null)) {
      throw new NullPointerException();
    }
    if ((start < 0) || (count < 0) || (start + count > src.length)) {
      throw new ArrayIndexOutOfBoundsException();      
    }
    if ((offset < 0) || (offset + count * 4 > dst.length)) {
      throw new ArrayIndexOutOfBoundsException();      
    }
    NativeUtils.nativeWriteIntArrayBE(src, start, count, dst, offset);
  }

  public static void makeSampledPosVector(byte[] content, int[] dst, byte separator, int sample) {
    if (content == null) throw new NullPointerException();
    if (dst == null) throw new NullPointerException();
    if ((sample < 0) || (sample >= 30)) throw new IllegalArgumentException();
    int result = nativeMakeSampledPosVector(content, dst, separator, sample);
    if (result != 0) throw new ArrayIndexOutOfBoundsException();
  }

  public static int getMaxIntVectorDelta(int[] data) {
    if (data == null) throw new NullPointerException();
    if (data.length == 0) throw new ArrayIndexOutOfBoundsException();
    return nativeGetMaxIntVectorDelta(data);
  }

  public static void readCharArrayBE(byte[] buffer, int offset, char[] dst, int size) {
    if (buffer == null) throw new NullPointerException();
    if (dst == null) throw new NullPointerException();
    if (size > dst.length) throw new ArrayIndexOutOfBoundsException();
    if (offset < 0) throw new ArrayIndexOutOfBoundsException();
    if (offset + (size << 1) > buffer.length) throw new ArrayIndexOutOfBoundsException();
    nativeReadCharArrayBE(buffer, offset, dst, size);
  }

  public static void readCharArrayLE(byte[] buffer, int offset, char[] dst, int size) {
    if (buffer == null) throw new NullPointerException();
    if (dst == null) throw new NullPointerException();
    if (size > dst.length) throw new ArrayIndexOutOfBoundsException();
    if (offset < 0) throw new ArrayIndexOutOfBoundsException();
    if (offset + (size << 1) > buffer.length) throw new ArrayIndexOutOfBoundsException();
    nativeReadCharArrayLE(buffer, offset, dst, size);
  }

  public static void writeCharArrayBE(char[] src, int start, int count,
      byte[] dst, int offset) {
    if ((src == null) || (dst == null)) {
      throw new NullPointerException();
    }
    if ((start < 0) || (count < 0) || (start + count > src.length)) {
      throw new ArrayIndexOutOfBoundsException();      
    }
    if ((offset < 0) || (offset + count * 2 > dst.length)) {
      throw new ArrayIndexOutOfBoundsException();      
    }
    nativeWriteCharArrayBE(src, start, count, dst, offset);    
  }

  public static byte getMinByteArray(byte[] data) {
    if (data == null) throw new NullPointerException();
    return nativeGetMinByteArray(data);
  }

  public static byte getMaxByteArray(byte[] data) {
    if (data == null) throw new NullPointerException();
    return nativeGetMaxByteArray(data);
  }  
}
