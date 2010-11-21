package org.alexvod.geosearch;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.nativeutils.NativeUtils;
import org.ushmax.android.SettingsHelper;
import org.ushmax.common.Callback;
import org.ushmax.common.Charset;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;

import android.content.SharedPreferences;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public class LocalSearcher implements Searcher {
  private static final Logger logger = LoggerFactory.getLogger(LocalSearcher.class);
  private static final String PREF_RESULT_COUNT = "local_result_count";
  private static final int DEFAULT_RESULT_COUNT = 400;
  private final byte[] content;
  private final int count;
  private final int[] offset;
  private final Charset charset;
  private final int[] xcoord;
  private final int[] ycoord;
  private int[] result_pos;
  private int resultCount;
  
  public LocalSearcher(String filename) throws IOException {
    logger.debug("Loading search index from " + filename);
    FileInputStream stream = new FileInputStream(filename);
    
    // Read number of entries.
    byte[] buffer = new byte[4];
    stream.read(buffer);
    count = NativeUtils.readIntBE(buffer, 0);
    logger.debug("Index file has " + count + " entries");
    buffer = new byte[4 * (count + 1)];

    // Read coords
    xcoord = new int[count];
    ycoord = new int[count];
    stream.read(buffer, 0, 4 * count);
    NativeUtils.readIntArrayBE(buffer, 0, ycoord, count);
    stream.read(buffer, 0, 4 * count);
    NativeUtils.readIntArrayBE(buffer, 0, xcoord, count);
    
    // Read content.
    charset = Charset.read(stream);
    stream.read(buffer, 0, 4);
    int totalChars = NativeUtils.readIntBE(buffer, 0);
    content = new byte[totalChars];
    stream.read(content);
    logger.debug("Title index has " + totalChars + " characters");

    // Read offset array.
    offset = new int[count + 1];
    stream.read(buffer, 0, 4 * (count + 1));
    NativeUtils.readIntArrayBE(buffer, 0, offset, count + 1);

    stream.close();
  }

  // JAVACRAP: why this is not a built-in function?
  private static int findIndexByOffset(int[] array, int value) {
    // Binary search with a small modification.
    int left = 0;
    int right = array.length - 1;
    if (value <= array[0]) return 0;
    if (value >= array[right]) return right;
    while (right - left > 1) {
      int middle = (left + right) / 2;
      int midValue = array[middle];
      if (value == midValue) return middle;
      if (value > midValue) {
        left = middle;
      } else {
        right = middle;
      }
    }
    if (array[right] == value) return right;
    return left;
  }  

  private int searchSubstring(String s, int nextHandle, int max_results, ArrayList<String> output) {
    result_pos = new int[max_results];
    final int str_length = s.length();
    if (str_length == 0) {
      return -1;
    }
    byte[] encoded = charset.encode(s);
    if (nextHandle < 0 || nextHandle > content.length) {
      throw new RuntimeException("Wrong next_handle for LocalSearch: " + nextHandle);
    }
    int searchStart = nextHandle;
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

  @Override
  public void search(String substring, int start, Callback<Results> callback) {
    long startTime = System.currentTimeMillis();
    Results results = new Results();
    // TODO: this is dirty hack, rewrite it
    ArrayList<String> searchResults = new ArrayList<String>();
    searchResults.ensureCapacity(resultCount);
    int nextHandle = searchSubstring(substring, start, resultCount, searchResults);
    int num = searchResults.size();
    results.titles = new String[num];
    results.x = new int[num];
    results.y = new int[num];
    for (int i = 0; i < num; ++i) {
      int idx = findIndexByOffset(offset, result_pos[i]); 
      results.x[i] = xcoord[idx];
      results.y[i] = ycoord[idx];
      results.titles[i] = searchResults.get(i);
    }
    results.nextHandle = nextHandle;
    results.query = substring;
    logger.debug("got " + num + " results");
    long endTime = System.currentTimeMillis();
    logger.debug("search for " + substring + " took " + (endTime - startTime) + "ms");
    callback.run(results);
  }

  @Override
  public void loadPreferences(SharedPreferences prefs) {
    resultCount = SettingsHelper.getIntPref(prefs, PREF_RESULT_COUNT, DEFAULT_RESULT_COUNT);
    if (resultCount < 1 || resultCount > 1000) {
      resultCount = DEFAULT_RESULT_COUNT;
    }
  }
}