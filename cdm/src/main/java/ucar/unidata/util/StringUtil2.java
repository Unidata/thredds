/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.util;

import ucar.nc2.constants.CDM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Static String and StringBuilder utilities
 *
 * @author caron
 * @since 7/29/11
 */
public class StringUtil2 {

  /**
   * Replace any char not alphanumeric or in allowChars by replaceChar.
   *
   * @param x           operate on this string
   * @param allowChars  these are ok.
   * @param replaceChar thar char to replace
   * @return resulting string.
   */
  public static String allow(String x, String allowChars, char replaceChar) {
    boolean ok = true;
    for (int pos = 0; pos < x.length(); pos++) {
      char c = x.charAt(pos);
      if (!(Character.isLetterOrDigit(c) || (0 <= allowChars.indexOf(c)))) {
        ok = false;
        break;
      }
    }
    if (ok)
      return x;

    // gotta do it
    StringBuilder sb = new StringBuilder(x);
    for (int pos = 0; pos < sb.length(); pos++) {
      char c = sb.charAt(pos);
      if (Character.isLetterOrDigit(c) || (0 <= allowChars.indexOf(c))) {
        continue;
      }

      sb.setCharAt(pos, replaceChar);
    }

    return sb.toString();
  }

  /**
   * Break the given text into lines, respecting word boundaries (blank space).
   *
   * @param text     The text to convert
   * @param insert   break to insert
   * @param lineSize line size to insert at
   * @return The text with added br tags.
   */
  public static String breakTextAtWords(String text, String insert, int lineSize) {
    StringBuilder buff = new StringBuilder();
    StringTokenizer stoker = new StringTokenizer(text);
    int lineCount = 0;
    while (stoker.hasMoreTokens()) {
      String tok = stoker.nextToken();
      if (tok.length() + lineCount >= lineSize) {
        buff.append(insert);
        lineCount = 0;
      }
      buff.append(tok);
      buff.append(" ");
      lineCount += tok.length() + 1;
    }

    return buff.toString();
  }

  /**
   * Delete any non-printable characters
   *
   * @param h byte array
   * @return cleaned up string
   */
  public static String cleanup(byte[] h) {
    byte[] bb = new byte[h.length];
    int count = 0;
    for (byte b : h) {
      if (b >= 32 && b < 127)
        bb[count++] = b;
    }
    return new String(bb, 0, count, CDM.utf8Charset);
  }

  public static String cleanup(String s) {
    if (s == null) return null;
    return cleanup(s.getBytes(CDM.utf8Charset));
  }


  /**
   * Remove any char not alphanumeric or in okChars.
   *
   * @param x       filter this string
   * @param okChars these are ok.
   * @return filtered string.
   */
  public static String filter(String x, String okChars) {
    boolean ok = true;
    for (int pos = 0; pos < x.length(); pos++) {
      char c = x.charAt(pos);
      if (!(Character.isLetterOrDigit(c) || (0 <= okChars.indexOf(c)))) {
        ok = false;
        break;
      }
    }
    if (ok) {
      return x;
    }

    // gotta do it
    StringBuilder sb = new StringBuilder(x.length());
    for (int pos = 0; pos < x.length(); pos++) {
      char c = x.charAt(pos);
      if (Character.isLetterOrDigit(c) || (0 <= okChars.indexOf(c))) {
        sb.append(c);
      }
    }

    return sb.toString();
  }

  /**
   * Remove all but printable ascii
   *
   * @param s filter this string
   * @return filtered string.
   */
  public static String filter7bits(String s) {
    if (s == null) return null;
    char[] bo = new char[s.length()];
    int count = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if ((c < 128) && (c > 31) || ((c == '\n') || (c == '\t'))) {
        bo[count++] = c;
      }
    }

