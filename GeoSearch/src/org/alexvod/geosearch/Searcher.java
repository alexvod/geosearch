package org.alexvod.geosearch;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nativeutils.IOUtils;

import android.util.Log;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public class Searcher {
  private static final String LOGTAG = "GeoSearch_Searcher";
  private final int RESULT_LIMIT = 400;
  private IStringData string_data;
  private int[] latVector;
  private int[] lngVector;

  private int count;

  public Searcher() {
    loadData();
  }
  
  public List<String> search(String substring) {
    List<String> result = new ArrayList<String>();
    if (string_data == null) {
      result.add("-NO CONTENT LOADED-");
      return result;
    }
    Log.d(LOGTAG, "searching for " + substring);
    if (substring.length() == 0) {
      result.add("-NOTHING TO SEARCH-");
      return result;
    }
    long startTime = System.currentTimeMillis();
    
    result = string_data.searchSubstring(substring, RESULT_LIMIT);
    
    if (result.size() == 0) {
      result.add("-NOT FOUND-");
    }
    Log.d(LOGTAG, "got " + result.size() + " results");
    long endTime = System.currentTimeMillis();
    Log.d(LOGTAG, "search for " + substring + " took " + (endTime - startTime) + "ms");
    return result;
  }

  public void getCoordsForResult(int num, double latlng[]) {
    int pos = string_data.getPosForResultNum(num); 
    int idx = string_data.getIndex(pos);
    latlng[0] = latVector[idx] * 1e-6;
    latlng[1] = lngVector[idx] * 1e-6;
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
    latVector = new int[count];
    lngVector = new int[count];
    byte[] buffer = new byte[4 * count];
    stream.read(buffer);
    IOUtils.readIntArrayBE(buffer, 0, latVector, count);
    stream.read(buffer);
    IOUtils.readIntArrayBE(buffer, 0, lngVector, count);
  }
}