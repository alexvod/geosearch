package org.alexvod.geosearch.ui;

import org.ushmax.common.Logger;

import android.util.Log;

public class AndroidLogger implements Logger {
  private final String tag;
  
  public AndroidLogger(String tag) {
    this.tag = tag;
  }
  
  public void debug(String message) {
    Log.d(tag, message);
  }

  public void error(String message) {
    Log.e(tag, message);
  }
  
  public void warning(String message) {
    Log.w(tag, message);
  }
}
