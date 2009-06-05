package org.ushmax;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

public class IOUtils {
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

  public static char[] readCharArray(byte[] buffer, int[] pos) throws IOException {
    int offset = pos[0];
    int size = readIntBE(buffer, offset); offset += 4;
    char[] str = new char[size];
    /*
    int end = offset + (size << 1);
    int idx = 0;
    while (offset < end) {
      int b1 = buffer[offset]; offset++;
      int b2 = buffer[offset]; offset++;
      str[idx] = (char) ((b1 << 8) + b2);
      idx++;
    }
    pos[0] = offset;
    return str;*/
    NativeUtils.readCharArrayBE(buffer, offset, str, size);
    pos[0] = offset + (size << 1);
    return str;
  }
  
  public static void writeCharArray(DataOutput out, char[] s) throws IOException {
    int size = s.length;
    out.writeInt(size);
    for (int i = 0; i < size; i++) {
      out.writeChar(s[i]);
    }
  }
  
  public static void writeIntArray(int[] data, DataOutputStream out) throws IOException {
    final int count = data.length;
    for (int i = 0; i < count; i++) {
      out.writeInt(data[i]);
    }
  }
}
