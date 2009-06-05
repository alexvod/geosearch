package org.alexvod.geosearch;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.ushmax.IOUtils;

import android.util.Log;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public class Searcher {
  private static final String LOGTAG = "GeoSearch_Searcher";
  private final int RESULT_LIMIT = 100;
  private IStringData string_data;
  private int min_lat;
  private int min_lng;
  private byte[] lat_vector;
  private byte[] lng_vector;

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
    latlng[0] = (Get3ByteInt(lat_vector, idx) + min_lat) * 1e-7;
    latlng[1] = (Get3ByteInt(lng_vector, idx) + min_lng) * 1e-7;
  }

  private static int Get3ByteInt(byte[] vector, int idx) {
    final int offset = 3 * idx;
    int t = 0;
    for(int i = 2; i >= 0; --i) {
      t <<= 8;
      int b = vector[offset + i];
      t += b & 0xff;
    }
    return t; 
  }

  private void loadData() {
    Log.d(LOGTAG, "Loading search data...");
    String stringDataFile = "/sdcard/maps/string.dat";
    String indexDataFile = "/sdcard/maps/index.dat";
    try {
      loadCoords(indexDataFile);
      loadContent(stringDataFile);
    } catch (IOException f) {
      Log.e(LOGTAG, "Cannot read file");
    }
  }

  private void loadContent(String stringDataFile) throws IOException {
    FileInputStream stream = new FileInputStream(stringDataFile);
    byte[] buffer = new byte[4];
    stream.read(buffer);
    int dataFormat = IOUtils.readIntLE(buffer, 0);
    IStringData data = null; 
    if (dataFormat == 1) {
      data = new CharStringData();
    } else if (dataFormat == 2) {
      data = new ByteStringData();
    } else {
      Log.e(LOGTAG, "Unknown string file format " + dataFormat);
      throw new RuntimeException();
    }
    data.initFromStream(stream);
    stream.close();
    string_data = data;
  }
  
  private void loadCoords(String indexDataFile) throws IOException {
    InputStream stream = new FileInputStream(indexDataFile);
    byte[] buffer = new byte[20];
    stream.read(buffer, 0, 12);
    count = IOUtils.readIntLE(buffer, 0);
    Log.d(LOGTAG, "coord file has " + count + " entries");
    min_lat = IOUtils.readIntLE(buffer, 4);
    min_lng = IOUtils.readIntLE(buffer, 8);
    lat_vector = new byte[3 * count];
    lng_vector = new byte[3 * count];
    stream.read(lat_vector, 0, 3 * count);
    stream.read(lng_vector, 0, 3 * count);
    stream.close();
  }
}