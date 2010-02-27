package org.nativeutils;

import org.nativeutils.IOUtils;

// NOTE: all read operations in this file can throw ArrayIndexOutOfBounds exceptions.
// This is not a bug, but a feature. Since JVM already check array indices, there is
// no reason in doing it twice. In fact, extra checks can severely hurt performance,
// especially on Android. As a bonus, the code becomes simpler.
public class InByteStream {
  // Currently Dalvik VM limits the whole application to 16 MB.
  private static final int kMaxStringLength = 32 * 1024 * 1024;
  private byte[] data;
  private int pos;
  private int end;
  
  public InByteStream(ByteArraySlice slice) {
    data = slice.data;
    pos = slice.start;
    end = slice.start + slice.count; 
  }

  public InByteStream(byte[] data, int pos, int end) {
    this.data = data;
    this.pos = pos;
    this.end = end;
  }

  public void reset(byte[] data, int pos, int end) {
    this.data = data;
    this.pos = pos;
    this.end = end;
  }

  public boolean done() {
    return pos >= end;
  }

  // Please use only as last resort.
  public byte[] getData() {
    return data;
  }

  public int getPosition() {
    return pos;
  }

  public int getEnd() {
    return end;
  }

  public int available() {
    return end - pos;
  }

  public int readShortBE() {
    int result = IOUtils.readShortBE(data, pos);
    pos += 2;
    return result;
  }

  public void readIntArrayBE(int[] buffer, int count) {
    IOUtils.readIntArrayBE(data, pos, buffer, count);
    pos += count * 4;
  }

  public void skip(int length) {
    pos += length;
  }

  public void skipByte() {
    pos++;;
  }
  
  public boolean skipUntil(byte[] sequence) {
    int newPos = IOUtils.memmem(data, pos, end, sequence);
    if (newPos < 0) {
      return false;
    }
    pos = newPos;
    return true;
  }

  public boolean skipUntil(byte marker) {
    while (pos < end) {
      if (data[pos] == marker) {
        return true;
      }
      pos++;
    }
    return false;
  }

  public int readByte() {
    int result = data[pos];
    pos++;
    return result;
  }

  public int readIntBE() {
    int result = IOUtils.readIntBE(data, pos);
    pos += 4;
    return result;    
  }

  public int parseUtf8String(char[] buffer, int bytesLen) {
    int charLen = IOUtils.parseUtf8String(data, pos, bytesLen, buffer);
    pos += bytesLen;
    return charLen;
  }

  public void readByteArray(byte[] buffer, int start, int count) {
    System.arraycopy(data, pos, buffer, start, count);
    pos += count;
  }

  public char[] readCharArrayWithLenBE() {
    int size = readIntBE();
    if ((size < 0) || (size > kMaxStringLength)) {
      throw new ArrayIndexOutOfBoundsException("Invalid string length " + size);
    }
    char[] string = new char[size];
    IOUtils.readCharArrayBE(data, pos, string, size);
    pos += (size << 1);
    return string;
  }
  
  public int getDecimalInt() {
    if (pos >= end) {
      return 0;
    }
    int sign = +1;
    if (data[pos] == '-') {
      sign = -1;
      pos++;
    }
    int result = 0;
    while (pos < end) {
      byte ch = data[pos];
      if (ch >= '0' && ch <= '9') {
        result = 10 * result + (ch - '0');
        pos++;
      } else {
        break;
      }
    }
    return sign * result;
  }
  
  private static int[] hextable = new int[256];
  static {
    for (int c = 0; c < 256; ++c) {
      if (c >= '0' && c <= '9') {
        hextable[c] = c - '0';
      } else if (c >= 'a' && c <= 'f') {
        hextable[c] = 10 + c - 'a';
      } else if (c >= 'A' && c <= 'F') {
        hextable[c] = 10 + c - 'A';
      } else {
        hextable[c] = 0;
      }
    }
  }

  // This function is very hot - try to keep it as fast as possible.
  public int getHex(int width) {
    int res = 0;
    int numEnd = pos + width;
    for (; pos < numEnd; ++pos) {
      int c = data[pos];
      res <<= 4;
      res += hextable[c & 0xff];
    }
    return res;
  }
  
  public ByteArraySlice readLineAsSlice() {
    if (pos >= end) {
      return null;
    }
    int lineEnd = pos;
    while (lineEnd < end) {
      byte ch = data[lineEnd];
      if ((ch == '\n') || (ch == '\r')){
        break;
      }
      lineEnd++;
    }
    ByteArraySlice result = new ByteArraySlice(data, pos, lineEnd - pos);
    pos = lineEnd + 1;
    return result;
  }
}
