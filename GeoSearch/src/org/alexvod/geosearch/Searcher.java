  package org.alexvod.geosearch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;

import org.nativeutils.InByteStream;

import android.util.Log;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public class Searcher {
  private static final String LOGTAG = "GeoSearch_Searcher";
  private static final String URL_FORMAT = "http://syringa.org/search?q=%s&s=%d&n=%d";
  private final int RESULT_LIMIT = 50;
  
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
    currentQuery = new Thread(new Runnable() {
      public void run() {
        Log.d(LOGTAG, "searching for " + substring);
        long startTime = System.currentTimeMillis();
        Results results = querySynchronous(substring, cont_handle, RESULT_LIMIT);
        if (results == null) {
          Log.d(LOGTAG, "got error");
        } else {
          Log.d(LOGTAG, "got " + results.titles.length + " results");
        }
        long endTime = System.currentTimeMillis();
        Log.d(LOGTAG, "search for " + substring + " took " + (endTime - startTime) + "ms");
        if (Thread.currentThread() != currentQuery) {
          Log.d(LOGTAG, "Preempted, not invoking callback");
        } else {
          callback.gotResults(results);
        }
      }
    });
    currentQuery.start();
  }

  private Results querySynchronous(String substring, int handle, int limit) {
    try {
      String encoded = URLEncoder.encode(substring, "utf-8");
      String url = String.format(URL_FORMAT, encoded, handle, limit);
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
      Log.w(LOGTAG, e.toString());
      return null;
    }
  }

  private char[] buffer = new char[8192];
  private Results parse(byte[] byteArray) {
    try {
      InByteStream ibs = new InByteStream(byteArray, 0, byteArray.length);
      int next = ibs.readIntBE();
      int num = ibs.readIntBE();
      //Log.d(LOGTAG, "next=" + next + " num=" + num);
      Results results = new Results();
      results.titles = new String[num];
      results.lats = new int[num];
      results.lngs = new int[num];
      for (int i = 0; i < num; ++i) {
        results.lats[i] = ibs.readIntBE();
        results.lngs[i] = ibs.readIntBE();
        int strlen = ibs.readIntBE();
        char[] buf;
        if (strlen > 8192) {
          buf = new char[strlen];
        } else {
          buf = buffer;
        }
        int s = ibs.parseUtf8String(buf, strlen);
        results.titles[i] = new String(buf, 0, s);
        //Log.d(LOGTAG, "lat=" + results.lats[i] + " lng=" + results.lngs[i] + " title=" + results.titles[i]);
      }
      results.next_handle = next;
      return results;
    } catch (ArrayIndexOutOfBoundsException e) {
      Log.w(LOGTAG, e.toString());
      return null;
    }
  }
}