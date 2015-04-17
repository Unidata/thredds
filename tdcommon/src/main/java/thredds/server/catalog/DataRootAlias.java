/* Copyright */
package thredds.server.catalog;

import ucar.unidata.util.StringUtil2;

import java.util.HashMap;
import java.util.Map;

/**
 * Describe
 *
 * @author caron
 * @since 4/16/2015
 */
public class DataRootAlias {

  private static Map<String, String> alias = new HashMap<>();

  public static void addAlias(String aliasKey, String actual) {
    alias.put(aliasKey, StringUtil2.substitute(actual, "\\", "/"));
  }

  public static String translateAlias(String scanDir) {
    for (Map.Entry<String, String> entry : alias.entrySet()) {
      if (scanDir.startsWith(entry.getKey()))  {   // only at the front
        StringBuilder sb = new StringBuilder(scanDir);
        return sb.replace(0, entry.getKey().length(), entry.getValue()).toString();
      }
    }
    return scanDir;
  }

  public static int size() {
    return alias.size();
  }

}
