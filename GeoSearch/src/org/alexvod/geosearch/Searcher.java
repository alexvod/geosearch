package org.alexvod.geosearch;

import java.util.List;

import org.ushmax.common.Callback;
import org.ushmax.wikimapia.Placemark;

import android.content.SharedPreferences;

public interface Searcher {
  public static class Results {
    public List<Placemark> placemarks;
    public int nextHandle;  // -1 if eof
    public String query;
  }

  public void search(String query, int next_handle, Callback<Results> callback);

  public void loadPreferences(SharedPreferences mPrefs);
}
