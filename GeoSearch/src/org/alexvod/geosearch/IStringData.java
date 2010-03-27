package org.alexvod.geosearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public interface IStringData {
  public void initFromStream(InputStream stream) throws IOException;
  public int searchSubstring(String needle, int next_handle, int max_results, ArrayList<String> output);
  public int getIndex(int pos);
  public int getPosForResultNum(int num);
}
