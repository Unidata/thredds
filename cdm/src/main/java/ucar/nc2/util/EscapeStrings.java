/* Copyright Unidata */

package ucar.nc2.util;

import ucar.nc2.constants.CDM;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
  We want to refactor the escaping.
  1. EscapeStrings (this class)
  2. StringUtil2.escape() DONE
  3. URLnaming DONE
  4. Eliminate URLencode/decode in favor of guava.

  https://texnoblog.wordpress.com/2014/06/11/urlencode-just-one-is-not-enough/
  "Starting from version 15.0, Google's excellent Guava libraries have UrlEscapers class to do just that!
  The documentation is pretty clear, use
    1) urlPathSegmentEscaper to encode URL path segments (things that go between slashes),
    2) urlFragmentEscaper if you already have a path/with/slashes
    3) urlPathSegmentEscaper for the names and values of request parameters (things after the '?').

  from Guava:

  1) HtmlEscapers
  HTML escaping is particularly tricky: For example, some elements' text contents must not be HTML escaped.
  As a result, it is impossible to escape an HTML document correctly without domain-specific knowledge beyond what HtmlEscapers provides.
  We strongly encourage the use of HTML templating systems.

  2) XMLEscaper
  Escaper instances suitable for strings to be included in XML attribute values and elements' text contents. When possible, avoid manual escaping
  by using templating systems and high-level APIs that provide autoescaping. For example, consider XOM or JDOM.

  3) PercentEscaper
  PercentEscaper(String safeChars, boolean plusForSpace)
  Constructs a percent escaper with the specified safe characters and optional handling of the space character.

 */

public class EscapeStrings {
  static protected final String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  static protected final String numeric = "0123456789";
  static protected final String alphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  static protected final String _allowableInUrl = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$&'()*+,-./:;=?@_~";
  static protected final String _allowableInUrlQuery = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$%&'()*+,-./:;=?@_~";
  static protected final String _allowableInDAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_!~*'-\"./";
  static protected final String _allowableInOGC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.!~*'()";
  static protected final char _URIEscape = '%';
  static protected final char _JavaEscape = '\\';
  static final byte blank = ((byte) ' ');
  static final byte plus = ((byte) '+');

  /**
   *
   * @param in   String to escape
   * @param allowable  allowedcharacters
   * @param esc   escape char prefix
   * @param spaceplus  true =>convert ' ' to '+'
     * @return
     */
    private static String xescapeString(String in, String allowable, char esc, boolean spaceplus) {
    try {
      StringBuffer out = new StringBuffer();
      if (in == null) return null;
      byte[] utf8 = in.getBytes(CDM.utf8Charset);
      byte[] allow8 = allowable.getBytes(CDM.utf8Charset);
      for (byte b : utf8) {
        if (b == blank && spaceplus) {
          out.append('+');
        } else {
          // search allow8
          boolean found = false;
          for (byte a : allow8) {
            if (a == b) {
              found = true;
              break;
            }
          }
          if (found) {
            out.append((char) b);
          } else {
            String c = Integer.toHexString(b);
            out.append(esc);
            if (c.length() < 2) out.append('0');
            out.append(c);
          }
        }
      }

      return out.toString();

    } catch (Exception e) {
      return in;
    }
  }

  private static String escapeString(String in, String allowable) {
    return xescapeString(in, allowable, _URIEscape, false);
  }

  /**
   * Given a string that contains WWW escape sequences, translate those escape
   * sequences back into ASCII characters. Return the modified string.
   *
   * @param in        The string to modify.
   * @param escape    The character used to signal the begining of an escape sequence.
   *                  param except If there is some escape code that should not be removed by
   *                  this call (e.g., you might not want to remove spaces, %20) use this
   *                  parameter to specify that code. The function will then transform all
   *                  escapes except that one.
   * @param spaceplus True if spaces should be replaced by '+'.
   * @return The modified string.
   */

