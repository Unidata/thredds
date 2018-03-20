/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * NOT USED
 *
 * PathMatcher implementation for Ant-style path patterns.
 * Examples are provided below.
 * <p/>
 * <p>Part of this mapping code has been kindly borrowed from
 * <a href="http://ant.apache.org">Apache Ant</a>.
 * <p/>
 * <p>The mapping matches URLs using the following rules:<br>
 * <ul>
 * <li>? matches one character</li>
 * <li>* matches zero or more characters</li>
 * <li>** matches zero or more 'directories' in a path</li>
 * </ul>
 * <p/>
 * <p>Some examples:<br>
 * <ul>
 * <li><code>com/t?st.jsp</code> - matches <code>com/test.jsp</code> but also
 * <code>com/tast.jsp</code> or <code>com/txst.jsp</code></li>
 * <li><code>com/*.jsp</code> - matches all <code>.jsp</code> files in the
 * <code>com</code> directory</li>
 * <li><code>com/&#42;&#42;/test.jsp</code> - matches all <code>test.jsp</code>
 * files underneath the <code>com</code> path</li>
 * <li><code>org/springframework/&#42;&#42;/*.jsp</code> - matches all <code>.jsp</code>
 * files underneath the <code>org/springframework</code> path</li>
 * <li><code>org/&#42;&#42;/servlet/bla.jsp</code> - matches
 * <code>org/springframework/servlet/bla.jsp</code> but also
 * <code>org/springframework/testing/servlet/bla.jsp</code> and
 * <code>org/servlet/bla.jsp</code></li>
 * </ul>
 * <p/>
 * Original code taken from spring 2.5.6:
 * org.springframework.util.AntPathMatcher.java
 * org.springframework.util.StringUtils.java
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 16.07.2003
 */
public class AntPathMatcher {

  /**
   * Default path separator: "/"
   */
  public static final String DEFAULT_PATH_SEPARATOR = "/";

  private String pathSeparator = DEFAULT_PATH_SEPARATOR;


  /**
   * Set the path separator to use for pattern parsing.
   * Default is "/", as in Ant.
   */
  public void setPathSeparator(String pathSeparator) {
    this.pathSeparator = (pathSeparator != null ? pathSeparator : DEFAULT_PATH_SEPARATOR);
  }


  public boolean isPattern(String path) {
    return (path.indexOf('*') != -1 || path.indexOf('?') != -1);
  }

  public boolean match(String pattern, String path) {
    return doMatch(pattern, path, true);
  }

  public boolean matchStart(String pattern, String path) {
    return doMatch(pattern, path, false);
  }


  /**
   * Actually match the given <code>path</code> against the given <code>pattern</code>.
   *
   * @param pattern   the pattern to match against
   * @param path      the path String to test
   * @param fullMatch whether a full pattern match is required
   *                  (else a pattern match as far as the given base path goes is sufficient)
   * @return <code>true</code> if the supplied <code>path</code> matched,
   *         <code>false</code> if it didn't
   */
  protected boolean doMatch(String pattern, String path, boolean fullMatch) {
    if (path.startsWith(this.pathSeparator) != pattern.startsWith(this.pathSeparator)) {
      return false;
    }

    String[] pattDirs = tokenizeToStringArray(pattern, this.pathSeparator);
    String[] pathDirs = tokenizeToStringArray(path, this.pathSeparator);

    int pattIdxStart = 0;
    int pattIdxEnd = pattDirs.length - 1;
    int pathIdxStart = 0;
    int pathIdxEnd = pathDirs.length - 1;

    // Match all elements up to the first **
    while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
      String patDir = pattDirs[pattIdxStart];
      if ("**".equals(patDir)) {
        break;
      }
      if (!matchStrings(patDir, pathDirs[pathIdxStart])) {
        return false;
      }
      pattIdxStart++;
      pathIdxStart++;
    }

