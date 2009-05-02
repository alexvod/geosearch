package org.alexvod.geosearch;

import java.io.IOException;
import java.io.InputStream;

public interface IStringData {
  public int searchSubstring(String needle, int pos);
  public int searchCharForward(char ch, int pos);
  public int searchCharBackward(char ch, int pos);
  public int getLength();
  public String getSubstring(int start, int end);
  public void initFromStream(InputStream stream) throws IOException;
  public int[] makePosVector();
}
