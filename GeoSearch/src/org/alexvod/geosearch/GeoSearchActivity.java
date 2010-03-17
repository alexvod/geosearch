package org.alexvod.geosearch;

import org.alexvod.geosearch.Searcher.Results;
import org.nativeutils.ByteArraySlice;
import org.nativeutils.OutByteStream;
import org.ushmax.mapviewer.MercatorReference;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
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
  private static Searcher searcher;
  private String lastSearchText;
  private SharedPreferences mPrefs;
  private ArrayAdapter<String> adapter;
  private ListView searchResultsList;
  private Handler handler;
  private Results currentResults;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPrefs = getPreferences(MODE_WORLD_WRITEABLE);
    lastSearchText = mPrefs.getString("last_search_text", "");
    setContentView(R.layout.main);

    // Create search on the first run
    if (searcher == null) {
      Log.e(LOGTAG, "Creating new Searcher");
      searcher = new Searcher();
    } else {
      Log.e(LOGTAG, "Using existing searcher");
    }

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
        searcher.search(lastSearchText, 0, new Searcher.Callback() {
          @Override
          public void gotResults(final Results results) {
            handler.post(new Runnable() {
              @Override
              public void run() {
                updateResults(results);
              }
            });
          }
        });
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
              @Override
              public void gotResults(final Results results) {
                handler.post(new Runnable() {
                  @Override
                  public void run() {
                    updateResults(results);
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

  private void updateResults(Results results) {
    if (lastSearchText != results.query) {
      // stale callback
      return;
    }
    adapter.clear();
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
    currentResults = results;
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
    return true;
  }
    
  private void showResultsOnMap() {
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
    Point point = new Point();
    for (int i = 0; i < size; ++i) {
      getCoords(i, point);
      out.writeIntBE(point.x);
      out.writeIntBE(point.y);
      out.writeString(currentResults.titles[i]);
      out.writeString("");
      out.writeString("res.png");
    }
    return out.getResult();
  }

  private void getCoords(int index, Point point) {
    MercatorReference.fromGeo(
            (float)currentResults.lats[index],
            (float)currentResults.lngs[index], 20, point);
  }

  private void returnResult(int index) {
    Point point = new Point();
    getCoords(index, point);
    String title = currentResults.titles[index];
    Log.e(LOGTAG, "returning result: " + title + "@" + point.x + 
        "," + point.y);
    Intent intent = getIntent();
    intent.putExtra("org.alexvod.geosearch.TITLE", title);
    intent.putExtra("org.alexvod.geosearch.LAT", "" + point.x);
    intent.putExtra("org.alexvod.geosearch.LNG", "" + point.y);
    setResult(RESULT_OK, intent);
    finish();
  }

  protected void onPause() {
    super.onPause();

    SharedPreferences.Editor ed = mPrefs.edit();
    ed.putString("last_search_text", lastSearchText);
    ed.putString("last_search_pos", lastSearchText);
    ed.commit();
  }
}