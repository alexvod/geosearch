package org.alexvod.geosearch;

import java.io.IOException;
import java.util.List;

import org.ushmax.android.SettingsHelper;
import org.ushmax.common.BadDataException;
import org.ushmax.common.Callback;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;
import org.ushmax.common.Pair;
import org.ushmax.geometry.GeoObject;
import org.ushmax.search.Query;
import org.ushmax.search.SearchIndex;

import android.content.SharedPreferences;

// Class that search for string (entered by user) with in (a previously 
// loaded) collection of points.
public final class LocalSearcher implements Searcher {
  private static final Logger logger = LoggerFactory.getLogger(LocalSearcher.class);
  private static final String PREF_RESULT_COUNT = "local_result_count";
  private static final int DEFAULT_RESULT_COUNT = 400;
  private SearchIndex index;
  private int resultCount;
  private boolean searchInDesc;
  
  public LocalSearcher(String indexFilename) throws IOException, BadDataException {
    index = new SearchIndex(indexFilename);
  }

  @Override
  public void search(String query, int start, Callback<Results> callback) {
    long startTime = System.currentTimeMillis();
    Query q = new Query();
    q.hasBounds = false;
    q.inTitle = !searchInDesc;
    q.query = query;
    q.start = start;
    q.numResults = resultCount;
    Pair<List<GeoObject>, Integer> searchResults = index.search(q);  
    long endTime = System.currentTimeMillis();
    Results results = new Results();
    results.nextHandle = searchResults.second.intValue();
    results.objects = searchResults.first;
    results.query = query;
    logger.debug("search for " + query + " took " + (endTime - startTime) + "ms, got " + results.objects.size() + " results");
    callback.run(results);
  }

  @Override
  public void loadPreferences(SharedPreferences prefs) {
    resultCount = SettingsHelper.getIntPref(prefs, PREF_RESULT_COUNT, DEFAULT_RESULT_COUNT);
    if (resultCount < 1) {
      resultCount = DEFAULT_RESULT_COUNT;
    }
    searchInDesc = SettingsHelper.getBoolPref(prefs, "search_in_desc", false);
  }
}