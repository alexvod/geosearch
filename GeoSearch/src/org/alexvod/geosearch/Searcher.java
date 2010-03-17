package org.alexvod.geosearch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.nativeutils.InByteStream;

import android.util.Log;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public class Searcher {
  private static final String LOGTAG = "GeoSearch_Searcher";
  private static final String URL_FORMAT = null;
  private final int RESULT_LIMIT = 400;
  
  private Thread currentQuery = null;

  public interface Callback {
    public void gotResults(final Results results); // null if query failed
  }
  
  public class Results {
    public String[] titles;
    public int[] lats;
    public int[] lngs;
    public int next_handle;  // -1 if eof
    public String query;
  }
  
  public void search(final String substring,
                     final int cont_handle,
                     final Callback callback) {
    if (currentQuery != null) {
      currentQuery.stop();
      currentQuery = null;
    }
    currentQuery = new Thread(new Runnable() {
      @Override
      public void run() {
        Log.d(LOGTAG, "searching for " + substring);
        long startTime = System.currentTimeMillis();
        Results results = querySynchronous(substring, cont_handle, RESULT_LIMIT);
        Log.d(LOGTAG, "got " + results.titles.length + " results");
        long endTime = System.currentTimeMillis();
        Log.d(LOGTAG, "search for " + substring + " took " + (endTime - startTime) + "ms");
        callback.gotResults(results);
      }
    });
    currentQuery.start();
  }

  private Results querySynchronous(String substring, int handle, int limit) {
    try {
      // TODO:encode substring!!!
      String url = String.format(URL_FORMAT, substring, handle);
      URL remote = new URL(url);
      InputStream inStream = remote.openStream();
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      byte[] buf = new byte[8192];
      while (true) {
        int numBytes = inStream.read(buf);
        if (numBytes < 0)
          break;
        outStream.write(buf, 0, numBytes);
      }
      inStream.close();
      Results results = parse(outStream.toByteArray());
      if (results == null) return null;
      results.query = substring;
      return results;
    } catch (IOException e) {
      return null;
    }
  }

  private Results parse(byte[] byteArray) {
    try {
      InByteStream ibs = new InByteStream(byteArray, 0, byteArray.length);
      int next = ibs.readIntBE();
      int num = ibs.readIntBE();
      Results results = new Results();
      results.titles = new String[num];
      results.lats = new int[num];
      results.lngs = new int[num];
      for (int i = 0; i < num; ++i) {
        results.lats[i] = ibs.readIntBE();
        results.lngs[i] = ibs.readIntBE();
        results.titles[i] = new String(ibs.readCharArrayWithLenBE());
      }
      results.next_handle = next;
      return results;
    } catch (ArrayIndexOutOfBoundsException e) {
      return null;
    }
  }
}