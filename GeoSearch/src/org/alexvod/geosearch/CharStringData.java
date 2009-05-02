package org.alexvod.geosearch;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class CharStringData implements IStringData {
  private final int RESULT_LIMIT = 100;
  private String content;
  int count;
  private int[] result_pos;
  private int[] pos_vector;
  
  public CharStringData() {
    result_pos = new int[RESULT_LIMIT];
  }
  
  public List<String> searchSubstring(String s, int max_result) {
    List<String> result = new ArrayList<String>();
    int searchStart = 0;
    int totalFound = 0;
    final int content_length = content.length();
    while (searchStart < content_length) {
      int pos = content.indexOf(s, searchStart);
      if (pos == -1) break;

      // -1 + 1 = 0
      int start = content.lastIndexOf('\n', pos) + 1;
      int end = content.indexOf('\n', start + 1);
      if (end == -1) end = content_length;
      result.add(content.substring(start, end));
      result_pos[totalFound] = start;
      totalFound++;
      if (totalFound >= max_result) {
        Log.e("s", "got " + totalFound + " results, truncated");
        break;
      }
      searchStart = end + 1;
    }
    return result; 
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
    Log.e("s", "Reading " + totalChars + " characters");
    // Read file with UTF-8
    InputStreamReader reader = new InputStreamReader(stream, "UTF-16LE");
    StringBuilder builder = new StringBuilder(totalChars);
    char[] inputBuffer = new char[8192];
    while (true) {
      int numChars = reader.read(inputBuffer);
      if (numChars <= 0) break;
      builder.append(inputBuffer, 0, numChars);
    }
    content = builder.toString();
    Log.e("s", "Total " + content.length() + " characters loaded " +
        "(must be == " + totalChars + ")");
    makePosVector();
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

  private void makePosVector() {
    pos_vector = new int[count + 1];
    pos_vector[0] = 0;
    int pos = -1;
    int idx = 1;
    while (true) {
      pos = content.indexOf('\n', pos + 1);
      if (pos == -1) break;
      pos_vector[idx] = pos + 1;
      idx++;
    }
    Log.e("s", "found " + idx + " items in strings, must be == " + (count + 1));
  }

  public int getPosForResultNum(int num) {
    return result_pos[num];
  }
}
