package org.alexvod.geosearch;

import org.ushmax.common.Closure;

import android.content.SharedPreferences;

public interface Searcher {
  public static class Results {
    public String[] titles;
    public int[] x;
    public int[] y;
    public int nextHandle;  // -1 if eof
    public String query;
  }

  public void search(String query, int next_handle, Closure<Results> callback);

  public void loadPreferences(SharedPreferences mPrefs);
}
