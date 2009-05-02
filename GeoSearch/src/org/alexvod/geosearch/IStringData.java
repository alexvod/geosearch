package org.alexvod.geosearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface IStringData {
  public void initFromStream(InputStream stream) throws IOException;
  public List<String> searchSubstring(String needle, int max_results);
  public int getIndex(int pos);
  public int getPosForResultNum(int num);
}
