package org.alexvod.geosearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class ByteStringData implements IStringData {
  private final int RESULT_LIMIT = 100;
  private byte[] content;
  private char[] charset;
  int count;
  private int[] result_pos;
  private int[] pos_vector;
  private byte separator;

  public ByteStringData() {
    result_pos = new int[RESULT_LIMIT];
  }
  
  private static int readInt(byte[] buffer, int offset) {
    int t = 0;
    for(int i = 3; i >= 0; --i) {
      t <<= 8;
      int b = buffer[offset+i];
      t += b & 0xff;
    }
    return t; 
  }

  public void initFromStream(InputStream stream) throws IOException {
    byte[] buffer = new byte[8];
    stream.read(buffer);
    count = readInt(buffer, 0);
    int totalChars = readInt(buffer, 4);

    charset = readCharset(stream);
    separator = char2byte('\n');
    
    Log.e("s", "Reading " + totalChars + " characters");
    content = new byte[totalChars];
    int numChars = stream.read(content);
    Log.e("s", "Total " + numChars + " characters loaded " +
        "(must be == " + count + ")");
    makePosVector();
  }
  
  private byte char2byte(char ch) {
    // Check bounds
    final int charset_size = charset.length;
    if (ch < charset[0]) return -1;
    if (ch > charset[charset_size - 1]) return -1;
    // Do binary search.
    int min_idx = 0;
    int max_idx = charset_size - 1;
    int mid_idx, mid_char;
    while (max_idx - min_idx > 1) {
      mid_idx = (min_idx + max_idx) / 2;
      mid_char = charset[mid_idx];
      if (mid_char == ch) return (byte)mid_idx;
      if (ch > mid_char) {
        min_idx = mid_idx;
      } else {
        max_idx = mid_idx;
      }
    }
    if (charset[min_idx] == ch) return (byte)min_idx;
    if (charset[max_idx] == ch) return (byte)max_idx;
    return -1;
  }
  
  private char byte2char(byte b) {
    return charset[b];
  }
   
  private static char[] readCharset(InputStream stream) throws IOException {
    byte[] buffer = new byte[4];
    stream.read(buffer);
    int charset_size = readInt(buffer, 0);
    Log.d("s", "Charset has " + charset_size + " chars");
    char[] chars = new char[charset_size];
    buffer = new byte[4*charset_size];
    stream.read(buffer);
    for (int i = 0; i < charset_size; i++) {
      chars[i] = (char)readInt(buffer, 4*i);
    }
    return chars;
  }
 
  private void makePosVector() {
    pos_vector = new int[count + 1];
    pos_vector[0] = 0;
    byte separator = char2byte('\n');
    int idx = 1;
    for (int i = 0; i < content.length; i++) {
      if (content[i] != separator) continue;
      pos_vector[idx] = i + 1;
      idx++;
    }
    Log.e("s", "found " + idx + " items in strings, must be == " + (count + 1));
  }
  
  public int getPosForResultNum(int num) {
    return result_pos[num];
  }

  public int getIndex(int pos) {
    // Check bounds
    if (pos <= pos_vector[0]) return 0;
    if (pos >= pos_vector[count-1]) return count-1;
    // Do binary search.
    int min_idx = 0;
    int max_idx = count-1;
    int mid_idx, mid_pos;
    while (max_idx - min_idx > 1) {
      mid_idx = (min_idx + max_idx) / 2;
      mid_pos = pos_vector[mid_idx];
      if (pos == mid_pos) return mid_idx;
      if (pos > mid_pos) {
        min_idx = mid_idx;
      } else {
        max_idx = mid_idx;
      }
    }
    if (pos_vector[min_idx] == pos) return min_idx;
    return max_idx;
  }
  
  private boolean decodeString(String string, byte[] bytes) {
    final int length = string.length();
    for (int i = 0; i < length; ++i) {
      byte b = char2byte(string.charAt(i));
      if (b == -1) return false;
      bytes[i] = b;
    }
    return true;
  }

  public List<String> searchSubstring(String s, int max_results) {
    List<String> result = new ArrayList<String>();
    final int str_length = s.length();
    byte[] encoded = new byte[str_length];
    if (!decodeString(s, encoded)) {
      Log.d("s", "cannot decode string");
      return result;
    }
    int searchStart = 0;
    int totalFound = 0;
    final int content_length = content.length;
    while (searchStart < content_length) {
      int pos = searchSubstringForward(content, encoded, searchStart);
      if (pos == -1) break;

      // -1 + 1 = 0
      int start = searchCharBackward(content, separator, pos) + 1;
      int end = searchCharForward(content, separator, start + 1);
      if (end == -1) end = content_length;
      result.add(getSubstring(start, end));
      result_pos[totalFound] = start;
      totalFound++;
      if (totalFound >= max_results) {
        Log.e("s", "got " + totalFound + " results, truncated");
        break;
      }
      searchStart = end + 1;
    }
    return result; 
  }

  static private int searchCharBackward(byte[] content, byte b, int start) {
    for (int i = start; i >= 0; --i) {
      if (content[i] == b) {
        return i;
      }
    }
    return -1;
  }

  static private int searchCharForward(byte[] content, byte b, int start) {
    final int length = content.length;
    for (int i = start; i < length; ++i) {
      if (content[i] == b) {
        return i;
      }
    }
    return -1;
  }

  static private int searchSubstringForward(byte[] text, byte[] substr,
      int start) {
    byte firstByte = substr[0];
    final int subLen = substr.length;
    final int len = text.length;
    while (true) {
        int i = searchCharForward(text, firstByte, start);
        if (i == -1 || subLen + i > len) {
            return -1; // handles subCount > count || start >= count
        }
        int i1 = i, i2 = 0;
        while (++i2 < subLen && text[++i1] == substr[i2]) {
            // Intentionally empty
        }
        if (i2 == subLen) {
            return i;
        }
        start = i + 1;
    }
  }
  
  private String getSubstring(int start, int end) {
    char[] chars = new char[end - start];
    for (int i = start; i < end; ++i) {
      chars[i - start] = byte2char(content[i]);
    }
    return new String(chars);
  }
}
