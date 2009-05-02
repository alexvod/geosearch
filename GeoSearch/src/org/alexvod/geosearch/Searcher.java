package org.alexvod.geosearch;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public class Searcher {
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

  public List<String> search(String s) {
    List<String> result = new LinkedList<String>();
    if (string_data == null) {
      result.add("NO CONTENT LOADED");
      return result;
    }
    Log.e("s", "substring " + s);
    if (s.length() == 0) {
      result.add("-NOTHING TO SEARCH-");
      return result;
    }
    long startTime = System.currentTimeMillis();
    
    result = string_data.searchSubstring(s, RESULT_LIMIT);
    
    if (result.size() == 0) {
      result.add("-NOT FOUND-");
    }
    Log.e("s", "num results " + result.size());
    long endTime = System.currentTimeMillis();
    Log.d("s", "search for " + s + " took " + (endTime - startTime) + "ms");
    return result;
  }

  public void getCoordsForResult(int num, double latlng[]) {
    Log.e("s", "num = " + num);
    int pos = string_data.getPosForResultNum(num); 
    Log.e("pos", "pos = " + pos);
    int idx = string_data.getIndex(pos);
    Log.e("idx", "idx = " + idx);
    latlng[0] = (Get3ByteInt(lat_vector, idx) + min_lat) * 1e-7;
    latlng[1] = (Get3ByteInt(lng_vector, idx) + min_lng) * 1e-7;
  }

  private static int Get3ByteInt(byte[] vector, int idx) {
    final int offset = 3 * idx;
    int t = 0;
    for(int i = 2; i >= 0; --i) {
      t *= 256;
      int b = vector[offset + i];
      t += b & 0xff;
    }
    return t; 
  }

  private void loadData() {
    Log.e("s", "Loading search data...");
    String stringDataFile = "/sdcard/maps/string.dat";
    String indexDataFile = "/sdcard/maps/index.dat";
    try {
      loadCoords(indexDataFile);
      loadContent(stringDataFile);
    } catch (IOException f) {
      Log.e("s", "Cannot read file");
    }
    Log.e("s", "Loaded search data.");
  }

  private void loadContent(String stringDataFile) throws IOException {
    FileInputStream stream = new FileInputStream(stringDataFile);
    byte[] buffer = new byte[4];
    stream.read(buffer);
    int dataFormat = readInt(buffer, 0);
    IStringData data = null; 
    if (dataFormat == 1) {
      data = new CharStringData();
    } else if (dataFormat == 2) {
      data = new ByteStringData();
    } else {
      Log.e("s", "Unknown string file format " + dataFormat);
      throw new RuntimeException();
    }
    data.initFromStream(stream);
    stream.close();
    string_data = data;
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
 
  private void loadCoords(String indexDataFile) throws IOException {
    InputStream stream = new FileInputStream(indexDataFile);
    byte[] buffer = new byte[20];
    stream.read(buffer, 0, 12);
    count = readInt(buffer, 0);
    Log.e("s", "num entries " + count);
    min_lat = readInt(buffer, 4);
    min_lng = readInt(buffer, 8);
    Log.e("s", "min_lat=" + min_lat + " min_lng=" + min_lng);
    lat_vector = new byte[3 * count];
    lng_vector = new byte[3 * count];
    stream.read(lat_vector, 0, 3 * count);
    stream.read(lng_vector, 0, 3 * count);
    stream.close();
  }
}