  private static String xunescapeString(String in, char escape, boolean spaceplus) {
    try {
      if (in == null) return null;

      byte[] utf8 = in.getBytes(CDM.utf8Charset);
      byte escape8 = (byte) escape;
      byte[] out = new byte[utf8.length]; // Should be max we need

      int index8 = 0;
      for (int i = 0; i < utf8.length; ) {
        byte b = utf8[i++];
        if (b == plus && spaceplus) {
          out[index8++] = blank;
        } else if (b == escape8) {
          // check to see if there are enough characters left
          if (i + 2 <= utf8.length) {
            b = (byte) (fromHex(utf8[i]) << 4 | fromHex(utf8[i + 1]));
            i += 2;
          }
        }
        out[index8++] = b;
      }
      return new String(out, 0, index8, CDM.utf8Charset);
    } catch (Exception e) {
      return in;
    }

  }

  private static String unescapeString(String in) {
    return xunescapeString(in, _URIEscape, false);
  }

  static final byte hexa = (byte) 'a';
  static final byte hexf = (byte) 'f';
  static final byte hexA = (byte) 'A';
  static final byte hexF = (byte) 'F';
  static final byte hex0 = (byte) '0';
  static final byte hex9 = (byte) '9';
  static final byte ten = (byte) 10;


  private static byte fromHex(byte b) throws NumberFormatException {
    if (b >= hex0 && b <= hex9) return (byte) (b - hex0);
    if (b >= hexa && b <= hexf) return (byte) (ten + (b - hexa));
    if (b >= hexA && b <= hexF) return (byte) (ten + (b - hexA));
    throw new NumberFormatException("Illegal hex character: " + b);
  }


  /**
   * Define the DEFINITIVE opendap identifier unescape function.
   *
   * @param id The identifier to unescape.
   * @return The unescaped identifier.
   */
  public static String unescapeDAPIdentifier(String id) {
    String s;
    try {
      s = unescapeString(id);
    } catch (Exception e) {
      s = null;
    }
    return s;
  }

  static private final Pattern p = Pattern.compile("([\\w]+)://([.\\w]+(:[\\d]+)?)([/][^?#])?([?][^#]*)?([#].*)?");

  public static String escapeURL(String url) {
    String protocol;
    String authority;
    String path;
    String query;
    String fragment;
    if (false) {
      // We split the url ourselves to minimize character dependencies
      Matcher m = p.matcher(url);
      boolean match = m.matches();
      if (!match) return null;
      protocol = m.group(1);
      authority = m.group(2);
      path = m.group(3);
      query = m.group(4);
      fragment = m.group(5);
    } else {// faster, but may not work quite right
      URL u;
      try {
        u = new URL(url);
      } catch (MalformedURLException e) {
        return null;
      }
      protocol = u.getProtocol();
      authority = u.getAuthority();
      path = u.getPath();
      query = u.getQuery();
      fragment = u.getRef();
    }
    // Reassemble
    StringBuilder ret = new StringBuilder(protocol);
    ret.append("://");
    ret.append(authority);
    if (path != null && path.length() > 0) {
      // Encode pieces between '/'
      String pieces[] = path.split("[/]", -1);
      for (int i = 0; i < pieces.length; i++) {
        String p = pieces[i];
        if (p == null) p = "";
        if (i > 0) ret.append("/");
        ret.append(urlEncode(p));
      }
    }
    if (query != null && query.length() > 0) {
      ret.append("?");
      ret.append(escapeURLQuery(query));
    }
    if (fragment != null && fragment.length() > 0) {
      ret.append("#");
      ret.append(urlEncode(fragment));
    }
    return ret.toString();
  }

  /**
   * Define the DEFINITIVE URL constraint expression escape function.
   *
   * @param ce The expression to modify.
   * @return The escaped expression.
   */
  public static String escapeURLQuery(String ce) {
    try {
      ce = escapeString(ce, _allowableInUrlQuery);
    } catch (Exception e) {
      ce = null;
    }
    return ce;
  }

