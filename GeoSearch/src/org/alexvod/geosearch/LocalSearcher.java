package org.alexvod.geosearch;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.nativeutils.NativeUtils;
import org.ushmax.android.SettingsHelper;
import org.ushmax.common.Closure;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;

import android.content.SharedPreferences;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public class LocalSearcher implements Searcher {
  private static final Logger logger = LoggerFactory.getLogger(LocalSearcher.class);
  private static final String PREF_RESULT_COUNT = "local_result_count";
  private static final int DEFAULT_RESULT_COUNT = 400;
  private IStringData stringData;
  private int resultCount;
  private int[] xcoord;
  private int[] ycoord;

  private int count;

  public LocalSearcher() {
    loadData();
  }

  private void loadData() {
    logger.debug("Loading search data...");
    String indexDataFile = "/sdcard/maps/index.dat";
    try {
      FileInputStream stream = new FileInputStream(indexDataFile);
      byte[] buffer = new byte[4];
      stream.read(buffer);
      count = NativeUtils.readIntBE(buffer, 0);
      logger.debug("index file has " + count + " entries");
      loadCoords(stream, count);
      loadContent(stream);
      stream.close();
    } catch (IOException f) {
      logger.error("Cannot read file " + indexDataFile);
    }
  }

  private void loadContent(FileInputStream stream) throws IOException {
    IStringData data = null; 
    data = new ByteStringData();
    data.initFromStream(stream);
    stringData = data;
  }

  private void loadCoords(FileInputStream stream, int count) throws IOException {
    xcoord = new int[count];
    ycoord = new int[count];
    byte[] buffer = new byte[4 * count];
    stream.read(buffer);
    NativeUtils.readIntArrayBE(buffer, 0, ycoord, count);
    stream.read(buffer);
    NativeUtils.readIntArrayBE(buffer, 0, xcoord, count);
  }

  @Override
  public void search(String substring, int start, Closure<Results> callback) {
    long startTime = System.currentTimeMillis();
    Results results = new Results();
    // TODO: this is dirty hack, rewrite it
    ArrayList<String> searchResults = new ArrayList<String>();
    searchResults.ensureCapacity(resultCount);
    int nextHandle = stringData.searchSubstring(substring, start, resultCount, searchResults);
    int num = searchResults.size();
    results.titles = new String[num];
    results.x = new int[num];
    results.y = new int[num];
    for (int i = 0; i < num; ++i) {
      int pos = stringData.getPosForResultNum(i); 
      int idx = stringData.getIndex(pos);
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