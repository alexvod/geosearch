package org.alexvod.geosearch;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.util.Log;

public class CharStringData implements IStringData {
  private String content;
  int count;
  
  public int searchCharBackward(char ch, int pos) {
    return content.lastIndexOf(ch, pos);
  }

  public int searchCharForward(char ch, int pos) {
    return content.indexOf(ch, pos);
  }

  public int searchSubstring(String needle, int pos) {
    return content.indexOf(needle, pos);
  }

  public int getLength() {
    return content.length();
  }

  public String getSubstring(int start, int end) {
    return content.substring(start, end);
  }

  private int readInt(byte[] buffer, int offset) {
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
  }
 
  public int[] makePosVector() {
    int[] pos_vector = new int[count + 1];
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
    count = idx;
    return pos_vector;
  }
}
