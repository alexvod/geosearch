package org.alexvod.geosearch.ui;

import org.alexvod.geosearch.R;
import org.alexvod.geosearch.Searcher;
import org.alexvod.geosearch.Searcher.Results;
import org.ushmax.android.SettingsHelper;
import org.ushmax.common.Callback;
import org.ushmax.common.Logger;
import org.ushmax.common.LoggerFactory;
import org.ushmax.geometry.GeoObject;
import org.ushmax.kml.Proto.KmlFeature;
import org.ushmax.kml.Proto.KmlFile;
import org.ushmax.kml.Proto.PointGeometry;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class GeoSearchActivity extends Activity {
  private static final Logger logger = LoggerFactory.getLogger(GeoSearchActivity.class);
  private static final int SWITCH_MODE_MENU_ID = 1;
  private static final int SETTINGS_ACTIVITY = 1;
  private static Searcher searcher;
  private String lastSearchText;
  private SharedPreferences prefs;
  private ArrayAdapter<String> adapter;
  private ListView searchResultsList;
  private Handler handler;
  private Results currentResults;
  private String searchMode;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    searcher = ((GeoSearchApplication)getApplication()).getSearcher();
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    loadPreferences();
    setContentView(R.layout.main);

    adapter = new ArrayAdapter<String>(this, R.layout.searchresult);
    handler = new Handler();

    EditText searchText = (EditText)findViewById(R.id.SearchText);
    searchText.addTextChangedListener(new TextWatcher() {
      public void afterTextChanged(Editable s) { }
      public void beforeTextChanged(CharSequence s, int start, int count,
          int after) {}
      public void onTextChanged(CharSequence s, int start, int before,
          int count) {
        lastSearchText = s.toString();
        doSearch();
      }
    });
    searchResultsList = (ListView)findViewById(R.id.ResultList);
    searchResultsList.setAdapter(adapter);

    searchResultsList.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view,
          int position, long id) {
        if (adapter.getCount() <= 1) {
          return;
        }
        if (adapter.getCount() > 1 && position == adapter.getCount()-1) {
          if (currentResults.nextHandle != -1) {
            searcher.search(currentResults.query,
                currentResults.nextHandle,
                new Callback<Results>() {
              @Override
              public void run(final Results results) {
                handler.post(new Runnable() {
                  public void run() {
                    updateResults(results, true);
                  }
                });
              }
            });
          }
        } else {
          returnResult(position);
        }
      }
    });

    searchText.setText(lastSearchText);
  }

  private void doSearch() {
    searcher.search(lastSearchText, 0, new Callback<Results>() {
      @Override
      public void run(final Results results) {
        handler.post(new Runnable() {
          public void run() {
            updateResults(results, false);
          }
        });
      }
    });
  }

  private void loadPreferences() {
    lastSearchText = prefs.getString("last_search_text", "");
    searchMode = prefs.getString("search_mode", "remote");
    if (searcher != null) {
      searcher.loadPreferences(prefs);
    }
  }

  private void updateResults(Results results, boolean addAtEnd) {
    if (results != null && lastSearchText != results.query) {
      // stale callback
      return;
    }
    if (addAtEnd) {
      // Remove the last (informational/active) element.
      adapter.remove(adapter.getItem(adapter.getCount()-1));
    } else {
      adapter.clear();
    }
    if (lastSearchText.length() == 0) {
      adapter.add("- NOTHING TO SEARCH FOR -");
    } else if (results == null) {
      adapter.add("- ERROR -");
    } else if (results.objects.size() == 0) {
      adapter.add("- NO RESULTS -");
    } else {
      for (GeoObject object : results.objects) {
        adapter.add(object.name);
      }
      int numResults = (addAtEnd ? currentResults.objects.size() : 0) + results.objects.size(); 
      if (results.nextHandle != -1) {
        adapter.add("- [" + numResults + "] GET MORE RESULTS -");
      } else {
        adapter.add("[END - " + numResults + " RESULTS TOTAL]");
      }
    }
    if (addAtEnd) {
      currentResults.objects.addAll(results.objects);
      currentResults.nextHandle = results.nextHandle;
    } else {
      currentResults = results;
    }
    adapter.notifyDataSetChanged();
    adapter.notifyDataSetInvalidated();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add("Show on map").setOnMenuItemClickListener(new OnMenuItemClickListener() {
      public boolean onMenuItemClick(MenuItem item) {
        showResultsOnMap();
        return true;
      }  
    });
    menu.add("Settings").setOnMenuItemClickListener(new OnMenuItemClickListener() {
      public boolean onMenuItemClick(MenuItem item) {
        showSettingsDialog();
        return true;
      }  
    });
    return true;
  }

  protected void showSettingsDialog() {
    Intent settingsIntent = new Intent(getBaseContext(), SettingsActivity.class );
    startActivityForResult(settingsIntent, SETTINGS_ACTIVITY);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == SETTINGS_ACTIVITY) {
      loadPreferences();
      doSearch();
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    final String otherMode = getNextModeName();
    menu.removeItem(SWITCH_MODE_MENU_ID);
    menu.add(Menu.NONE, SWITCH_MODE_MENU_ID, Menu.  NONE,
        "Switch to " + otherMode).setOnMenuItemClickListener(new OnMenuItemClickListener() {
          public boolean onMenuItemClick(MenuItem item) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GeoSearchActivity.this);
            SettingsHelper.setStringPref(prefs, "search_mode", otherMode);
            searchMode = otherMode;
            searcher = ((GeoSearchApplication)getApplication()).createSearcher(); 
            searcher.loadPreferences(prefs);
            doSearch();
            return true;
          }  
        });
    return true;
  }

  private String getNextModeName() {
    // Get name for alternative mode.
    if (searchMode.equals("local")) {
      return "remote";
    } else if (searchMode.equals("remote")) {
      return "local";
    }
    throw new RuntimeException("Unsupported search mode " + searchMode);
  }

  private void showResultsOnMap() {
    if (currentResults == null) {
      Toast.makeText(this, "No results to show", Toast.LENGTH_SHORT).show();
      return;
    } 
    logger.debug("returning all results");
    byte[] packedResult = getPackedResults();
    Intent intent = getIntent();
    intent.putExtra("org.alexvod.geosearch.ALL_RESULTS", packedResult);
    setResult(RESULT_OK, intent);
    finish();
  }

  private byte[] getPackedResults() {
    KmlFile.Builder builder = KmlFile.newBuilder();
    for (GeoObject object : currentResults.objects) {
      KmlFeature.Builder feature = KmlFeature.newBuilder();
      PointGeometry.Builder point = PointGeometry.newBuilder();
      point.setX(object.x);
      point.setY(object.y);
      feature.setPoint(point);
      feature.setName(object.name);
      feature.setIcon("red_dot.png");
      builder.addFeature(feature);
    }
    return builder.build().toByteArray();
  }

  private void returnResult(int index) {
    GeoObject object = currentResults.objects.get(index);
    String title = object.name;
    int x = object.x;
    int y = object.y;
    logger.debug("returning result: " + title + "@" + x + "," + y);
    Intent intent = getIntent();
    intent.putExtra("org.alexvod.geosearch.TITLE", title);
    intent.putExtra("org.alexvod.geosearch.XCOORD", "" + x);
    intent.putExtra("org.alexvod.geosearch.YCOORD", "" + y);
    setResult(RESULT_OK, intent);
    finish();
  }

  protected void onPause() {
    super.onPause();

    SharedPreferences.Editor ed = prefs.edit();
    ed.putString("last_search_text", lastSearchText);
    ed.putString("last_search_pos", lastSearchText);
    ed.putString("search_mode", searchMode);
    ed.commit();
  }
}