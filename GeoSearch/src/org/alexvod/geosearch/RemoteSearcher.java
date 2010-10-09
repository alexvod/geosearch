package org.alexvod.geosearch;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.ushmax.android.SettingsHelper;
import org.ushmax.common.ByteArraySlice;
import org.ushmax.common.Callback;
import org.ushmax.common.InByteStream;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;
import org.ushmax.fetcher.HttpFetcher;
import org.ushmax.fetcher.HttpFetcher.NetworkException;

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
  private final HttpFetcher httpFetcher;
  
  public RemoteSearcher(HttpFetcher httpFetcher) {
    this.httpFetcher = httpFetcher;
  }

  @Override
  public void search(final String substring,
      final int cont_handle,
      final Callback<Results> callback) {
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
          callback.run(results);
        }
      }
    });
    currentQuery.start();
  }

  private Results querySynchronous(String substring, int handle, int limit) {
    try {
      String encoded = URLEncoder.encode(substring, "utf-8");
      String url = String.format(urlFormat, encoded, handle, limit);
      ByteArraySlice data = httpFetcher.fetch(url);
      Results results = parse(data);
      if (results == null) return null;
      results.query = substring;
      return results;
    } catch (NetworkException e) {
      logger.warning("Network error occured: " + e);
      return null;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private char[] buffer = new char[8192];
  private Results parse(ByteArraySlice data) {
    try {
      InByteStream ibs = new InByteStream(data);
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
      results.nextHandle = next;
      return results;
    } catch (ArrayIndexOutOfBoundsException e) {
      logger.warning(e.toString());
      return null;
    }
  }

  @Override
  public void loadPreferences(SharedPreferences prefs) {
    urlFormat = SettingsHelper.getStringPref(prefs, PREF_URL_FORMAT, DEFAULT_URL_FORMAT);
    resultCount = SettingsHelper.getIntPref(prefs, PREF_RESULT_COUNT, DEFAULT_RESULT_COUNT); 
    if (resultCount < 1 || resultCount > 1000) {
      resultCount = DEFAULT_RESULT_COUNT;
    }
  }
}