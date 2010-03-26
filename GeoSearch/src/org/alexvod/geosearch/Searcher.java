package org.alexvod.geosearch;

import android.content.SharedPreferences;

public abstract class Searcher {
  public static class Results {
    public String[] titles;
    public int[] x;
    public int[] y;
    public int next_handle;  // -1 if eof
    public String query;
  }

  public interface Callback {
    public void gotResults(final Results results); // null if query failed
  }

  public abstract void search(String query, int next_handle, Callback callback);

  public abstract void loadPreferences(SharedPreferences mPrefs);
}
