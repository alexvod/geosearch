package org.alexvod.geosearch.ui;

import java.io.IOException;

import org.alexvod.geosearch.LocalSearcher;
import org.alexvod.geosearch.RemoteSearcher;
import org.alexvod.geosearch.Searcher;
import org.ushmax.android.AndroidHttpFetcher;
import org.ushmax.android.AndroidLogger;
import org.ushmax.common.BadDataException;
import org.ushmax.common.Factory;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class GeoSearchApplication extends Application {
  private final Logger logger; 
  private Searcher searcher;
  
  public GeoSearchApplication() {
    System.loadLibrary("nativeutils");
    LoggerFactory.setLoggerFactory(new Factory<Logger, Class<?>>() {
      public Logger create(Class<?> clazz) {
        return new AndroidLogger(clazz.getSimpleName());
      }});
    logger = LoggerFactory.getLogger(GeoSearchApplication.class);
  }
  
  @Override
  public void onCreate() {
    super.onCreate();
    createSearcher();
  }

  public Searcher createSearcher() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    String searchMode = prefs.getString("search_mode", "remote");
    if (searchMode.equals("remote")) {
      logger.debug("Creating new RemoteSearcher");
      searcher = new RemoteSearcher(new AndroidHttpFetcher());
    } else if (searchMode.equals("local")) {
      logger.debug("Creating new LocalSearcher");
      try {
        searcher = new LocalSearcher("/sdcard/maps/index.dat");
      } catch (IOException e) {
        throw new RuntimeException("Failed to load search index");
      } catch (BadDataException e) {
        throw new RuntimeException("Corrupted search index");
      }
    } else {
      throw new RuntimeException("Unknown search mode " + searchMode);
    }
    searcher.loadPreferences(prefs);
    return searcher;
  }
  
  public Searcher getSearcher() {
    return searcher;
  }
}
