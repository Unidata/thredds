/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.util;

import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;

/**
 * Utility methods for validating strings.
 *
 * @author edavis
 * @since 3.16.47
 */
public class StringValidateEncodeUtils {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StringValidateEncodeUtils.class);

  private StringValidateEncodeUtils() {
  }

  public final static String CHARACTER_ENCODING_UTF_8 = "UTF-8";

  /**
   * Return true if the given String is a valid single-line String.
   * <p>
   * <p>A string will be considered a valid single-line string if it does not
   * contain any characters from these Unicode general categories:
   * <p>
   * <ul>
   * <li>Cc - Other, Control</li>
   * <li>Cf - Other, Format</li>
   * <li>Cs - Other, Surrogate</li>
   * <li>Co - Other, Private Use</li>
   * <li>Cn - Other, Not Assigned</li>
   * <li>Zl - Separator, Line</li>
   * <li>Zp - Separator, Paragraph</li>
   * </ul>
   * <p>
   * <p>Or, in other words, allow: Letters, Numbers, Marks, Punctuation,
   * Symbols, and Space separators.
   *
   * @param singleLineString the String to validate
   * @return true if the given String is a valid single-line String.
   */
  public static boolean validSingleLineString(String singleLineString) {
    if (singleLineString == null) return false;
    Matcher m = INVALID_CHARACTERS_FOR_SINGLE_LINE_STRING_PATTERN.matcher(singleLineString);
    return !m.find();
  }

  private final static Pattern INVALID_CHARACTERS_FOR_SINGLE_LINE_STRING_PATTERN
          = Pattern.compile("[\\p{Zl}\\p{Zp}\\p{C}]");

  /**
   * Return true if the given String is a valid path.
   * <p>
   * <p>
   * A String is considered a valid path if:
   * <ul>
   * <li> when passed to validSingleLineString(String) true is returned, and
   * <li> it does not contain any parent path segments ("../").</li>
   * </li>
   * </ul>
   *
   * @param path the String to validate
   * @return true if the given String is a valid path.
   * @see #validSingleLineString(String)
   */
  @SuppressWarnings({"SimplifiableIfStatement"})
  public static boolean validPath(String path) {
    if (path == null)
      return false;
    // Don't allow ".." directories in path.
    if (path.indexOf("/../") != -1 || path.equals("..")
            || path.startsWith("../") || path.endsWith("/.."))
      return false;

    return validSingleLineString(path);
  }

  /**
   * Return true if the given String is a valid File path.
   * <p>
   * <p>
   * A String is considered a valid File path if:
   * <ul>
   * <li> when passed to validPath(String) true is returned; and</li>
   * <li> it does not contain the Java File path separator
   * (java.io.File.pathSeparatorChar) which is system dependant.</li>
   * </ul>
   *
   * @param path the String to validate
   * @return true if the given String is a valid File path.
   * @see #validPath(String)
   */
  @SuppressWarnings({"SimplifiableIfStatement"})
  public static boolean validFilePath(String path) {
    if (path == null)
      return false;
    if (path.indexOf(File.pathSeparatorChar) != -1)
      return false;
    return validPath(path);
  }

  /**
   * Return true if the given String is a valid URI string.
   * <p>
   * <p>
   * A String is considered a valid URI path if:
   * <ul>
   * <li> when passed to validPath(String) true is returned; and</li>
   * <li> ??? see Note below</li>
   * </ul>
   * <p>
   * <p><strong>NOTE:</strong> Check compliance with URI RFC (RFC 3986) - TODO.
   *
   * @param uri the String to validate.
   * @return true if the given String is a valid URI string.
   */
  @SuppressWarnings({"SimplifiableIfStatement"})
  public static boolean validUriString(String uri) {
    if (uri == null)
      return false;
    return validPath(uri);
  }

  /**
   * Return true if the given String is a valid ID string.
   * <p>
   * <p>
   * A String is considered a valid ID string if:
   * <ul>
   * <li>it contains no space separator characters (Unicode general
   * category Zs - Separator, Space); and</li>
   * <li>true is returned when the string is passed to
   * validSingleLineString(String).</li>
   * </ul>
   *
   * @param id the String to validate
   * @return true if the given String is a valid ID string.
   * @see #validSingleLineString(String)
   */
  public static boolean validIdString(String id) {
    if (id == null) return false;
    Matcher m = INVALID_CHARACTERS_FOR_ID_STRING_PATTERN.matcher(id);
    return !(m.find() || !validSingleLineString(id));
  }

  private final static Pattern INVALID_CHARACTERS_FOR_ID_STRING_PATTERN
          = Pattern.compile("[\\p{Zs}]");

  /**
   * Return true if the given String contains any less than ("<") or
   * greater than (">") characters; otherwise return false.
   *
   * @param string the String to check.
   * @return true if the given String contains any less than ("<") or greater than (">") characters
   */
  public static boolean containsAngleBracketCharacters(String string) {
    if (string == null)
      return false;
    if (string.indexOf("<") == -1
            && string.indexOf(">") == -1)
      return false;

    return true;
  }

  /**
   * Return true if the given String contains any ampersand ("&")
   * characters; otherwise return false.
   *
   * @param string the String to check.
   * @return true if the given String contains any ampersand ("&") characters
   */
  public static boolean containsAmpersandCharacters(String string) {
    if (string == null)
      return false;
    if (string.indexOf("&") == -1)
      return false;

    return true;
  }

  /**
   * Return true if the given String contains any backslash ("\")
   * characters; otherwise return false.
   *
   * @param string the String to check.
   * @return true if the given String contains any backslash ("\") characters
   */
  public static boolean containsBackslashCharacters(String string) {
    if (string == null)
      return false;
    if (string.indexOf("\\") == -1)
      return false;

    return true;
  }

  public static boolean validDecimalNumber(String number) {
    if (number == null)
      return false;
    Matcher m = VALID_DECIMAL_DIGITS_PATTERN.matcher(number);
    return m.matches();
  }

  private final static Pattern VALID_DECIMAL_DIGITS_PATTERN
          = Pattern.compile("[\\+\\-]?[0-9]+");

  /**
   * Return true if the given String is "true" or "false", ignoring case.
   *
   * @param boolString the String to validate.
   * @return true if the given String is "true" or "false", ignoring case.
   */
  @SuppressWarnings({"SimplifiableIfStatement"})
  public static boolean validBooleanString(String boolString) {
    if (boolString == null)
      return false;
    Matcher m = VALID_CHARACTERS_FOR_BOOLEAN_STRING_PATTERN.matcher(boolString);
    if (!m.matches())
      return false;
    return boolString.equalsIgnoreCase("true")
            || boolString.equalsIgnoreCase("false");
  }

  private final static Pattern VALID_CHARACTERS_FOR_BOOLEAN_STRING_PATTERN
          = Pattern.compile("[trueTRUEfalsFALS]*");

  /**
   * Return true if the given String is an alphanumeric string.
   *
   * @param alphNumString the String to validate.
   * @return true if the given String is an alphanumeric string.
   */
  public static boolean validAlphanumericString(String alphNumString) {
    if (alphNumString == null)
      return false;
    Matcher m = VALID_CHARACTERS_FOR_ALPHANUMERIC_STRING_PATTERN.matcher(alphNumString);
    return m.matches();
  }

  private final static Pattern VALID_CHARACTERS_FOR_ALPHANUMERIC_STRING_PATTERN
          = Pattern.compile("[a-zA-Z0-9]*");

  /**
   * Return true if the given String is an alphanumeric string and one of
   * the valid strings in the constrained set.
   *
   * @param alphNumString  the String to validate.
   * @param constrainedSet the set of valid strings
   * @param ignoreCase     if true ignore the case of the letters
   * @return true if the given String is an alphanumeric string.
   */
  public static boolean validAlphanumericStringConstrainedSet(String alphNumString,
                                                              String[] constrainedSet,
                                                              boolean ignoreCase) {
    if (alphNumString == null || constrainedSet == null || constrainedSet.length == 0)
      return false;
    Matcher m = VALID_CHARACTERS_FOR_ALPHANUMERIC_STRING_PATTERN.matcher(alphNumString);
    if (!m.matches())
      return false;
    for (String s : constrainedSet) {
      if (ignoreCase ? alphNumString.equalsIgnoreCase(s) : alphNumString.equals(s))
        return true;
    }
    return false;
  }

  /**
   * Return true if the given path does not ascend into parent directory.
   *
   * @param path the path to check
   * @return true if the given path does not ascend into parent directory.
   */
  @SuppressWarnings({"UnnecessaryContinue"})
  public static boolean descendOnlyFilePath(String path) {
    String[] pathSegments = path.split("/");
    //String[] newPathSegments = new String[pathSegments.length];
    int i = 0;
    for (int indxOrigSegs = 0; indxOrigSegs < pathSegments.length; indxOrigSegs++) {
      String s = pathSegments[indxOrigSegs];
      if (s.equals("."))
        continue;
      else if (s.equals("..")) {
        if (i == 0)
          return false;
        i--;
      } else {
        //newPathSegments[i] = s;
        i++;
      }
    }

    return true;
  }

  /**
   * Check that the given string is a valid percentHexOctets string. The
   * string is considered valid if it only contains a sequence of "%" prefixed,
   * two character strings where each two character string is composed only of
   * US-ASCII digits and upper- or lower-case A-F.
   * <p>
   * For example: "%31%32" or "%7b%7d%7E"
   *
   * @param percentHexOctetsString the string to check for validity
   * @return true if the string is valid, false otherwise.
   */
  public static boolean validPercentHexOctetsString(String percentHexOctetsString) {
    if (percentHexOctetsString == null)
      return false;
    Matcher m = VALID_PERCENT_HEX_OCTETS_PATTERN.matcher(percentHexOctetsString);
    return m.matches();
  }

  private final static Pattern VALID_PERCENT_HEX_OCTETS_PATTERN
          = Pattern.compile("(?:%[0-9a-fA-F]{2})*");

  /**
   * Return the percentHexOctets string that represents the given Unicode
   * code point in the given character set or null if the given character
   * set cannot encode the given code point.
   *
   * @param codePoint   the given Unicode code point
   * @param charsetName the name of the character set.
   * @return the percentHexOctets string that represents the given Unicode code point in the given character set.
   * @throws IllegalArgumentException if the code point is not defined or the the character set is not supported.
   */
  public static String unicodeCodePoint2PercentHexString(int codePoint, String charsetName) {
    if (!Character.isDefined(codePoint))
      throw new IllegalArgumentException(String.format("Given code point [U+%1$04X - %1$d] not assigned to an abstract character.", codePoint));
    if (Character.getType(codePoint) == Character.SURROGATE)
      throw new IllegalArgumentException(String.format("Given code point [U+%1$04X - %1$d] is an unencodable (by itself) surrogate character.", codePoint));

    Charset charset = Charset.availableCharsets().get(charsetName);
    if (charset == null)
      throw new IllegalArgumentException(String.format("Unsupported charset [%s].", charsetName));

    char[] chars = Character.toChars(codePoint);
    ByteBuffer byteBuffer = null;
    try {
      byteBuffer = charset.newEncoder().encode(CharBuffer.wrap(chars));
    } catch (CharacterCodingException e) {
      String message = String.format("Given code point [U+%1$04X - %1$d] cannot be encode in given charset [%2$s].", codePoint, charsetName);
      throw new IllegalArgumentException(message, e);
    }
    byteBuffer.rewind();
    StringBuilder encodedString = new StringBuilder();
    for (int i = 0; i < byteBuffer.limit(); i++) {
      String asHex = Integer.toHexString(byteBuffer.get() & 0xFF);
      encodedString.append("%").append(asHex.length() == 1 ? "0" : "").append(asHex);
    }

    return encodedString.toString();
  }
}
