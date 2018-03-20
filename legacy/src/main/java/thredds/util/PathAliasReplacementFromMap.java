/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.util;

import ucar.unidata.util.StringUtil2;

import java.util.HashMap;
import java.util.Map;

/**
 * PathAliasReplacement from map of key -> value
 *
 * @author caron
 * @since 3/9/2015
 */
public class PathAliasReplacementFromMap implements PathAliasReplacement {

  private final Map<String,String> aliases;

  public PathAliasReplacementFromMap(Map<String,String> aliases) {
    this.aliases = aliases;
  }

  public PathAliasReplacementFromMap(String... keyvals) {
    aliases = new HashMap<>();
    for (int i=0; i<keyvals.length;i+=2) {
       aliases.put(keyvals[i], keyvals[i+1]);
    }
  }

    @Override
    public boolean containsPathAlias(String path) {
      for (String key : aliases.keySet()) {
        if (path.contains(key)) return true;
      }
      return false;
    }

    @Override
    public String replaceIfMatch(String path) {
      for (Map.Entry<String, String> entry : aliases.entrySet()) {
        if (path.contains(entry.getKey())) {
          return StringUtil2.replace(path, entry.getKey(), entry.getValue());
        }
      }
      return path;
    }

    @Override
    public String replacePathAlias(String path) {
      return replaceIfMatch(path);
    }

}