    return new String(bo, 0, count);
  }

  // remove leading and trailing blanks
  // remove control characters (< 0x20)
  // transform "/" to "_"
  // transform embedded space to "_"
  public static String makeValidCdmObjectName(String name) {
    name = name.trim();
    // common case no change
    boolean ok = true;
    for (int i = 0; i < name.length(); i++) {
      int c = name.charAt(i);
      if (c < 0x20) ok = false;
      if (c == '/') ok = false;
      if (c == ' ') ok = false;
      if (!ok) break;
    }
    if (ok) return name;

    StringBuilder sbuff = new StringBuilder(name.length());
    for (int i = 0, len = name.length(); i < len; i++) {
      int c = name.charAt(i);
      if ((c == '/') || (c == ' '))
        sbuff.append('_');
      else if (c >= 0x20)
        sbuff.append((char) c);
    }
    return sbuff.toString();
  }

  /**
   * Count number of chars that match in two strings, starting from front.
   *
   * @param s1 compare this string
   * @param s2 compare this string
   * @return number of matching chars, starting from first char
   */
  public static int match(String s1, String s2) {
    int i = 0;
    while ((i < s1.length()) && (i < s2.length())) {
      if (s1.charAt(i) != s2.charAt(i)) {
        break;
      }
      i++;
    }
    return i;
  }

  /**
   * Left pad the given value with zeros up to the number of digits
   *
   * @param value     The value.
   * @param numDigits number of digits
   * @return The String  represenation of the value, padded with
   * leading "0"-s if value &lt; 10E(numDigits-1)
   */
  public static String padZero(int value, int numDigits) {
    return padLeft(String.valueOf(value), numDigits, "0");
  }


  /**
   * Pad the given string with spaces on the left up to the given length.
   *
   * @param s             String to pad
   * @param desiredLength ending length
   * @return padded String
   */
  public static String padLeft(String s, int desiredLength) {
    return padLeft(s, desiredLength, " ");
  }


  /**
   * Pad the given string with padString on the left up to the given length.
   *
   * @param s             String to pad
   * @param desiredLength ending length
   * @param padString     String to pad with (e.g, " ")
   * @return padded String
   */
  public static String padLeft(String s, int desiredLength,
                               String padString) {
    while (s.length() < desiredLength) {
      s = padString + s;
    }
    return s;
  }


  /**
   * Pad the given string with spaces on the right up to the given length.
   *
   * @param s             String to pad
   * @param desiredLength ending length
   * @return padded String
   */
  public static String padRight(String s, int desiredLength) {
    return padRight(s, desiredLength, " ");
  }


  /**
   * Pad the given string with padString on the right up to the given length.
   *
   * @param s             String to pad
   * @param desiredLength ending length
   * @param padString     String to pad with (e.g, " ")
   * @return padded String
   */
  public static String padRight(String s, int desiredLength,
                                String padString) {
    StringBuilder ret = new StringBuilder(s);
    while (ret.length() < desiredLength) {
      ret.append(padString);
    }
    return ret.toString();
  }

  /**
   * Remove all occurrences of the substring sub in the string s.
   *
   * @param s   operate on this string
   * @param sub remove all occurrences of this substring.
   * @return result with substrings removed
   */
  public static String remove(String s, String sub) {
    int len = sub.length();
    int pos;
    while (0 <= (pos = s.indexOf(sub))) {
      s = s.substring(0, pos) + s.substring(pos + len);
    }
    return s;
  }

  /**
   * Remove all occurrences of the character c in the string s.
   *
   * @param s operate on this string
   * @param c remove all occurrences of this character.
   * @return result with any character c removed
   */
  public static String remove(String s, int c) {
    if (0 > s.indexOf(c)) {  // none
      return s;
    }

    StringBuilder buff = new StringBuilder(s);
    int i = 0;
    while (i < buff.length()) {
      if (buff.charAt(i) == c) {
        buff.deleteCharAt(i);
      } else {
        i++;
      }
    }
    return buff.toString();
  }

  /**
   * Remove all occurrences of the character c at the end of s.
   *
   * @param s operate on this string
   * @param c remove all occurrences of this character that are at the end of the string.
   * @return result with any character c removed
   */
  public static String removeFromEnd(String s, int c) {
    if (0 > s.indexOf(c))   // none
      return s;

    int len = s.length();
    while ((s.charAt(len - 1) == c) && (len > 0))
      len--;

    if (len == s.length())
      return s;
    return s.substring(0, len);
  }

  public static String removeFromEnd(String s, String suffix) {
    if (s.endsWith(suffix))
      return s.substring(0, s.length() - suffix.length());

    return s;
  }


  /**
   * Remove any whitespace (ie., Character.isWhitespace) from the input string.
   *
   * @param inputString The string to remove the whitespace.
   * @return The whitespaceless result.
   */
  public static String removeWhitespace(String inputString) {
    StringBuilder sb = new StringBuilder();
    char[] chars = inputString.toCharArray();
    for (char c : chars) {
      if (Character.isWhitespace(c)) {
        continue;
      }
      sb.append(c);
    }
    return sb.toString();
  }

  /**
   * Collapse continuous whitespace into one single " ".
   *
   * @param s operate on this string
   * @return result with collapsed whitespace
   */
  public static String collapseWhitespace(String s) {
    int len = s.length();
    StringBuilder b = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (!Character.isWhitespace(c)) {
        b.append(c);
      } else {
        b.append(' ');
        while ((i + 1 < len) && Character.isWhitespace(s.charAt(i + 1))) {
          i++;  /// skip further whitespace
        }
      }
    }
    return b.toString();
  }

  /**
   * Replace any char "out" in s with "in".
   *
   * @param s   string to replace
   * @param out replace this character
   * @param in  with this string
   * @return modified string if needed
   */
  public static String replace(String s, char out, String in) {
    if (s.indexOf(out) < 0) {
      return s;
    }

    // gotta do it
    StringBuilder sb = new StringBuilder(s);
    replace(sb, out, in);
    return sb.toString();
  }


  /**
   * Replace all occurrences of any char in replaceChar with corresponding String in replaceWith
   *
   * @param x           operate on this string
   * @param replaceChar get rid of these
   * @param replaceWith replace with these
   * @return resulting string
   */
  public static String replace(String x, char[] replaceChar, String[] replaceWith) {
    // common case no replacement
    boolean ok = true;
    for (char aReplaceChar : replaceChar) {
      int pos = x.indexOf(aReplaceChar);
      ok = (pos < 0);
      if (!ok)
        break;
    }
    if (ok)
      return x;

    // gotta do it
    StringBuilder sb = new StringBuilder(x);
    for (int i = 0; i < replaceChar.length; i++) {
      int pos = x.indexOf(replaceChar[i]);
      if (pos >= 0) {
        replace(sb, replaceChar[i], replaceWith[i]);
      }
    }

    return sb.toString();
  }

  /**
   * Replaces all occurrences of "pattern" in "string" with "value"
   *
   * @param string  string to munge
   * @param pattern pattern to replace
   * @param value   replacement value
   * @return munged string
   */
  public static String replace(String string, String pattern, String value) {
    if (pattern.length() == 0)
      return string;

    if (!string.contains(pattern)) return string;

    // ok gotta do it
    StringBuilder returnValue = new StringBuilder();
    int patternLength = pattern.length();
    while (true) {
      int idx = string.indexOf(pattern);
      if (idx < 0)
        break;

      returnValue.append(string, 0, idx);
      if (value != null)
        returnValue.append(value);

      string = string.substring(idx + patternLength);
    }
    returnValue.append(string);
    return returnValue.toString();
  }


  /**
   * Replace all occurrences of orgReplace with orgChar; inverse of replace().
   *
   * @param x          operate on this string
   * @param orgReplace get rid of these
   * @param orgChar    replace with these
   * @return resulting string
   */
  public static String unreplace(String x, String[] orgReplace, char[] orgChar) {
    // common case no replacement
    boolean ok = true;
    for (String anOrgReplace : orgReplace) {
      int pos = x.indexOf(anOrgReplace);
      ok = (pos < 0);
      if (!ok) break;
    }
    if (ok)
      return x;

    // gotta do it
    StringBuilder result = new StringBuilder(x);
    for (int i = 0; i < orgReplace.length; i++) {
      int pos = result.indexOf(orgReplace[i]);
      if (pos >= 0) {
        unreplace(result, orgReplace[i], orgChar[i]);
      }
    }

    return result.toString();
  }

  /**
   * Find all occurrences of the "match" in original, and substitute the "subst" string.
   *
   * @param original starting string
   * @param match    string to match
   * @param subst    string to substitute
   * @return a new string with substitutions
   */
  public static String substitute(String original, String match, String subst) {
    String s = original;
    int pos;
    while (0 <= (pos = s.indexOf(match))) {
      StringBuilder sb = new StringBuilder(s);
      s = sb.replace(pos, pos + match.length(), subst).toString();
    }
    return s;
  }

  /**
   * Escape any char not alphanumeric or in okChars.
   * Escape by replacing char with %xx (hex).
   *
   * @param x       escape this string
   * @param okChars these are ok.
   * @return equivilent escaped string.
   */
  public static String escape(String x, String okChars) {
    StringBuilder newname = new StringBuilder();
    for (char c : x.toCharArray()) {
      if (c == '%') {
        newname.append("%%");
      } else if (!Character.isLetterOrDigit(c) && okChars.indexOf(c) < 0) {
        newname.append('%');
        newname.append(Integer.toHexString((0xFF & (int) c)));
      } else
        newname.append(c);
    }
    return newname.toString();
  }

  /**
   * This finds any '%xx' and converts to the equivalent char. Inverse of
   * escape().
   *
   * @param x operate on this String
   * @return original String.
   */
  public static String unescape(String x) {
    if (x.indexOf('%') < 0) {
      return x;
    }

    // gotta do it
    char[] b = new char[2];
    StringBuilder sb = new StringBuilder(x);
    for (int pos = 0; pos < sb.length(); pos++) {
      char c = sb.charAt(pos);
      if (c != '%') {
        continue;
      }
      if (pos >= sb.length() - 2) { // malformed - should be %xx
        return x;
      }
      b[0] = sb.charAt(pos + 1);
      b[1] = sb.charAt(pos + 2);
      int value;
      try {
        value = Integer.parseInt(new String(b), 16);
      } catch (NumberFormatException e) {
        continue;   // not a hex number
      }
      c = (char) value;
      sb.setCharAt(pos, c);
      sb.delete(pos + 1, pos + 3);
    }

    return sb.toString();
  }

  /**
   * Split a string on one or more whitespace.
   * Cover for String.split, because who can remember regexp?
   *
   * @param source split this string
   * @return tokens that were seperated by whitespace
   */
  public static String[] splitString(String source) {
    return source.trim().split("\\s+"); // Separated by "whitespace"
  }

  /**
   * Find all occurences of match strings in original, and substitute the corresponding
   * subst string.
   *
   * @param original starting string
   * @param match    array of strings to match
   * @param subst    array of strings to substitute
   * @return a new string with substitutions
   */
  public static String substitute(String original, String[] match, String[] subst) {

    boolean ok = true;
    for (String aMatch : match) {
      if (original.contains(aMatch)) {
        ok = false;
        break;
      }
    }
    if (ok) {
      return original;
    }

    // gotta do it;
    StringBuilder sb = new StringBuilder(original);
    for (int i = 0; i < match.length; i++) {
      substitute(sb, match[i], subst[i]);
    }
    return sb.toString();
  }

  public static List<String> getTokens(String fullString, String sep) throws Exception {

    List<String> strs = new ArrayList<>();
    if (sep != null) {
      int sepLength = sep.length();
      switch (sepLength) {
        case 0:
          String[] tokens = splitString(fullString);   // default to use white space if separator is ""
          strs = Arrays.asList(tokens);
          break;

        case 1:
          StringTokenizer tokenizer = new StringTokenizer(fullString, sep);       // maybe use StreamTokenizer?
          while (tokenizer.hasMoreTokens())
            strs.add(tokenizer.nextToken());
          break;

        default:
          String remainderString = fullString;    // multicharacter separator
          int location = remainderString.indexOf(sep);
          while (location != -1) {    // watch out for off-by-one errors on the string splitting indices!!!
            if (location == 0) {    // remainderString starts with the separator, cut it off
              remainderString = remainderString.substring(location + sepLength);
              location = remainderString.indexOf(sep);
            } else {
              String token = remainderString.substring(0, location); // pull the token off the front of the string
              strs.add(token);    // add the token to our list
              remainderString = remainderString.substring(location + sepLength); // cut out both the token and the separator
              location = remainderString.indexOf(sep);
            }
          }  //close while loop
          if (remainderString.length() > 0) strs.add(remainderString);    // add the last token, post last separator
      }  //close switch (sepLength)
    } else {  // default to white space separator if sep is null
      String[] tokens = splitString(fullString);
      strs = Arrays.asList(tokens);
    }
    if (strs.size() == 0) strs.add(""); // maybe thrown an exception instead?  return null?
    return strs;
  }

  ////////////////////////////////////////////////////
  // StringBuilder

  /**
   * Remove any of the characters in out from sb
   *
   * @param sb  the StringBuilder
   * @param out get rid of any of these characters
   */
  public static void removeAll(StringBuilder sb, String out) {
    int i = 0;
    while (i < sb.length()) {
      int c = sb.charAt(i);
      boolean ok = true;
      for (int j = 0; j < out.length(); j++) {
        if (out.charAt(j) == c) {
          sb.delete(i, i + 1);
          ok = false;
          break;
        }
      }
      if (ok) i++;
    }
  }

  /**
   * Replace any char "out" in sb with String "in".
   *
   * @param sb  StringBuilder to replace
   * @param out repalce this character
   * @param in  with this string
   */
  public static void replace(StringBuilder sb, char out, String in) {
    for (int i = 0; i < sb.length(); i++) {
      if (sb.charAt(i) == out) {
        sb.replace(i, i + 1, in);
        i += in.length() - 1;
      }
    }
  }

  /**
   * Replace any String "out" in sb with char "in".
   *
   * @param sb  StringBuilder to replace
   * @param out repalce this String
   * @param in  with this char
   */
  public static void unreplace(StringBuilder sb, String out, char in) {
    int pos;
    while (0 <= (pos = sb.indexOf(out))) {
      sb.setCharAt(pos, in);
      sb.delete(pos + 1, pos + out.length());
    }
  }

  /**
   * Replace any of the characters from out with corresponding character from in
   *
   * @param sb  the StringBuilder
   * @param out get rid of any of these characters
   * @param in  replacing with the character at same index
   */
  public static void replace(StringBuilder sb, String out, String in) {
    for (int i = 0; i < sb.length(); i++) {
      int c = sb.charAt(i);
      for (int j = 0; j < out.length(); j++) {
        if (out.charAt(j) == c)
          sb.setCharAt(i, in.charAt(j));
      }
    }
  }


  /**
   * Find all occurences of the "match" in original, and substitute the "subst" string,
   * directly into the original.
   *
   * @param sbuff starting string buffer
   * @param match string to match
   * @param subst string to substitute
   */
  public static void substitute(StringBuilder sbuff, String match, String subst) {
    int pos, fromIndex = 0;
    int substLen = subst.length();
    int matchLen = match.length();
    while (0 <= (pos = sbuff.indexOf(match, fromIndex))) {
      sbuff.replace(pos, pos + matchLen, subst);
      fromIndex = pos + substLen;  // make sure dont get into an infinite loop
    }
  }

  /**
   * Remove bad char from beginning or end of string
   *
   * @param s operate on this
   * @return trimmed string
   */

  public static String trim(String s, int bad) {
    int len = s.length();
    int st = 0;

    while ((st < len) && (s.charAt(st) == bad)) {
      st++;
    }
    while ((st < len) && (s.charAt(len - 1) == bad)) {
      len--;
    }
    return ((st > 0) || (len < s.length())) ? s.substring(st, len) : s;
  }

  //////////////////////////////////////////////////////////////////////

  private static final char[] htmlIn = {'&', '"', '\'', '<', '>', '\n'};
  private static final String[] htmlOut = {"&amp;", "&quot;", "&#39;", "&lt;", "&gt;", "\n<p>"};

  /**
   * @deprecated legacy only, use HtmlEscapers.htmlEscaper()
   */
  public static String quoteHtmlContent(String x) {
    if (x == null) return null;
    return replace(x, htmlIn, htmlOut);
  }
}
