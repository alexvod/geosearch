package org.alexvod.geosearch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;

import org.alexvod.geosearch.ui.SettingsHelper;
import org.ushmax.common.InByteStream;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;

import android.content.SharedPreferences;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public class RemoteSearcher implements Searcher {
  private static final Logger logger = LoggerFactory.getLogger(RemoteSearcher.class);
  private static final String PREF_URL_FORMAT= "remote_url_format";
  private static String DEFAULT_URL_FORMAT = "http://syringa.org/search?q=%s&s=%d&n=%d";
  private static final String PREF_RESULT_COUNT = "remote_result_count";
  private static final int DEFAULT_RESULT_COUNT = 100;
  
  private int resultCount;
  private String urlFormat;
  private Thread currentQuery = null;

  public void search(final String substring,
      final int cont_handle,
      final Callback callback) {
    currentQuery = new Thread(new Runnable() {
      public void run() {
        logger.debug("searching for " + substring);
        long startTime = System.currentTimeMillis();
        Results results = querySynchronous(substring, cont_handle, resultCount);
        if (results == null) {
          logger.debug("got error");
        } else {
          logger.debug("got " + results.titles.length + " results");
        }
        long endTime = System.currentTimeMillis();
        logger.debug("search for " + substring + " took " + (endTime - startTime) + "ms");
        if (Thread.currentThread() != currentQuery) {
          logger.debug("Preempted, not invoking callback");
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
      String url = String.format(urlFormat, encoded, handle, limit);
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
      logger.warning(e.toString());
      return null;
    }
  }

  private char[] buffer = new char[8192];
  private Results parse(byte[] byteArray) {
    try {
      InByteStream ibs = new InByteStream(byteArray, 0, byteArray.length);
      int next = ibs.readIntBE();
      int num = ibs.readIntBE();
      //logger.debug("next=" + next + " num=" + num);
      Results results = new Results();
      results.titles = new String[num];
      results.x = new int[num];
      results.y = new int[num];
      for (int i = 0; i < num; ++i) {
        results.y[i] = ibs.readIntBE();
        results.x[i] = ibs.readIntBE();
        int strlen = ibs.readIntBE();
        char[] buf;
        if (strlen > 8192) {
          buf = new char[strlen];
        } else {
          buf = buffer;
        }
        int s = ibs.parseUtf8String(buf, strlen);
        results.titles[i] = new String(buf, 0, s);
        //logger.debug("lat=" + results.lats[i] + " lng=" + results.lngs[i] + " title=" + results.titles[i]);
      }
      results.next_handle = next;
      return results;
    } catch (ArrayIndexOutOfBoundsException e) {
      logger.warning(e.toString());
      return null;
    }
  }

  public void loadPreferences(SharedPreferences prefs) {
    urlFormat = SettingsHelper.getStringPref(prefs, PREF_URL_FORMAT, DEFAULT_URL_FORMAT);
    resultCount = SettingsHelper.getIntPref(prefs, PREF_RESULT_COUNT, DEFAULT_RESULT_COUNT); 
    if (resultCount < 1 || resultCount > 1000) {
      resultCount = DEFAULT_RESULT_COUNT;
    }
  }
}