package org.alexvod.geosearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.nativeutils.NativeUtils;
import org.ushmax.common.Charset;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;

public class ByteStringData implements IStringData {
  private static final Logger logger = LoggerFactory.getLogger(ByteStringData.class);
  private static final int POS_VECTOR_SAMPLING = 3;
  private byte[] content;
  int count;
  private int[] result_pos;
  private int[] pos_vector;
  private byte separator;
  private Charset charset;

  public ByteStringData() {
  }

  public void initFromStream(InputStream stream) throws IOException {
    byte[] buffer = new byte[8];
    stream.read(buffer);
    count = NativeUtils.readIntBE(buffer, 0);
    int totalChars = NativeUtils.readIntBE(buffer, 4);

    charset = Charset.read(stream);
    // TODO: switch to 0 as separator.
    separator = charset.encode("\n")[0];

    logger.debug("Reading " + totalChars + " characters");
    content = new byte[totalChars];
    int numChars = stream.read(content);
    logger.debug("Total " + numChars + " characters loaded " +
        "(must be == " + totalChars + ")");
    makePosVector();
  }

  private void makePosVector() {
    long startTime = System.currentTimeMillis();
    pos_vector = new int[(count >> POS_VECTOR_SAMPLING) + 1];
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

  public int searchSubstring(String s, int next_handle, int max_results, ArrayList<String> output) {
    result_pos = new int[max_results];
    final int str_length = s.length();
    if (str_length == 0) {
      return -1;
    }
    byte[] encoded = charset.encode(s);
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
      output.add(charset.decodeSubstring(content, start, end));
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
}
