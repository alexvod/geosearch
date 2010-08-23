package org.alexvod.geosearch.ui;

import org.alexvod.geosearch.R;
import org.alexvod.geosearch.Searcher;
import org.alexvod.geosearch.Searcher.Results;
import org.nativeutils.ByteArraySlice;
import org.nativeutils.OutByteStream;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class GeoSearchActivity extends Activity {
  private static final String LOGTAG = "GeoSearch_GeoSearchActivity";
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
    searcher = ((SearchApplication)getApplication()).getSearcher();
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
          if (currentResults.next_handle != -1) {
            searcher.search(currentResults.query,
                currentResults.next_handle,
                new Searcher.Callback() {
              public void gotResults(final Results results) {
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
    searcher.search(lastSearchText, 0, new Searcher.Callback() {
      public void gotResults(final Results results) {
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
    } else if (results.titles.length == 0) {
      adapter.add("- NO RESULTS -");
    } else {
      for (String result : results.titles) {
        adapter.add(result);
      }
      if (results.next_handle != -1) {
        adapter.add("- GET MORE RESULTS -");
      } else {
        adapter.add("[END]");
      }
    }
    if (addAtEnd) {
      int currentSize = currentResults.x.length;
      int resultsSize = results.x.length;
      // Append new data to currentResults.
      int[] newlats = new int[currentSize + resultsSize];
      System.arraycopy(currentResults.x, 0, newlats, 0, currentSize);
      System.arraycopy(results.x, 0, newlats, currentSize, resultsSize);
      currentResults.x = newlats;

      int[] newlngs = new int[currentSize + resultsSize];
      System.arraycopy(currentResults.y, 0, newlngs, 0, currentSize);
      System.arraycopy(results.y, 0, newlngs, currentSize, resultsSize);
      currentResults.y = newlngs;

      String[] newtitles = new String[currentSize + resultsSize];
      System.arraycopy(currentResults.titles, 0, newtitles, 0, currentSize);
      System.arraycopy(results.titles, 0, newtitles, currentSize, resultsSize);
      currentResults.titles = newtitles;

      currentResults.next_handle = results.next_handle;
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
            searchMode = otherMode;
            searcher = ((SearchApplication)getApplication()).createSearcher(); 
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
    Log.e(LOGTAG, "returning all results");
    ByteArraySlice packedResult = getPackedResults();
    Intent intent = getIntent();
    intent.putExtra("org.alexvod.geosearch.ALL_RESULTS", packedResult.getCopy());
    setResult(RESULT_OK, intent);
    finish();
  }

  private ByteArraySlice getPackedResults() {
    OutByteStream out = new OutByteStream();
    int size = currentResults.titles.length;
    out.writeIntBE(size);
    for (int i = 0; i < size; ++i) {
      out.writeIntBE(currentResults.x[i]);
      out.writeIntBE(currentResults.y[i]);
      out.writeString(currentResults.titles[i]);
      out.writeString("");
      out.writeString("res.png");
    }
    return out.getResult();
  }

  private void returnResult(int index) {
    String title = currentResults.titles[index];
    int x = currentResults.x[index];
    int y = currentResults.y[index];
    Log.e(LOGTAG, "returning result: " + title + "@" + x + "," + y);
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