    if (pathIdxStart > pathIdxEnd) {
      // Path is exhausted, only match if rest of pattern is * or **'s
      if (pattIdxStart > pattIdxEnd) {
        return (pattern.endsWith(this.pathSeparator) ?
                path.endsWith(this.pathSeparator) : !path.endsWith(this.pathSeparator));
      }
      if (!fullMatch) {
        return true;
      }
      if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*") &&
              path.endsWith(this.pathSeparator)) {
        return true;
      }
      for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
        if (!pattDirs[i].equals("**")) {
          return false;
        }
      }
      return true;
    } else if (pattIdxStart > pattIdxEnd) {
      // String not exhausted, but pattern is. Failure.
      return false;
    } else if (!fullMatch && "**".equals(pattDirs[pattIdxStart])) {
      // Path start definitely matches due to "**" part in pattern.
      return true;
    }

    // up to last '**'
    while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
      String patDir = pattDirs[pattIdxEnd];
      if (patDir.equals("**")) {
        break;
      }
      if (!matchStrings(patDir, pathDirs[pathIdxEnd])) {
        return false;
      }
      pattIdxEnd--;
      pathIdxEnd--;
    }
    if (pathIdxStart > pathIdxEnd) {
      // String is exhausted
      for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
        if (!pattDirs[i].equals("**")) {
          return false;
        }
      }
      return true;
    }

    while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
      int patIdxTmp = -1;
      for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
        if (pattDirs[i].equals("**")) {
          patIdxTmp = i;
          break;
        }
      }
      if (patIdxTmp == pattIdxStart + 1) {
        // '**/**' situation, so skip one
        pattIdxStart++;
        continue;
      }
      // Find the pattern between padIdxStart & padIdxTmp in str between
      // strIdxStart & strIdxEnd
      int patLength = (patIdxTmp - pattIdxStart - 1);
      int strLength = (pathIdxEnd - pathIdxStart + 1);
      int foundIdx = -1;

      strLoop:
      for (int i = 0; i <= strLength - patLength; i++) {
        for (int j = 0; j < patLength; j++) {
          String subPat = (String) pattDirs[pattIdxStart + j + 1];
          String subStr = (String) pathDirs[pathIdxStart + i + j];
          if (!matchStrings(subPat, subStr)) {
            continue strLoop;
          }
        }
        foundIdx = pathIdxStart + i;
        break;
      }

      if (foundIdx == -1) {
        return false;
      }

      pattIdxStart = patIdxTmp;
      pathIdxStart = foundIdx + patLength;
    }

    for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
      if (!pattDirs[i].equals("**")) {
        return false;
      }
    }

    return true;
  }

  /**
   * Tests whether or not a string matches against a pattern.
   * The pattern may contain two special characters:<br>
   * '*' means zero or more characters<br>
   * '?' means one and only one character
   *
   * @param pattern pattern to match against.
   *                Must not be <code>null</code>.
   * @param str     string which must be matched against the pattern.
   *                Must not be <code>null</code>.
   * @return <code>true</code> if the string matches against the
   *         pattern, or <code>false</code> otherwise.
   */
  private boolean matchStrings(String pattern, String str) {
    char[] patArr = pattern.toCharArray();
    char[] strArr = str.toCharArray();
    int patIdxStart = 0;
    int patIdxEnd = patArr.length - 1;
    int strIdxStart = 0;
    int strIdxEnd = strArr.length - 1;
    char ch;

    boolean containsStar = false;
    for (int i = 0; i < patArr.length; i++) {
      if (patArr[i] == '*') {
        containsStar = true;
        break;
      }
    }

    if (!containsStar) {
      // No '*'s, so we make a shortcut
      if (patIdxEnd != strIdxEnd) {
        return false; // Pattern and string do not have the same size
      }
      for (int i = 0; i <= patIdxEnd; i++) {
        ch = patArr[i];
        if (ch != '?') {
          if (ch != strArr[i]) {
            return false;// Character mismatch
          }
        }
      }
      return true; // String matches against pattern
    }


    if (patIdxEnd == 0) {
      return true; // Pattern contains only '*', which matches anything
    }

    // Process characters before first star
    while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
      if (ch != '?') {
        if (ch != strArr[strIdxStart]) {
          return false;// Character mismatch
        }
      }
      patIdxStart++;
      strIdxStart++;
    }
    if (strIdxStart > strIdxEnd) {
      // All characters in the string are used. Check if only '*'s are
      // left in the pattern. If so, we succeeded. Otherwise failure.
      for (int i = patIdxStart; i <= patIdxEnd; i++) {
        if (patArr[i] != '*') {
          return false;
        }
      }
      return true;
    }

    // Process characters after last star
    while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
      if (ch != '?') {
        if (ch != strArr[strIdxEnd]) {
          return false;// Character mismatch
        }
      }
      patIdxEnd--;
      strIdxEnd--;
    }
    if (strIdxStart > strIdxEnd) {
      // All characters in the string are used. Check if only '*'s are
      // left in the pattern. If so, we succeeded. Otherwise failure.
      for (int i = patIdxStart; i <= patIdxEnd; i++) {
        if (patArr[i] != '*') {
          return false;
        }
      }
      return true;
    }

    // process pattern between stars. padIdxStart and patIdxEnd point
    // always to a '*'.
    while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
      int patIdxTmp = -1;
      for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
        if (patArr[i] == '*') {
          patIdxTmp = i;
          break;
        }
      }
      if (patIdxTmp == patIdxStart + 1) {
        // Two stars next to each other, skip the first one.
        patIdxStart++;
        continue;
      }
      // Find the pattern between padIdxStart & padIdxTmp in str between
      // strIdxStart & strIdxEnd
      int patLength = (patIdxTmp - patIdxStart - 1);
      int strLength = (strIdxEnd - strIdxStart + 1);
      int foundIdx = -1;
      strLoop:
      for (int i = 0; i <= strLength - patLength; i++) {
        for (int j = 0; j < patLength; j++) {
          ch = patArr[patIdxStart + j + 1];
          if (ch != '?') {
            if (ch != strArr[strIdxStart + i + j]) {
              continue strLoop;
            }
          }
        }

        foundIdx = strIdxStart + i;
        break;
      }

      if (foundIdx == -1) {
        return false;
      }

      patIdxStart = patIdxTmp;
      strIdxStart = foundIdx + patLength;
    }

    // All characters in the string are used. Check if only '*'s are left
    // in the pattern. If so, we succeeded. Otherwise failure.
    for (int i = patIdxStart; i <= patIdxEnd; i++) {
      if (patArr[i] != '*') {
        return false;
      }
    }

    return true;
  }

  /**
   * Given a pattern and a full path, determine the pattern-mapped part.
   * <p>For example:
   * <ul>
   * <li>'<code>/docs/cvs/commit.html</code>' and '<code>/docs/cvs/commit.html</code> -> ''</li>
   * <li>'<code>/docs/*</code>' and '<code>/docs/cvs/commit</code> -> '<code>cvs/commit</code>'</li>
   * <li>'<code>/docs/cvs/*.html</code>' and '<code>/docs/cvs/commit.html</code> -> '<code>commit.html</code>'</li>
   * <li>'<code>/docs/**</code>' and '<code>/docs/cvs/commit</code> -> '<code>cvs/commit</code>'</li>
   * <li>'<code>/docs/**\/*.html</code>' and '<code>/docs/cvs/commit.html</code> -> '<code>cvs/commit.html</code>'</li>
   * <li>'<code>/*.html</code>' and '<code>/docs/cvs/commit.html</code> -> '<code>docs/cvs/commit.html</code>'</li>
   * <li>'<code>*.html</code>' and '<code>/docs/cvs/commit.html</code> -> '<code>/docs/cvs/commit.html</code>'</li>
   * <li>'<code>*</code>' and '<code>/docs/cvs/commit.html</code> -> '<code>/docs/cvs/commit.html</code>'</li>
   * </ul>
   * <p>Assumes that {@link #match} returns <code>true</code> for '<code>pattern</code>'
   * and '<code>path</code>', but does <strong>not</strong> enforce this.
   */
  public String extractPathWithinPattern(String pattern, String path) {
    String[] patternParts = tokenizeToStringArray(pattern, this.pathSeparator);
    String[] pathParts = tokenizeToStringArray(path, this.pathSeparator);

    StringBuffer buffer = new StringBuffer();

    // Add any path parts that have a wildcarded pattern part.
    int puts = 0;
    for (int i = 0; i < patternParts.length; i++) {
      String patternPart = patternParts[i];
      if ((patternPart.indexOf('*') > -1 || patternPart.indexOf('?') > -1) && pathParts.length >= i + 1) {
        if (puts > 0 || (i == 0 && !pattern.startsWith(this.pathSeparator))) {
          buffer.append(this.pathSeparator);
        }
        buffer.append(pathParts[i]);
        puts++;
      }
    }

    // Append any trailing path parts.
    for (int i = patternParts.length; i < pathParts.length; i++) {
      if (puts > 0 || i > 0) {
        buffer.append(this.pathSeparator);
      }
      buffer.append(pathParts[i]);
    }

    return buffer.toString();
  }

  /**
   * Tokenize the given String into a String array via a StringTokenizer.
   * Trims tokens and omits empty tokens.
   * <p>The given delimiters string is supposed to consist of any number of
   * delimiter characters. Each of those characters can be used to separate
   * tokens. A delimiter is always a single character; for multi-character
   * delimiters, consider using <code>delimitedListToStringArray</code>
   *
   * @param str        the String to tokenize
   * @param delimiters the delimiter characters, assembled as String
   *                   (each of those characters is individually considered as delimiter).
   * @return an array of the tokens
   * @see java.util.StringTokenizer
   * @see java.lang.String#trim()
   */
  public static String[] tokenizeToStringArray(String str, String delimiters) {
    return tokenizeToStringArray(str, delimiters, true, true);
  }

  /**
   * Tokenize the given String into a String array via a StringTokenizer.
   * <p>The given delimiters string is supposed to consist of any number of
   * delimiter characters. Each of those characters can be used to separate
   * tokens. A delimiter is always a single character; for multi-character
   * delimiters, consider using <code>delimitedListToStringArray</code>
   *
   * @param str               the String to tokenize
   * @param delimiters        the delimiter characters, assembled as String
   *                          (each of those characters is individually considered as delimiter)
   * @param trimTokens        trim the tokens via String's <code>trim</code>
   * @param ignoreEmptyTokens omit empty tokens from the result array
   *                          (only applies to tokens that are empty after trimming; StringTokenizer
   *                          will not consider subsequent delimiters as token in the first place).
   * @return an array of the tokens (<code>null</code> if the input String
   *         was <code>null</code>)
   * @see java.util.StringTokenizer
   * @see java.lang.String#trim()
   */
  public static String[] tokenizeToStringArray(
          String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

    if (str == null) {
      return null;
    }
    StringTokenizer st = new StringTokenizer(str, delimiters);
    List tokens = new ArrayList();
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (trimTokens) {
        token = token.trim();
      }
      if (!ignoreEmptyTokens || token.length() > 0) {
        tokens.add(token);
      }
    }
    return toStringArray(tokens);
  }

  /**
   * Copy the given Collection into a String array.
   * The Collection must contain String elements only.
   *
   * @param collection the Collection to copy
   * @return the String array (<code>null</code> if the passed-in
   *         Collection was <code>null</code>)
   */
  public static String[] toStringArray(Collection collection) {
    if (collection == null) {
      return null;
    }
    return (String[]) collection.toArray(new String[collection.size()]);
  }
}
