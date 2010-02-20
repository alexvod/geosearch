package org.nativeutils;

import org.nativeutils.IOUtils;

/**
 * Fast binary output without any inheritance/customization.
 */
public class OutByteStream {
  private byte mData[];
  private int mCapacity;
  private int mLength;
  // invariant: mLength >= 0
  // invariant: mCapacity >= mLength
  // invariant: mCapacity == mData.length
  
  public OutByteStream() {
    Init();
  }

  private void Init() {
    mData = null;
    mCapacity = 0;
    mLength = 0;
  }

  public int totalSize() {
    return mLength;
  }

  private void realloc(int size) {
    byte newData[] = new byte[size];
    if (mData != null) {
      System.arraycopy(mData, 0, newData, 0, mLength);
    }
    mData = newData;
    mCapacity = size;
  }
  
  public void allocHeadroom(int length) {
    int newLength = mLength + length;
    if (newLength <= mCapacity) {
      return;
    }
    if (newLength < mCapacity * 2) {
      newLength = mCapacity * 2;
    }
    realloc(newLength);
  }

  public void writeByte(byte item) {
    allocHeadroom(1); 
    mData[mLength] = item;
    mLength++;
  }

  public void writeByteUnchecked(byte item) {
    mData[mLength] = item;
    mLength++;
  }

  public void writeIntBE(int word) {
    allocHeadroom(4);
    writeIntBEUnchecked(word);
  }
  
  public void writeIntBEUnchecked(int word) {
    byte b4 = (byte) (word & 0xff); word >>= 8;
    byte b3 = (byte) (word & 0xff); word >>= 8;
    byte b2 = (byte) (word & 0xff); word >>= 8;
    byte b1 = (byte) (word & 0xff);
    int ofs = mLength;
    mData[ofs++] = b1;
    mData[ofs++] = b2;
    mData[ofs++] = b3;
    mData[ofs++] = b4;
    mLength = ofs;
  }
  
  public void writeShortBE(int word) {
    allocHeadroom(2);
    writeShortBEUnchecked(word);
  }
  
  public void writeShortBEUnchecked(int word) {
    byte b2 = (byte) (word & 0xff); word >>= 8;
    byte b1 = (byte) (word & 0xff);
    int ofs = mLength;
    mData[ofs++] = b1;
    mData[ofs++] = b2;
    mLength = ofs;
  }

  public void writeIntArrayBE(int[] items, int start, int count) {
    allocHeadroom(4 * count);
    IOUtils.writeIntArrayBE(items, start, count, mData, mLength);
    mLength += 4 * count;
  }

  public void writeCharArrayBE(char[] items) {
    int length = items.length;
    allocHeadroom(2 * length);
    IOUtils.writeCharArrayBE(items, 0, length, mData, mLength);
    mLength += 2 * length;
  }

  public ByteArraySlice getResult() {
    ByteArraySlice result = new ByteArraySlice(mData, 0, mLength);
    return result;
  }
  
  // Inefficient version: only for debugging/testing.
  public byte[] getResultAsByteArray() {
    return getResult().getCopy();
  }

  public void writeByteArray(byte[] items, int start, int count) {
    allocHeadroom(count);
    System.arraycopy(items, start, mData, mLength, count);
    mLength += count;
  }

  public void writeByteArray(byte[] items) {
    writeByteArray(items, 0, items.length);
  }

  public void skipBytes(int count) {
    allocHeadroom(count);
    mLength += count;
  }
  
  public void skipBytesUnchecked(int count) {
    mLength += count;
  }
}
