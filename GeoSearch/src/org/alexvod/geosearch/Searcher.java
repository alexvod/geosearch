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
  private int[] latVector;
  private int[] lngVector;

  private int count;

  public Searcher() {
    loadData();
  }
  
  public interface Callback {
    public void gotResults(final Results results); // null if query failed
  }
  
  public class Results {
    public String[] titles;
    public int[] lats;
    public int[] lngs;
  }
  
  public void search(final String substring, final Callback callback) {
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        List<String> result = new ArrayList<String>();
        Log.d(LOGTAG, "searching for " + substring);
        if (substring.length() == 0) {
          result.add("-NOTHING TO SEARCH-");
          callback.gotResult(result);
        }
        long startTime = System.currentTimeMillis();
        
        result = querySynchronous(substring, RESULT_LIMIT);
        
        Log.d(LOGTAG, "got " + result.size() + " results");
        if (result.size() == 0) {
          result.add("-NOT FOUND-");
        }
        long endTime = System.currentTimeMillis();
        Log.d(LOGTAG, "search for " + substring + " took " + (endTime - startTime) + "ms");
        callback.gotResult(result);
      }
    });
    thread.start();
  }

  private List<String> querySynchronous(String substring, int rESULTLIMIT) {
    List<String> result = new ArrayList<String>();
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