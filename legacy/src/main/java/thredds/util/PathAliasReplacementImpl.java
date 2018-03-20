/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.util;

//import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * PathAliasReplacement where alias must be at the start of the path.
 *
 * @author edavis
 * @since 4.0
 */
public class PathAliasReplacementImpl implements PathAliasReplacement {
  private static boolean debug = false;

  public static List<PathAliasReplacement> makePathAliasReplacements(Map<String, String> aliases) {
    List<PathAliasReplacement> result = new ArrayList<>();

    for (Map.Entry<String, String> entry : aliases.entrySet()) {
      String value = entry.getValue();
      if (value == null || value.isEmpty()) continue;
      PathAliasReplacementImpl alias = new PathAliasReplacementImpl("${" + entry.getKey() + "}", value);
      result.add(alias);
      if (debug) System.out.printf("DataRootHandler alias= %s%n", alias);
    }
    return result;
  }

  private final String alias;
  private final String replacementPath;

  public PathAliasReplacementImpl(String alias, String replacementPath) {
    if (alias == null) throw new IllegalArgumentException("Alias must not be null.");
    if (replacementPath == null) throw new IllegalArgumentException("Replacment path must not be null.");

    //alias = StringUtils.cleanPath(alias);
    //replacementPath = StringUtils.cleanPath(replacementPath);

    // Make sure neither alias nor replacementPath ends with a slash ("/").
    this.alias = alias.endsWith("/") ? alias.substring(0, alias.length() - 1) : alias;
    this.replacementPath = replacementPath.endsWith("/") ? replacementPath.substring(0, replacementPath.length() - 1) : replacementPath;
  }

  public String getAlias() {
    return this.alias;
  }

  public String getReplacementPath() {
    return this.replacementPath;
  }

  @Override
  public boolean containsPathAlias(String path) {
    if (path == null) throw new IllegalArgumentException("Path must not be null.");
    //path = StringUtils.cleanPath(path);
    return path.startsWith(alias + "/");
  }

  @Override
  public String replacePathAlias(String path) {
    if (path == null) throw new IllegalArgumentException("Path must not be null.");
    //path = StringUtils.cleanPath(path);
    if (!path.startsWith(alias + "/"))
      throw new IllegalArgumentException("Path [" + path + "] does not contain alias [startWith( \"" + alias + "/\" )].");
    return replacementPath + path.substring(alias.length());
  }

  @Override
  public String replaceIfMatch(String path) {
    if (!path.startsWith(alias)) return null;
    return replacementPath + path.substring(alias.length());
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("StartsWithPathAliasReplacement{");
    sb.append("alias='").append(alias).append('\'');
    sb.append(", replacementPath='").append(replacementPath).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