  /**
   * Define the DEFINITIVE URL constraint expression unescape function.
   *
   * @param ce The expression to unescape.
   * @return The unescaped expression.
   */
  public static String unescapeURLQuery(String ce) {
    try {
      ce = unescapeString(ce);
    } catch (Exception e) {
      ce = null;
    }
    return ce;
  }

  /**
   * Define the DEFINITIVE URL escape function.
   * Note that the whole string is escaped, so
   * be careful what you pass into this procedure.
   *
   * @param s The string to modify.
   * @return The escaped expression.
   */
  private static String urlEncode(String s) {
    //try {s = URLEncoder.encode(s,"UTF-8");} catch(Exception e) {s = null;}
    s = escapeString(s, _allowableInUrl);
    return s;
  }

  /**
   * Define the DEFINITIVE URL unescape function.
   *
   * @param s The string to unescape.
   * @return The unescaped expression.
   */
  public static String urlDecode(String s) {
    try {
      //s = unescapeString(s, _URIEscape, "", false);
      s = URLDecoder.decode(s, "UTF-8");
    } catch (Exception e) {
      s = null;
    }
    return s;
  }

  /**
   * Decode all of the parts of the url including query and fragment
   *
   * @param url the url to encode
   */
  public static String unescapeURL(String url) {
    String newurl;
    newurl = urlDecode(url);
    return newurl;
  }


  /**
   * Define the DAP escape identifier function.
   *
   * @param s The string to encode.
   * @return The escaped string.
   */
  public static String escapeDAPIdentifier(String s) {
    return escapeString(s, _allowableInDAP);
  }

  /**
   * Define the OGC Web Services escape function.
   *
   * @param s The string to encode.
   * @return The escaped string.
   */
  public static String escapeOGC(String s) {
    return escapeString(s, _allowableInOGC);
  }

  static public void testOGC() {
    for (char c : (alphaNumeric + " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~").toCharArray()) {
      String encoded = EscapeStrings.escapeOGC("" + c);
      System.err.printf("|%c|=|%s|%n", c, encoded);
    }
  }


  ///////////////////////////////////////////////////////////////

  /**
   * backslash escape a string
   *
   * @param x             escape this; may be null
   * @param reservedChars these chars get a backslash in front of them
   * @return escaped string
   */
  static public String backslashEscape(String x, String reservedChars) {
    if (x == null) {
      return null;
    } else if (reservedChars == null) {
      return x;
    }

    boolean ok = true;
    for (int pos = 0; pos < x.length(); pos++) {
      char c = x.charAt(pos);
      if (reservedChars.indexOf(c) >= 0) {
        ok = false;
        break;
      }
    }
    if (ok) return x;

    // gotta do it
    StringBuilder sb = new StringBuilder(x);
    for (int pos = 0; pos < sb.length(); pos++) {
      char c = sb.charAt(pos);
      if (reservedChars.indexOf(c) < 0) {
        continue;
      }

      sb.setCharAt(pos, '\\');
      pos++;
      sb.insert(pos, c);
      pos++;
    }

    return sb.toString();
  }

  /**
   * backslash unescape a string
   *
   * @param x unescape this
   * @return string with \c -> c
   */
  static public String backslashUnescape(String x) {
    if (!x.contains("\\")) return x;

    // gotta do it
    StringBuilder sb = new StringBuilder(x.length());
    for (int pos = 0; pos < x.length(); pos++) {
      char c = x.charAt(pos);
      if (c == '\\') {
        c = x.charAt(++pos); // skip backslash, get next cha
      }
      sb.append(c);
    }

    return sb.toString();
  }


  /**
   * Tokenize an escaped name using "." as delimiter, skipping "\."
   *
   * @param escapedName an escaped name
   * @return list of tokens
   */
  public static List<String> tokenizeEscapedName(String escapedName) {
    List<String> result = new ArrayList<>();
    int pos = 0;
    int start = 0;
    while (true) {
      pos = escapedName.indexOf(sep, pos + 1);
      if (pos <= 0) break;
      if ((pos > 0) && escapedName.charAt(pos - 1) != '\\') {
        result.add(escapedName.substring(start, pos));
        start = pos + 1;
      }
    }
    result.add(escapedName.substring(start, escapedName.length())); // remaining
    return result;
  }

