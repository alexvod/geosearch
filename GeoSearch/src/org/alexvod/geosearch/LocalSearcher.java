package org.alexvod.geosearch;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.nativeutils.IOUtils;

import android.content.SharedPreferences;
import android.util.Log;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public class LocalSearcher extends Searcher {
  private static final String LOGTAG = "GeoSearch_LocalSearcher";
  private static final String PREF_RESULT_COUNT = "local_result_count";
  private static final int DEFAULT_RESULT_COUNT = 400;
  private IStringData string_data;
  private int resultCount;
  private int[] xcoord;
  private int[] ycoord;

  private int count;

  public LocalSearcher() {
    loadData();
  }

  private void loadData() {
    Log.d(LOGTAG, "Loading search data...");
    String indexDataFile = "/sdcard/maps/index.dat";
    try {
      FileInputStream stream = new FileInputStream(indexDataFile);
      byte[] buffer = new byte[4];
      stream.read(buffer);
      count = IOUtils.readIntBE(buffer, 0);
      Log.d(LOGTAG, "index file has " + count + " entries");
      loadCoords(stream, count);
      loadContent(stream);
      stream.close();
    } catch (IOException f) {
      Log.e(LOGTAG, "Cannot read file");
    }
  }

  private void loadContent(FileInputStream stream) throws IOException {
    IStringData data = null; 
    data = new ByteStringData();
    data.initFromStream(stream);
    string_data = data;
  }

  private void loadCoords(FileInputStream stream, int count) throws IOException {
    xcoord = new int[count];
    ycoord = new int[count];
    byte[] buffer = new byte[4 * count];
    stream.read(buffer);
    IOUtils.readIntArrayBE(buffer, 0, ycoord, count);
    stream.read(buffer);
    IOUtils.readIntArrayBE(buffer, 0, xcoord, count);
  }

  @Override
  public void search(String substring, int next_handle, Callback callback) {
          long startTime = System.currentTimeMillis();
    Results results = new Results();
    List<String> list_result = string_data.searchSubstring(substring, resultCount);
    int num = list_result.size();
    results.titles = new String[num];
    results.x = new int[num];
    results.y = new int[num];
    for (int i = 0; i < num; ++i) {
      int pos = string_data.getPosForResultNum(i); 
      int idx = string_data.getIndex(pos);
      results.x[i] = xcoord[idx];
      results.y[i] = ycoord[idx];
      results.titles[i] = list_result.get(i);
    }
    results.next_handle = -1;
    results.query = substring;
    Log.d(LOGTAG, "got " + num + " results");
    long endTime = System.currentTimeMillis();
    Log.d(LOGTAG, "search for " + substring + " took " + (endTime - startTime) + "ms");
    callback.gotResults(results);
  }

  @Override
  public void loadPreferences(SharedPreferences prefs) {
    if (!prefs.contains(PREF_RESULT_COUNT)) {
      SharedPreferences.Editor ed = prefs.edit();
      ed.putString(PREF_RESULT_COUNT, "" + DEFAULT_RESULT_COUNT);
      ed.commit();
    }
    resultCount = 0;
    try {
      resultCount = Integer.parseInt(prefs.getString(PREF_RESULT_COUNT, "0"));
    } catch (NumberFormatException e) {
      // Do nothing.
    }
    if (resultCount < 1 || resultCount > 1000) {
      resultCount = DEFAULT_RESULT_COUNT;
    }
  }
}