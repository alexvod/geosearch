package org.alexvod.geosearch;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nativeutils.NativeUtils;
import org.ushmax.android.SettingsHelper;
import org.ushmax.common.Callback;
import org.ushmax.common.Charset;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;
import org.ushmax.common.Pair;
import org.ushmax.geometry.GeoObject;

import android.content.SharedPreferences;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public final class LocalSearcher implements Searcher {
  private static final Logger logger = LoggerFactory.getLogger(LocalSearcher.class);
  private static final String PREF_RESULT_COUNT = "local_result_count";
  private static final int DEFAULT_RESULT_COUNT = 400;
  private static final int MAX_RESULTS = 1000;
  private final byte[] content;
  private final int count;
  private final int[] offset;
  private final Charset charset;
  private final int[] xcoord;
  private final int[] ycoord;
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
  
  public Pair<List<GeoObject>, Integer> search(String query, int start, int numResults) {
    ArrayList<GeoObject> result = new ArrayList<GeoObject>();
    if (start < 0) {
      // JAVACRAP Java type inference sucks
      return Pair.<List<GeoObject>, Integer>newInstance(result, -1);
    }
    
    int num = numResults;
    if (num < 0 || num > MAX_RESULTS) {
      num = MAX_RESULTS;
    }
    
    result.ensureCapacity(num);
    byte[] queryBytes = charset.encode(query);
    
    while (start < count) {
      int pos = NativeUtils.indexOf(content, queryBytes, offset[start]);
      if (pos < 0) {
        // Not found.
        start = -1;
        break;
      }
    
      int idx = findIndexByOffset(offset, pos);
      while ((idx + 1 < offset.length) && (offset[idx + 1] == offset[idx])) {
        idx++;
      }
      start = idx + 1;
      
      String name = charset.decodeSubstring(content, offset[idx], offset[idx + 1] - 1);
      result.add(new GeoObject(xcoord[idx], ycoord[idx], name, 0));
      if (result.size() >= num) {
        break;
      }
    }
    
    // JAVACRAP Java type inference sucks
    return Pair.<List<GeoObject>, Integer>newInstance(result, start);
  }

  @Override
  public void search(String query, int start, Callback<Results> callback) {
    long startTime = System.currentTimeMillis();
    Pair<List<GeoObject>, Integer> searchResults = search(query, start, resultCount);  
    long endTime = System.currentTimeMillis();
    Results results = new Results();
    results.nextHandle = searchResults.second.intValue();
    results.objects = searchResults.first;
    results.query = query;
    logger.debug("search for " + query + " took " + (endTime - startTime) + "ms, got " + results.objects.size() + " results");
    callback.run(results);
  }

  @Override
  public void loadPreferences(SharedPreferences prefs) {
    resultCount = SettingsHelper.getIntPref(prefs, PREF_RESULT_COUNT, DEFAULT_RESULT_COUNT);
    if (resultCount < 1 || resultCount > MAX_RESULTS) {
      resultCount = DEFAULT_RESULT_COUNT;
    }
  }
}