package org.ushmax;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.util.Log;

public class NativeUtils {
  private static final String LOGTAG = "NativeUtils";

  public static void loadNativeLibrary(Context context, String appname) throws IOException {
    if (nativeLibraryAvailable) return;
    String libraryPath = "/data/data/" + appname + "/libnativeutils_jni.so"; 
    if (!(new File(libraryPath)).exists()) {
      Log.d(LOGTAG, "Native library, doesn't exist. Trying to create");
      InputStream resourceStream = context.getResources().openRawResource(org.alexvod.geosearch.R.raw.libnativeutils_jni);
      byte[] data = new byte[resourceStream.available()];
      resourceStream.read(data);
      resourceStream.close();
      Log.d(LOGTAG, "Read " + data.length  + " bytes from resource");
      FileOutputStream outStream = new FileOutputStream(libraryPath);
      outStream.write(data);
      outStream.close();
    }
    try {
      Log.i(LOGTAG, "Trying to load " + libraryPath);
      System.load(libraryPath);
      Log.i(LOGTAG, "Successfully loaded!");
      nativeLibraryAvailable = true;
    }
    catch (UnsatisfiedLinkError ule) {
      Log.e(LOGTAG, "WARNING: Could not load " + libraryPath);
      nativeLibraryAvailable = false;
    }
  }
  
  private static native int nativeIndexOf(byte[] str, byte[] substr, int start);
  private static native void nativeReadIntArrayBE(byte[] buffer, int offset, int[] dst, int size);
  private static native int nativeMakeSampledPosVector(byte[] content, int[] dst, byte separator, int sample);
  private static native int nativeGetMaxIntVectorDelta(int[] data);
  private static native void nativeReadCharArrayBE(byte[] buffer, int offset, char[] dst, int size);
  
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
}
