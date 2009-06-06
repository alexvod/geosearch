package org.alexvod.geosearch;

import java.io.IOException;
import java.util.List;

import org.ushmax.NativeUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class GeoSearchActivity extends Activity {
  private static final String LOGTAG = "GeoSearch_GeoSearchActivity";
  private static Searcher searcher;
  private String lastSearchText;
  private SharedPreferences mPrefs;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    try {
      NativeUtils.loadNativeLibrary(this, "/data/data/org.alexvod.geosearch",
          org.alexvod.geosearch.R.raw.libnativeutils_jni);    
    } catch (IOException e) {
      Log.e(LOGTAG, "Could not load native library");
      e.printStackTrace();
    }
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

    EditText searchText = (EditText)findViewById(R.id.SearchText);
    final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.searchresult);
    searchText.addTextChangedListener(new TextWatcher() {
      public void afterTextChanged(Editable s) { }
      public void beforeTextChanged(CharSequence s, int start, int count,
          int after) {}
      public void onTextChanged(CharSequence s, int start, int before,
          int count) {
        lastSearchText = s.toString();
        List<String> searchResults = searcher.search(lastSearchText);
        adapter.clear();
        for (String result : searchResults) {
          adapter.add(result);
        }
        adapter.notifyDataSetChanged();
        adapter.notifyDataSetInvalidated();
      }
    });
    ListView searchResults = (ListView)findViewById(R.id.ResultList);
    searchResults.setAdapter(adapter);

    searchResults.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view,
          int position, long id) {
        String result = adapter.getItem(position);
        double[] latlng = new double[2];
        searcher.getCoordsForResult(position, latlng);
        returnResult(result, latlng);
      }
    });

    searchText.setText(lastSearchText);
  }

  private void returnResult(String result, double[] latlng) {
    Log.e(LOGTAG, "returning result: " + result + "@" + latlng[0] + 
        "," + latlng[1]);
    Intent intent = getIntent();
    intent.putExtra("org.alexvod.geosearch.TITLE", result);
    intent.putExtra("org.alexvod.geosearch.LAT", "" + latlng[0]);
    intent.putExtra("org.alexvod.geosearch.LNG", "" + latlng[1]);
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