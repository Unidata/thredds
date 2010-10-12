/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
public class StringValidateEncodeUtils
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( StringValidateEncodeUtils.class );

  private StringValidateEncodeUtils() {}
  
  public final static String CHARACTER_ENCODING_UTF_8 = "UTF-8";

  /**
   * Return true if the given String is a valid single-line String.
   *
   * <p>A string will be considered a valid single-line string if it does not
   * contain any characters from these Unicode general categories:
   *
   * <ul>
   *   <li>Cc - Other, Control</li>
   *   <li>Cf - Other, Format</li>
   *   <li>Cs - Other, Surrogate</li>
   *   <li>Co - Other, Private Use</li>
   *   <li>Cn - Other, Not Assigned</li>
   *   <li>Zl - Separator, Line</li>
   *   <li>Zp - Separator, Paragraph</li>
   * </ul>
   *
   * <p>Or, in other words, allow: Letters, Numbers, Marks, Punctuation,
   * Symbols, and Space separators.
   *
   * @param singleLineString the String to validate
   * @return true if the given String is a valid single-line String.
   */
  public static boolean validSingleLineString( String singleLineString )
  {
    if ( singleLineString == null ) return false;
    Matcher m = INVALID_CHARACTERS_FOR_SINGLE_LINE_STRING_PATTERN.matcher( singleLineString );
    return ! m.find();
  }
  private final static Pattern INVALID_CHARACTERS_FOR_SINGLE_LINE_STRING_PATTERN
          = Pattern.compile( "[\\p{Zl}\\p{Zp}\\p{C}]");

  /**
   * Return true if the given String is a valid path.
   *
   * <p>
   * A String is considered a valid path if:
   * <ul>
   *   <li> when passed to validSingleLineString(String) true is returned, and
   *   <li> it does not contain any parent path segments ("../").</li>
   *   </li>
   * </ul>
   *
   * @param path the String to validate
   * @return true if the given String is a valid path.
   * @see #validSingleLineString(String)
   */
  @SuppressWarnings({"SimplifiableIfStatement"})
  public static boolean validPath( String path )
  {
    if ( path == null )
      return false;
    // Don't allow ".." directories in path.
    if ( path.indexOf( "/../" ) != -1 || path.equals( ".." )
         || path.startsWith( "../" ) || path.endsWith( "/.." ) )
      return false;

    return validSingleLineString( path );
  }

  /**
   * Return true if the given String is a valid File path.
   *
   * <p>
   * A String is considered a valid File path if:
   * <ul>
   *   <li> when passed to validPath(String) true is returned; and</li>
   *   <li> it does not contain the Java File path separator
   *        (java.io.File.pathSeparatorChar) which is system dependant.</li>
   * </ul>
   *
   * @param path the String to validate
   * @return true if the given String is a valid File path.
   * @see #validPath(String)
   */
  @SuppressWarnings({"SimplifiableIfStatement"})
  public static boolean validFilePath( String path )
  {
    if ( path == null )
      return false;
    if ( path.indexOf( File.pathSeparatorChar) != -1 )
      return false;
    return validPath( path );
  }

  /**
   * Return true if the given String is a valid URI string.
   *
   * <p>
   * A String is considered a valid URI path if:
   * <ul>
   *   <li> when passed to validPath(String) true is returned; and</li>
   *   <li> ??? see Note below</li>
   * </ul>
   *
   * <p><strong>NOTE:</strong> Check compliance with URI RFC (RFC 3986) - TODO.
   *
   * @param uri the String to validate.
   * @return true if the given String is a valid URI string.
   */
  @SuppressWarnings({"SimplifiableIfStatement"})
  public static boolean validUriString( String uri )
  {
    if ( uri == null )
      return false;
    return validPath( uri );
  }

  /**
   * Return true if the given String is a valid ID string.
   *
   * <p>
   * A String is considered a valid ID string if:
   * <ul>
   *   <li>it contains no space separator characters (Unicode general
   *       category Zs - Separator, Space); and</li>
   *   <li>true is returned when the string is passed to
   *       validSingleLineString(String).</li>
   * </ul>
   *
   * @param id the String to validate
   * @return true if the given String is a valid ID string.
   * @see #validSingleLineString(String)
   */
  public static boolean validIdString( String id )
  {
    if ( id == null ) return false;
    Matcher m = INVALID_CHARACTERS_FOR_ID_STRING_PATTERN.matcher( id );
    return ! ( m.find() || ! validSingleLineString( id ) );
  }
  private final static Pattern INVALID_CHARACTERS_FOR_ID_STRING_PATTERN
          = Pattern.compile( "[\\p{Zs}]");

  /**
   * Return true if the given String contains any less than ("<") or
   * greater than (">") characters; otherwise return false.
   *
   * @param string the String to check.
   * @return true if the given String contains any less than ("<") or greater than (">") characters
   */
  public static boolean containsAngleBracketCharacters( String string )
  {
    if ( string == null )
      return false;
    if ( string.indexOf( "<" ) == -1
         && string.indexOf( ">" ) == -1 )
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
  public static boolean containsAmpersandCharacters( String string )
  {
    if ( string == null )
      return false;
    if ( string.indexOf( "&" ) == -1 )
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
  public static boolean containsBackslashCharacters( String string )
  {
    if ( string == null )
      return false;
    if ( string.indexOf( "\\" ) == -1 )
      return false;

    return true;
  }

  public static boolean validDecimalNumber( String number )
  {
    if ( number == null )
      return false;
    Matcher m = VALID_DECIMAL_DIGITS_PATTERN.matcher( number );
    return m.matches();
  }
  private final static Pattern VALID_DECIMAL_DIGITS_PATTERN
          = Pattern.compile( "[\\+\\-]?[0-9]+");

  /**
   * Return true if the given String is "true" or "false", ignoring case.
   *
   * @param boolString the String to validate.
   * @return true if the given String is "true" or "false", ignoring case.
   */
  @SuppressWarnings({"SimplifiableIfStatement"})
  public static boolean validBooleanString( String boolString )
  {
    if ( boolString == null )
      return false;
    Matcher m = VALID_CHARACTERS_FOR_BOOLEAN_STRING_PATTERN.matcher( boolString );
    if ( ! m.matches() )
      return false;
    return boolString.equalsIgnoreCase( "true" )
           || boolString.equalsIgnoreCase( "false" );
  }
  private final static Pattern VALID_CHARACTERS_FOR_BOOLEAN_STRING_PATTERN
          = Pattern.compile( "[trueTRUEfalsFALS]*" );

  /**
   * Return true if the given String is an alphanumeric string.
   *
   * @param alphNumString the String to validate.
   * @return true if the given String is an alphanumeric string.
   */
  public static boolean validAlphanumericString( String alphNumString )
  {
    if ( alphNumString == null )
      return false;
    Matcher m = VALID_CHARACTERS_FOR_ALPHANUMERIC_STRING_PATTERN.matcher( alphNumString);
    return m.matches();
  }
  private final static Pattern VALID_CHARACTERS_FOR_ALPHANUMERIC_STRING_PATTERN
          = Pattern.compile( "[a-zA-Z0-9]*" );

  /**
   * Return true if the given String is an alphanumeric string and one of
   * the valid strings in the constrained set.
   *
   * @param alphNumString the String to validate.
   * @param constrainedSet the set of valid strings
   * @param ignoreCase if true ignore the case of the letters
   * @return true if the given String is an alphanumeric string.
   */
  public static boolean validAlphanumericStringConstrainedSet( String alphNumString,
                                                               String[] constrainedSet,
                                                               boolean ignoreCase )
  {
    if ( alphNumString == null || constrainedSet == null || constrainedSet.length == 0 )
      return false;
    Matcher m = VALID_CHARACTERS_FOR_ALPHANUMERIC_STRING_PATTERN.matcher( alphNumString );
    if ( !m.matches() )
      return false;
    for ( String s : constrainedSet )
    {
      if ( ignoreCase ? alphNumString.equalsIgnoreCase( s ) : alphNumString.equals( s ) )
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
  public static boolean descendOnlyFilePath( String path )
  {
    String[] pathSegments = path.split( "/" );
    //String[] newPathSegments = new String[pathSegments.length];
    int i = 0;
    for ( int indxOrigSegs = 0; indxOrigSegs < pathSegments.length; indxOrigSegs++ )
    {
      String s = pathSegments[ indxOrigSegs];
      if ( s.equals( "." ) )
        continue;
      else if ( s.equals( ".." ) )
      {
        if ( i == 0 )
          return false;
        i--;
      }
      else
      {
        //newPathSegments[i] = s;
        i++;
      }
    }

    return true;
  }

  public static String encodeLogMessages( String msg )
  {
    // For now, just use URLEncoder.
    // ToDo Would rather not encode "/" and " " (maybe others).
    try
    {
      return java.net.URLEncoder.encode( msg, CHARACTER_ENCODING_UTF_8 );
    }
    catch ( UnsupportedEncodingException e )
    {
      // This SHOULD NEVER HAPPEN as all JVMs are required to support UTF-8 encoding.
      log.error( "UnsupportedEncodingException for \"" + CHARACTER_ENCODING_UTF_8 + "\": " + e.getMessage() );
      throw new IllegalStateException( "UnsupportedEncodingException for \"" + CHARACTER_ENCODING_UTF_8 + "\".");
    }
  }

  public static String encodeContentForHtml( String content )
  {
    return HtmlUtils.htmlEscape( content );
  }

  /* public static String encodeContentForXml( String content )
  {
    return StringEscapeUtils.escapeXml( content );
  }  */

  /**
   * <strong>NOT YET IMPLEMENTED:</strong>
   * Convert a percent hex encoded string (%20) to a unicode code point.
   *
   * @param percentHexString the string to convert to a Unicode code point
   * @param charsetName the name of the Character set to use in the conversion
   * @return the Unicode code point represented by the given percentHex encoded string.
   * @throws IllegalArgumentException if the given percentHex string is not valid or if the requested character set is not supported.
   */
  public static int percentHexString2unicodeCodePoint( String percentHexString, String charsetName )
  {
    Charset charset = Charset.availableCharsets().get( charsetName );
    if ( charset == null )
      throw new IllegalArgumentException( "Unsupported charset [" + charsetName + "]." );

    if ( ! StringValidateEncodeUtils.validPercentHexOctetsString( percentHexString ) )
      throw new IllegalArgumentException( "Invalid percentHexOctets string ["+percentHexString+"].");

    String[] hexOctets = percentHexString.split( "%" );
    ByteBuffer bb = ByteBuffer.allocate( hexOctets.length );
    bb.putInt( Integer.valueOf( hexOctets[0], 16 ));

    CharBuffer cb = charset.decode( bb );
    //cb.rewind().get();
    cb.hasArray();
    // ToDo Look Implement.

    return -1;
  }

  /**
   * Check that the given string is a valid percentHexOctets string. The
   * string is considered valid if it only contains a sequence of "%" prefixed,
   * two character strings where each two character string is composed only of
   * US-ASCII digits and upper- or lower-case A-F.
   *
   * For example: "%31%32" or "%7b%7d%7E"
   *
   * @param percentHexOctetsString the string to check for validity
   * @return true if the string is valid, false otherwise.
   */
  public static boolean validPercentHexOctetsString( String percentHexOctetsString )
  {
    if ( percentHexOctetsString == null )
      return false;
    Matcher m = VALID_PERCENT_HEX_OCTETS_PATTERN.matcher( percentHexOctetsString );
    return m.matches();
  }

  private final static Pattern VALID_PERCENT_HEX_OCTETS_PATTERN
          = Pattern.compile( "(?:%[0-9a-fA-F]{2})*" );

  /**
   * Return the percentHexOctets string that represents the given Unicode
   * code point in the given character set or null if the given character
   * set cannot encode the given code point.
   *
   * @param codePoint the given Unicode code point
   * @param charsetName the name of the character set.
   * @return the percentHexOctets string that represents the given Unicode code point in the given character set.
   * @throws IllegalArgumentException if the code point is not defined or the the character set is not supported.
   */
  public static String unicodeCodePoint2PercentHexString( int codePoint, String charsetName )
  {
    if ( ! Character.isDefined( codePoint ))
      throw new IllegalArgumentException( "Not a valid Unicode code point [" + codePoint + "]." );
    Charset charset = Charset.availableCharsets().get( charsetName );
    if ( charset == null )
      throw new IllegalArgumentException( "Unsupported charset ["+charsetName+"].");

    char[] chars = Character.toChars( codePoint );
    ByteBuffer byteBuffer = null;
    try
    {
      byteBuffer = charset.newEncoder().encode( CharBuffer.wrap( chars ) );
    }
    catch ( CharacterCodingException e )
    {
      // The selected charset cannot encode given code point.
      return null;
    }
    byteBuffer.rewind();
    StringBuilder encodedString = new StringBuilder();
    for ( int i = 0; i < byteBuffer.limit(); i++ )
    {
      String asHex = Integer.toHexString( byteBuffer.get() & 0xFF );
      encodedString.append( "%" ).append( asHex.length() == 1 ? "0" : "").append( asHex );
    }

    return encodedString.toString();
  }
}
