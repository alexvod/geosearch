package org.nativeutils;

public final class ByteArraySlice {
  public final byte[] data;
  public final int start;
  public final int count;
  
  public ByteArraySlice(byte[] bytes) {
    data = bytes;
    start = 0;
    count = bytes.length;
  }
  
  public ByteArraySlice(byte[] bytes, int start, int count) {
    data = bytes;
    this.start = start;
    this.count = count;
  }

  // This method is inefficient - it creates a copy of the array.
  // It should be used only for debugging and testing.
  public byte[] getCopy() {
    byte[] copy = new byte[count];
    System.arraycopy(data, start, copy, 0, count);
    return copy;
  }
}
