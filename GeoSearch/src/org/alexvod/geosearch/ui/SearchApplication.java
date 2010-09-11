package org.alexvod.geosearch.ui;

import org.alexvod.geosearch.LocalSearcher;
import org.alexvod.geosearch.RemoteSearcher;
import org.alexvod.geosearch.Searcher;
import org.ushmax.common.Factory;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SearchApplication extends Application {
  private final Logger logger; 
  private Searcher searcher;
  
  public SearchApplication() {
    System.loadLibrary("nativeutils");
    LoggerFactory.setLoggerFactory(new Factory<Logger, Class<?>>() {
      public Logger create(Class<?> clazz) {
        return new AndroidLogger(clazz.getSimpleName());
      }});
    logger = LoggerFactory.getLogger(SearchApplication.class);
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
      searcher = new RemoteSearcher();
    } else if (searchMode.equals("local")) {
      logger.debug("Creating new LocalSearcher");
      searcher = new LocalSearcher();
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
