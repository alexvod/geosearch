package org.alexvod.geosearch.ui;

import org.alexvod.geosearch.LocalSearcher;
import org.alexvod.geosearch.RemoteSearcher;
import org.alexvod.geosearch.Searcher;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SearchApplication extends Application {
  private static final Logger logger = LoggerFactory.getLogger(SearchApplication.class);
  private Searcher searcher;
  
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
