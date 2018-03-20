/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.config;

import thredds.util.ThreddsConfigReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Read and process the threddsConfig.xml file.
 * You can access the values by calling ThreddsConfig.getXXX(name1.name2), where
 * <pre>
 *  <name1>
 *   <name2>value</name2>
 *  </name1>
 * </pre>
 */
public final class ThreddsConfig {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("serverStartup");
  private static ThreddsConfigReader reader;

  public static void init(String filename) {
    reader = new ThreddsConfigReader(filename, log);
  }

  static public String get(String paramName, String defValue) {
    if (reader == null) return defValue;
    return reader.get(paramName, defValue);
  }

  static public boolean hasElement(String paramName) {
    if (reader == null) return false;
    return reader.hasElement(paramName);
  }

  static public boolean getBoolean(String paramName, boolean defValue) {
    if (reader == null) return defValue;
    return reader.getBoolean(paramName, defValue);
  }

  // return null if not set
  static public Boolean getBoolean(String paramName) {
    return reader.getBoolean(paramName);
  }


  static public long getBytes(String paramName, long defValue) {
    if (reader == null) return defValue;
    return reader.getBytes(paramName, defValue);
  }

  static public int getInt(String paramName, int defValue) {
    if (reader == null) return defValue;
    return reader.getInt(paramName, defValue);
  }

  static public long getLong(String paramName, long defValue) {
    if (reader == null) return defValue;
    return reader.getLong(paramName, defValue);
  }

  static public int getSeconds(String paramName, int defValue) {
    if (reader == null) return defValue;
    return reader.getSeconds(paramName, defValue);
  }

  static public List<String> getRootList(String elementName) {
    if (reader == null) return new ArrayList<>(0);
    return reader.getRootList(elementName);
  }

}
