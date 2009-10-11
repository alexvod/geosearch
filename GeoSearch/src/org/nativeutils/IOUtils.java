package org.nativeutils;

import java.io.IOException;

public class IOUtils {
  // Currently Dalvik VM limits the whole application to 16 MB.
  private static final int MAX_STRING_LEN = 32 * 1024 * 1024;

  public static int readIntBE(byte[] buffer, int offset) {
    int b1 = buffer[offset] & 0xff; offset++;
    int b2 = buffer[offset] & 0xff; offset++;
    int b3 = buffer[offset] & 0xff; offset++;
    int b4 = buffer[offset] & 0xff; offset++;
    return ((b1 << 24) + (b2 << 16) + (b3 << 8) + b4);
  }

  public static int readIntLE(byte[] buffer, int offset) {
    int b1 = buffer[offset] & 0xff; offset++;
    int b2 = buffer[offset] & 0xff; offset++;
    int b3 = buffer[offset] & 0xff; offset++;
    int b4 = buffer[offset] & 0xff; offset++;
    return ((b4 << 24) + (b3 << 16) + (b2 << 8) + b1);
  }

  public static void readIntArrayBE(byte[] buffer, int offset, int[] dst, int size) {
    /*
    for (int i = 0; i < size; i++) {
      int b1 = buffer[offset] & 0xff; offset++;
      int b2 = buffer[offset] & 0xff; offset++;
      int b3 = buffer[offset] & 0xff; offset++;
      int b4 = buffer[offset] & 0xff; offset++;
      dst[i] = ((b1 << 24) + (b2 << 16) + (b3 << 8) + b4);
    }*/
    NativeUtils.readIntArrayBE(buffer, offset, dst, size);
  }

  public static void readCharArrayBE(byte[] buffer, int offset, char[] dst, int size) throws IOException {
    /*
    int end = offset + (size << 1);
    int idx = 0;
    while (offset < end) {
      int b1 = buffer[offset]; offset++;
      int b2 = buffer[offset]; offset++;
      str[idx] = (char) ((b1 << 8) + b2);
      idx++;
    } */
    NativeUtils.readCharArrayBE(buffer, offset, dst, size);
  }

  public static void readCharArrayLE(byte[] buffer, int offset, char[] dst, int size) throws IOException {
    /*
    int end = offset + (size << 1);
    int idx = 0;
    while (offset < end) {
      int b1 = buffer[offset]; offset++;
      int b2 = buffer[offset]; offset++;
      str[idx] = (char) ((b2 << 8) + b1);
      idx++;
    } */
    NativeUtils.readCharArrayLE(buffer, offset, dst, size);
  }

  public static char[] readCharArrayWithLenBE(byte[] buffer, int[] pos) throws IOException {
    int offset = pos[0];
    int size = readIntBE(buffer, offset); offset += 4;
    if ((size < 0) || (size > MAX_STRING_LEN)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    char[] str = new char[size];
    readCharArrayBE(buffer, offset, str, size);
    pos[0] = offset + (size << 1);
    return str;
  }

  public static char[] readCharArrayWithLenLE(byte[] buffer, int[] pos) throws IOException {
    int offset = pos[0];
    int size = readIntLE(buffer, offset); offset += 4;
    char[] str = new char[size];
    readCharArrayLE(buffer, offset, str, size);
    pos[0] = offset + (size << 1);
    return str;
  }
  
  public static void writeIntArrayBE(int[] src, int start, int count,
      byte[] dst, int offset) {
    /*
    for (int i = 0; i < count; i++) {
      int word = src[start + i];
      byte b4 = (byte) (word & 0xff); word >>= 8;
      byte b3 = (byte) (word & 0xff); word >>= 8;
      byte b2 = (byte) (word & 0xff); word >>= 8;
      byte b1 = (byte) (word & 0xff);
      dst[offset++] = b1;
      dst[offset++] = b2;
      dst[offset++] = b3;
      dst[offset++] = b4;
    }*/
    NativeUtils.writeIntArrayBE(src, start, count, dst, offset);
  }

  public static void writeCharArrayBE(char[] src, int start, int count,
      byte[] dst, int offset) {
    /*
    for (int i = 0; i < count; i++) {
      char ch = src[start + i];
      byte b2 = (byte) (ch & 0xff); ch >>= 8;
      byte b1 = (byte) (ch & 0xff);
      dst[offset++] = b1;
      dst[offset++] = b2;
    }*/
    NativeUtils.writeCharArrayBE(src, start, count, dst, offset);
  }
}