  static private final int sep = '.';

  /**
   * Find first occurence of char c in escapedName, excluding escaped c.
   *
   * @param escapedName search in this string
   * @param c           for this char but not \\cha
   * @return pos in string, or -1
   */
  public static int indexOf(String escapedName, char c) {
    int pos = 0;
    while (true) {
      pos = escapedName.indexOf(c, pos + 1);
      if (pos <= 0) return pos;
      if ((pos > 0) && escapedName.charAt(pos - 1) != '\\') return pos;
    }
  }

  // Some handy definitons.
  static final String xmlGT = "&gt;";
  static final String xmlLT = "&lt;";
  static final String xmlAmp = "&amp;";
  static final String xmlApos = "&apos;";
  static final String xmlQuote = "&quot;";

  public static String normalizeToXML(String s) {
    StringBuffer sb = new StringBuffer(s);
    for (int offset = 0; offset < sb.length(); offset++) {
      char c = sb.charAt(offset);
      switch (c) {
        case '>': // GreaterThan
          sb.replace(offset, offset + 1, xmlGT);
          break;
        case '<': // Less Than
          sb.replace(offset, offset + 1, xmlLT);
          break;
        case '&': // Ampersand
          sb.replace(offset, offset + 1, xmlAmp);
          break;
        case '\'': // Single Quote
          sb.replace(offset, offset + 1, xmlApos);
          break;
        case '\"': // Double Quote
          sb.replace(offset, offset + 1, xmlQuote);
          break;
        default:
          break;
      }
    }
    return (sb.toString());

  }

  /**
   * Given a backslash escaped name,
   * convert to a DAP escaped name
   *
   * @param bs the string to DAP encode; may have backslash escapes
   * @return escaped string
   */

  public static String backslashToDAP(String bs) {
    StringBuilder buf = new StringBuilder();
    int len = bs.length();
    for (int i = 0; i < len; i++) {
      char c = bs.charAt(i);
      if (i < (len - 1) && c == '\\') {
        c = bs.charAt(++i);
      }
      if (_allowableInDAP.indexOf(c) < 0) {
        buf.append(_URIEscape);
        // convert the char to hex
        String ashex = Integer.toHexString((int) c);
        if (ashex.length() < 2) buf.append('0');
        buf.append(ashex);
      } else
        buf.append(c);
    }
    return buf.toString();
  }


  /**
   * Given a DAP (attribute) string, insert backslashes
   * before '"' and '/' characters. This code also escapes
   * control characters, although the spec does not call for it;
   * make that code conditional.
   */
  static public String backslashEscapeDapString(String s) {
    StringBuilder buf = new StringBuilder();
    for(int i=0;i<s.length();i++) {
	int c = s.charAt(i);
        if(true) {
            if(c < ' ') {
                switch (c) {
                case '\n': case '\r': case '\t': case '\f':
                    buf.append((char)c);
                    break;
                default:
                    buf.append(String.format("\\x%02x",(c&0xff)));
                    break;
                }
                continue;
            }
        }
        if(c == '"') {
            buf.append("\\\"");
        } else if(c == '\\') {
            buf.append("\\\\");
        } else
            buf.append((char)c);
    }
    return buf.toString();
  }

  /**
     * Given a CDM string, insert backslashes
     * before <toescape> characters.
     */
    static public String backslashEscapeCDMString(String s, String toescape)
    {
      if(toescape == null || toescape.length() == 0) return s;
      if(s == null || s.length() == 0) return s;
      StringBuilder buf = new StringBuilder();
      for(int i=0;i<s.length();i++) {
        int c = s.charAt(i);
        if(toescape.indexOf(c) >= 0) {
          buf.append('\\');
        }
        buf.append((char)c);
      }
      return buf.toString();
    }

}
