package org.alexvod.geosearch;

import android.content.SharedPreferences;

public class SettingsHelper {
  public static int getIntPref(SharedPreferences prefs, String name, int defaultValue) {
    if (!prefs.contains(name)) {
      SharedPreferences.Editor ed = prefs.edit();
      ed.putString(name, "" + defaultValue);
      ed.commit();
      return defaultValue;
    }
    try {
      return Integer.parseInt(prefs.getString(name, "" + defaultValue));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public static String getStringPref(SharedPreferences prefs, String name, String defaultValue) {
    if (!prefs.contains(name)) {
      SharedPreferences.Editor ed = prefs.edit();
      ed.putString(name, defaultValue);
      ed.commit();
    }
    return prefs.getString(name, defaultValue);
  }
}
