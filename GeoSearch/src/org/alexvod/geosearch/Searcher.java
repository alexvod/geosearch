package org.alexvod.geosearch;

public abstract class Searcher {
  public static class Results {
    public String[] titles;
    public int[] lats;
    public int[] lngs;
    public int next_handle;  // -1 if eof
    public String query;
  }

  public interface Callback {
    public void gotResults(final Results results); // null if query failed
  }

  public abstract void search(String query, int next_handle, Callback callback);
}
