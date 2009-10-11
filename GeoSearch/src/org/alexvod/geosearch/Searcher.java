package org.alexvod.geosearch;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.nativeutils.IOUtils;

import android.util.Log;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public class Searcher {
  private static final String LOGTAG = "GeoSearch_Searcher";
  private final int RESULT_LIMIT = 100;
  private IStringData string_data;
  private int minLat;
  private int minLng;
  private byte[] latVector;
  private byte[] lngVector;

  private int count;

  public Searcher() {
    loadData();
  }
  
  public List<String> search(String substring) {
    List<String> result = new LinkedList<String>();
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
    latlng[0] = (Get3ByteInt(latVector, idx) + minLat) * 1e-7;
    latlng[1] = (Get3ByteInt(lngVector, idx) + minLng) * 1e-7;
  }

  private static int Get3ByteInt(byte[] vector, int idx) {
    final int offset = 3 * idx;
    int t = 0;
    for(int i = 0; i < 3; i++) {
      t <<= 8;
      int b = vector[offset + i];
      t += b & 0xff;
    }
    return t; 
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
    byte[] buffer = new byte[8];
    stream.read(buffer, 0, 8);
    minLat = IOUtils.readIntBE(buffer, 0);
    minLng = IOUtils.readIntBE(buffer, 4);
    latVector = new byte[3 * count];
    lngVector = new byte[3 * count];
    stream.read(latVector, 0, 3 * count);
    stream.read(lngVector, 0, 3 * count);
  }
}