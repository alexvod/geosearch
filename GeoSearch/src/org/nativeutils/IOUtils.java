package org.nativeutils;

import java.io.UnsupportedEncodingException;

public class IOUtils {
  private static final char REPLACEMENT_CHAR = (char) 0xfffd;

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

  public static void readCharArrayBE(byte[] buffer, int offset, char[] dst, int size) {
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

  public static void readCharArrayLE(byte[] buffer, int offset, char[] dst, int size) {
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

  public static void writeIntLE(int word, byte[] data, int offset) {
    byte b1 = (byte) (word & 0xff); word >>= 8;
    byte b2 = (byte) (word & 0xff); word >>= 8;
    byte b3 = (byte) (word & 0xff); word >>= 8;
    byte b4 = (byte) (word & 0xff);
    data[offset++] = b1;
    data[offset++] = b2;
    data[offset++] = b3;
    data[offset++] = b4;
  }

  public static void writeIntBE(int word, byte[] data, int offset) {
    byte b4 = (byte) (word & 0xff); word >>= 8;
    byte b3 = (byte) (word & 0xff); word >>= 8;
    byte b2 = (byte) (word & 0xff); word >>= 8;
    byte b1 = (byte) (word & 0xff);
    data[offset++] = b1;
    data[offset++] = b2;
    data[offset++] = b3;
    data[offset++] = b4;
  }
  
  // JAVACRAP: Why this is not a built-in function?
  public static int memmem(byte[] data, int start, int end, byte[] subData) {
    byte firstByte = subData[0];
    int subLen = subData.length;
    int len = end - subLen;
    // Search for the first byte of subData.
    for (int i = start; i <= len; ++i) {
      if (data[i] != firstByte) continue;
      int i1 = i, i2 = 0;
      while (++i2 < subLen && data[++i1] == subData[i2]) {
        // Intentionally empty
      }
      if (i2 == subLen) {
        return i;
      }
    }
    return -1;
  }

  public static boolean memcmp(byte[] data1, byte[] data2, int start1, int start2, 
      int length) {
    for (int i = 0; i < length; i++) {
      if (data1[start1 + i] != data2[start2 + i]) {
        return false;
      }
    }
    return true;
  }

  public static int readShortBE(byte[] buffer, int offset) {
    int b1 = buffer[offset] & 0xff; offset++;
    int b2 = buffer[offset] & 0xff; offset++;
    return (b1 << 8) + b2;
  }

  public static int parseUtf8String(byte[] buffer, int start, int length, char[] dst) {
    // TODO: optimize, rewrite via native

    // NOTE: this code is copy-pasted without modification from Android
    // JavaVM sources (file main/java/lang/String.java).

    // We inline UTF8 decoding for speed and because a
    // non-constructor can't write directly to the final
    // members 'value' or 'count'.
    byte[] d = buffer;
    char[] v = dst;

    int idx = start, last = start + length, s = 0;
    outer:
      while (idx < last) {
        byte b0 = d[idx++];
        if ((b0 & 0x80) == 0) {
          // 0xxxxxxx
          // Range:  U-00000000 - U-0000007F
          int val = b0 & 0xff;
          v[s++] = (char) val;
        } else if (((b0 & 0xe0) == 0xc0) ||
            ((b0 & 0xf0) == 0xe0) ||
            ((b0 & 0xf8) == 0xf0) ||
            ((b0 & 0xfc) == 0xf8) ||
            ((b0 & 0xfe) == 0xfc)) {
          int utfCount = 1;
          if ((b0 & 0xf0) == 0xe0) utfCount = 2;
          else if ((b0 & 0xf8) == 0xf0) utfCount = 3;
          else if ((b0 & 0xfc) == 0xf8) utfCount = 4;
          else if ((b0 & 0xfe) == 0xfc) utfCount = 5;

          // 110xxxxx (10xxxxxx)+
          // Range:  U-00000080 - U-000007FF (count == 1)
          // Range:  U-00000800 - U-0000FFFF (count == 2)
          // Range:  U-00010000 - U-001FFFFF (count == 3)
          // Range:  U-00200000 - U-03FFFFFF (count == 4)
          // Range:  U-04000000 - U-7FFFFFFF (count == 5)

          if (idx + utfCount > last) {
            v[s++] = REPLACEMENT_CHAR;
            break;
          }

          // Extract usable bits from b0
          int val = b0 & (0x1f >> (utfCount - 1));
          for (int i = 0; i < utfCount; i++) {
            byte b = d[idx++];
            if ((b & 0xC0) != 0x80) {
              v[s++] = REPLACEMENT_CHAR;
              idx--; // Put the input char back
              continue outer;
            }
            // Push new bits in from the right side
            val <<= 6;
            val |= b & 0x3f;
          }

          // Note: Java allows overlong char
          // specifications To disallow, check that val
          // is greater than or equal to the minimum
          // value for each count:
          //
          // count    min value
          // -----   ----------
          //   1           0x80
          //   2          0x800
          //   3        0x10000
          //   4       0x200000
          //   5      0x4000000

          // Allow surrogate values (0xD800 - 0xDFFF) to
          // be specified using 3-byte UTF values only
          if ((utfCount != 2) &&
              (val >= 0xD800) && (val <= 0xDFFF)) {
            v[s++] = REPLACEMENT_CHAR;
            continue;
          }

          // Reject chars greater than the Unicode
          // maximum of U+10FFFF
          if (val > 0x10FFFF) {
            v[s++] = REPLACEMENT_CHAR;
            continue;
          }

          // Encode chars from U+10000 up as surrogate pairs
          if (val < 0x10000) {
            v[s++] = (char) val;
          } else {
            int x = val & 0xffff;
            int u = (val >> 16) & 0x1f;
            int w = (u - 1) & 0xffff;
            int hi = 0xd800 | (w << 6) | (x >> 10);
            int lo = 0xdc00 | (x & 0x3ff);
            v[s++] = (char) hi;
            v[s++] = (char) lo;
          }
        } else {
          // Illegal values 0x8*, 0x9*, 0xa*, 0xb*, 0xfd-0xff
          v[s++] = REPLACEMENT_CHAR;
        }
      }

    return s;
  }

  // TODO: optimize
  public static String decodeUtf8(byte[] bytes) {
    try {
      return new String(bytes, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 is unsupported by JavaVM", e);
    }
  }

  // TODO: optimize
  public static String decodeUtf8(byte[] bytes, int start, int count) {
    try {
      return new String(bytes, start, count, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 is unsupported by JavaVM", e);
    }
  }
  
  // TODO: optimize
  public static byte[] encodeUtf8(String string) {
    try {
      return string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 is unsupported by JavaVM", e);
    }
  }
}
