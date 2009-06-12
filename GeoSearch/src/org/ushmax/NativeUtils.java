package org.ushmax;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

import android.content.Context;
import android.util.Log;

public class NativeUtils {
  private static final String LOGTAG = "NativeUtils";
  // Update these constants when library is updated.
  private static final int kLibLength = 9304;;
  private static final long kLibChecksum = 0xa72b4b81l;

  public static void loadNativeLibrary(Context context, String libPath, int rawResourceId) throws IOException {
    if (nativeLibraryAvailable) return;
    String libraryPath = libPath + "/libnativeutils_jni.so";
    boolean uptodate = true;
    File lib = new File(libraryPath);
    if (!lib.exists()) {
      uptodate = false;
    } else if (lib.length() != kLibLength) {
      uptodate = false;
    } else {
      FileInputStream istream = new FileInputStream(lib);
      byte[] data = new byte[kLibLength];
      istream.read(data);
      istream.close();
      CRC32 crc32 = new CRC32();
      crc32.update(data);
      if (crc32.getValue() != kLibChecksum) {
        uptodate = false;
      }
    }
    if (!uptodate) {
      Log.d(LOGTAG, "Native library doesn't exist or out of date. Trying to create");
      InputStream resourceStream = context.getResources().openRawResource(rawResourceId);
      byte[] data = new byte[resourceStream.available()];
      resourceStream.read(data);
      resourceStream.close();
      Log.d(LOGTAG, "Read " + data.length  + " bytes from resource");
      FileOutputStream outStream = new FileOutputStream(libraryPath);
      outStream.write(data);
      outStream.close();
    } else {
      Log.d(LOGTAG, "Native library is up to date");
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
  private static native void nativeReadIntArrayLE(byte[] buffer, int offset, int[] dst, int size);
  private static native int nativeMakeSampledPosVector(byte[] content, int[] dst, byte separator, int sample);
  private static native int nativeGetMaxIntVectorDelta(int[] data);
  private static native void nativeReadCharArrayBE(byte[] buffer, int offset, char[] dst, int size);
  private static native void nativeReadCharArrayLE(byte[] buffer, int offset, char[] dst, int size);
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

  public static byte getMinByteArray(byte[] data) {
    if (data == null) throw new NullPointerException();
    return nativeGetMinByteArray(data);
  }

  public static byte getMaxByteArray(byte[] data) {
    if (data == null) throw new NullPointerException();
    return nativeGetMaxByteArray(data);
  }  
}
