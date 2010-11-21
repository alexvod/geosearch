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
  private byte[] content;
  private int count;
  private int[] result_pos;
  private int[] offset;
  private Charset charset;
  private int resultCount;
  private int[] xcoord;
  private int[] ycoord;

  public LocalSearcher() {
    try { 
      loadData("/sdcard/maps/index.dat");
    } catch (IOException f) {
      logger.error("Cannot read search index");
    }
  }

  private void loadData(String filename) throws IOException {
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
      int idx = getIndex(result_pos[i]); 
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