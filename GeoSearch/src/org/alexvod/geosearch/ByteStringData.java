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
  private byte[] content;
  int count;
  private int[] result_pos;
  private int[] offset;
  private Charset charset;

  public ByteStringData() {
  }

  public void initFromStream(InputStream stream) throws IOException {
    // Read number of entries.
    byte[] buffer = new byte[4];
    stream.read(buffer);
    count = NativeUtils.readIntBE(buffer, 0);
    
    // Read content.
    charset = Charset.read(stream);
    stream.read(buffer);
    int totalChars = NativeUtils.readIntBE(buffer, 0);
    content = new byte[totalChars];
    stream.read(content);
    logger.debug("Title index has " + totalChars + " characters");
    
    // Read offset array.
    offset = new int[count + 1];
    buffer = new byte[4 * (count + 1)];
    stream.read(buffer);
    NativeUtils.readIntArrayBE(buffer, 0, offset, count + 1);
  }

  public int getIndexForResultNum(int num) {
    return getIndex(result_pos[num]);
  }

  private int getIndex(int pos) {
    // Do binary search.
    int min_idx = 0;
    int max_idx = count - 1;
    // Check bounds
    if (pos <= offset[0]) return 0;
    if (pos >= offset[max_idx]) return max_idx;
    int mid_idx, mid_pos;
    while (max_idx - min_idx > 1) {
      mid_idx = (min_idx + max_idx) / 2;
      mid_pos = offset[mid_idx];
      if (pos == mid_pos) return mid_idx;
      if (pos > mid_pos) {
        min_idx = mid_idx;
      } else {
        max_idx = mid_idx;
      }
    }
    if (offset[max_idx] == pos) return max_idx;
    return min_idx;
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
      int start = searchCharBackward(content, (byte) 0, pos) + 1;
      int end = searchCharForward(content, (byte) 0, start + 1);
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
