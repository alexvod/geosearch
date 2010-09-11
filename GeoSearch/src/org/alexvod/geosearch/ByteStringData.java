package org.alexvod.geosearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.nativeutils.NativeUtils;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;

public class ByteStringData implements IStringData {
  private static final Logger logger = LoggerFactory.getLogger(ByteStringData.class);
  private static final int POS_VECTOR_SAMPLING = 3;
  private byte[] content;
  private char[] charset;
  int count;
  private int[] result_pos;
  private int[] pos_vector;
  private byte separator;

  public ByteStringData() {
  }

  public void initFromStream(InputStream stream) throws IOException {
    byte[] buffer = new byte[8];
    stream.read(buffer);
    count = NativeUtils.readIntBE(buffer, 0);
    int totalChars = NativeUtils.readIntBE(buffer, 4);

    charset = readCharset(stream);
    separator = char2byte('\n');

    logger.debug("Reading " + totalChars + " characters");
    content = new byte[totalChars];
    int numChars = stream.read(content);
    logger.debug("Total " + numChars + " characters loaded " +
        "(must be == " + totalChars + ")");
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
    return charset[(int)b & 0xff];
  }

  private static char[] readCharset(InputStream stream) throws IOException {
    byte[] buffer = new byte[4];
    stream.read(buffer);
    int charset_size = NativeUtils.readIntBE(buffer, 0);
    logger.debug("Custom charset has " + charset_size + " chars");
    char[] chars = new char[charset_size];
    buffer = new byte[2*charset_size];
    stream.read(buffer);
    NativeUtils.readCharArrayBE(buffer, 0, chars, charset_size);
    return chars;
  }

  private void makePosVector() {
    long startTime = System.currentTimeMillis();
    pos_vector = new int[(count >> POS_VECTOR_SAMPLING) + 1];
    byte separator = char2byte('\n');
    NativeUtils.makeSampledPosVector(content, pos_vector, separator, POS_VECTOR_SAMPLING);
    logger.debug("sample vector: " + (System.currentTimeMillis() - startTime) + "ms");
  }

  public int getPosForResultNum(int num) {
    return result_pos[num];
  }

  private int binarySearchForPos(int pos) {
    // Do binary search.
    int min_idx = 0;
    int max_idx = (count - 1) >> POS_VECTOR_SAMPLING;
    // Check bounds
    if (pos <= pos_vector[0]) return 0;
    if (pos >= pos_vector[max_idx]) return max_idx;
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
    if (pos_vector[max_idx] == pos) return max_idx;
    return min_idx;
  }

  public int getIndex(int pos) {
    int idx = binarySearchForPos(pos);
    int cur_pos = pos_vector[idx];
    int cur_idx = idx << POS_VECTOR_SAMPLING;
    while (cur_pos < pos) {
      if (content[cur_pos] == separator) cur_idx++;
      cur_pos++;
    }
    return cur_idx;
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

  public int searchSubstring(String s, int next_handle, int max_results, ArrayList<String> output) {
    result_pos = new int[max_results];
    final int str_length = s.length();
    if (str_length == 0) {
      return -1;
    }
    byte[] encoded = new byte[str_length];
    if (!decodeString(s, encoded)) {
      logger.debug("cannot decode string");
      return -1;
    }
    if (next_handle < 0 || next_handle > content.length) {
      throw new RuntimeException("Wrong next_handle for LocalSearch: " + next_handle);
    }
    int searchStart = next_handle;
    int totalFound = 0;
    final int contentLength = content.length;
    while (searchStart < contentLength) {
      int pos = 0;
      pos = NativeUtils.indexOf(content, encoded, searchStart);
      if (pos == -1) {
        searchStart = contentLength;
        break;
      }

      // -1 + 1 = 0
      int start = searchCharBackward(content, separator, pos) + 1;
      int end = searchCharForward(content, separator, start + 1);
      if (end == -1) end = contentLength;
      output.add(getSubstring(start, end));
      result_pos[totalFound] = start;
      totalFound++;
      searchStart = end + 1;
      if (totalFound >= max_results) {
        logger.debug("got " + totalFound + " results, truncated");
        break;
      }
    }
    if (searchStart >= contentLength) {
      return -1;
    }
    return searchStart; 
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

  private String getSubstring(int start, int end) {
    char[] chars = new char[end - start];
    for (int i = start; i < end; ++i) {
      chars[i - start] = byte2char(content[i]);
    }
    return new String(chars);
  }
}
