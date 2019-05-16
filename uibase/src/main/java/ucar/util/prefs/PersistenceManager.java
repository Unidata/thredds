/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.util.prefs;

public interface PersistenceManager {

  void addPreferenceChangeListener(java.util.prefs.PreferenceChangeListener pcl);

  String get(String key, String def);
  void put(String key, String value);

  boolean getBoolean(String key, boolean def);
  void putBoolean(String key, boolean value);

  double getDouble(String key, double def);
  void putDouble(String key, double value);

  int getInt(String key, int def);
  void putInt(String key, int value);

  java.util.List getList(String key, java.util.List def);
  void putList(String key, java.util.List value);

  Object getObject(String key);
  void putObject(String key, Object value);

  long getLong(String key, long def);
  void putLong(String key, long value);
}