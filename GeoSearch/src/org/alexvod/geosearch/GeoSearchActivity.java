package org.alexvod.geosearch;

import java.util.List;

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
	private static Searcher searcher;
	//private Searcher searcher;
	private String lastSearchText;
    private SharedPreferences mPrefs;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = getPreferences(MODE_WORLD_WRITEABLE);
        lastSearchText = mPrefs.getString("last_search_text", "");
        setContentView(R.layout.main);

        // Create search on the first run
        if (searcher == null) {
        	Log.e("s", "Creating new Searcher");
        	searcher = new Searcher();
        } else {
        	Log.e("s", "Using existing searcher");
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
				Log.e("s", "Item " + position + " selected");
				String result = adapter.getItem(position);
				double[] latlng = new double[2];
				searcher.getCoordsForResult(position, latlng);
				returnResult(result, latlng);
			}
	    });
	    
	    searchText.setText(lastSearchText);
   }
    
   private void returnResult(String result, double[] latlng) {
	   Log.e("s", "returning result: " + result + "@" + latlng[0] + 
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