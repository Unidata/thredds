/* Copyright */
package ucar.nc2.util;

import ucar.unidata.util.StringUtil2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handle textual substitution for dataroots.
 *
 * @author caron
 * @since 4/16/2015
 */
public class AliasTranslator {

  private static Map<String, String> alias = new ConcurrentHashMap<>();

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
