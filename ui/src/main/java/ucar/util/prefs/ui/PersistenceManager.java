package ucar.util.prefs.ui;

public interface PersistenceManager {

  public void addPreferenceChangeListener(java.util.prefs.PreferenceChangeListener pcl);

  public String get(String key, String def);
  public void put(String key, String value);

  public boolean getBoolean(String key, boolean def);
  public void putBoolean(String key, boolean value);

  public double getDouble(String key, double def);
  public void putDouble(String key, double value);

  public int getInt(String key, int def);
  public void putInt(String key, int value);

  public java.util.List getList(String key, java.util.List def);
  public void putList(String key, java.util.List value);

  public Object getObject(String key);
  public void putObject(String key, Object value);

  public long getLong(String key, long def);
  public void putLong(String key, long value);
}