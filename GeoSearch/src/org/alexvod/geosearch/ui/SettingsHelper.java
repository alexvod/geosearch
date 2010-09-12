package org.alexvod.geosearch.ui;

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

  public static boolean getBoolPref(SharedPreferences prefs, String name, boolean defaultValue) {
    if (!prefs.contains(name)) {
      SharedPreferences.Editor ed = prefs.edit();
      ed.putBoolean(name, defaultValue);
      ed.commit();
      return defaultValue;
    }
    return prefs.getBoolean(name, defaultValue);
  }
  
  public static void setBoolPref(SharedPreferences prefs, String name, boolean value) {
    SharedPreferences.Editor ed = prefs.edit();
    ed.putBoolean(name, value);
    ed.commit();
  }
  
  public static void setStringPref(SharedPreferences prefs, String name, String value) {
    SharedPreferences.Editor ed = prefs.edit();
    ed.putString(name, value);
    ed.commit();
  }
}
