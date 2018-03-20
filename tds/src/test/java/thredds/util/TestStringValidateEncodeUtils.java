/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.util;


import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestStringValidateEncodeUtils
{
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testValidSingleLineString()
  {
    String begin = "a really co";
    String end = "ol string";

    // The following should be valid single-line strings.
    assertTrue( "Invalid string when no extra character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + end ) );
    assertTrue( "Invalid string when [Latin small letter eth] character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + '\u00F0' + end ) );
    assertTrue( "Invalid string when [space] character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + ' ' + end ) );
    assertTrue( "Invalid string when [File.pathSeparatorChar] character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + File.pathSeparatorChar + end ) );
    assertTrue( "Invalid string when [less than] character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + '<' + end ) );
    assertTrue( "Invalid string when [greater than] character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + '>' + end ) );
    assertTrue( "Invalid string when [ampersand] character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + '&' + end ) );
    assertTrue( "Invalid string when [backslash] character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + '\\' + end ) );
    assertTrue( "Invalid string when [up directory path] string inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + "/../" + end ) );

    // The following should be invalid single-line strings.
    assertFalse( "Invalid string when [line feed] character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + '\n' + end ) );
    assertFalse( "Invalid string when [carriage return] character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + '\r' + end ) );
    assertFalse( "Invalid string when [tab] character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + '\t' + end ) );
    assertFalse( "Invalid string when [line separator] character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + '\u2028' + end ) );
    assertFalse( "Invalid string when [paragraph separator] character inserted.",
                StringValidateEncodeUtils.validSingleLineString( begin + '\u2029' + end ) );
    assertFalse( "Valid string when [bell] character inserted.",
                 StringValidateEncodeUtils.validSingleLineString( begin + '\u0007' + end ) );
  }

  @Test
  public void testValidPath()
  {
    String begin = "/a/really/co";
    String end = "ol/path";

    // The following should be valid file path strings.
    assertTrue( "Invalid path when no extra character inserted.",
                StringValidateEncodeUtils.validPath( begin + end ));
    assertTrue( "Invalid path when [Latin small letter eth] character inserted.",
                StringValidateEncodeUtils.validPath( begin + '\u00F0' + end));
    assertTrue( "Invalid path when [space] character inserted.",
                StringValidateEncodeUtils.validPath( begin + ' ' + end ) );
    assertTrue( "Invalid path when [java.io.File.pathSeparatorChar] character inserted.",
                 StringValidateEncodeUtils.validPath( begin + File.pathSeparatorChar + end ) );
    assertTrue( "Invalid path when [less than] character inserted.",
                 StringValidateEncodeUtils.validPath( begin + '<' + end ) );
    assertTrue( "Invalid path when [greater than] character inserted.",
                 StringValidateEncodeUtils.validPath( begin + '>' + end ) );
    assertTrue( "Invalid path when [ampersand] character inserted.",
                StringValidateEncodeUtils.validPath( begin + '&' + end ) );
    assertTrue( "Invalid path when [backslash] character inserted.",
                 StringValidateEncodeUtils.validPath( begin + '\\' + end ) );

    // The following should be invalid file path strings.
    assertFalse( "Valid path when [up directory path] string inserted.",
                StringValidateEncodeUtils.validPath( begin + "/../" + end ) );
    assertFalse( "Valid path when [line feed] character inserted.",
                 StringValidateEncodeUtils.validPath( begin + '\n' + end));
    assertFalse( "Valid path when [carriage return] character inserted.",
                 StringValidateEncodeUtils.validPath( begin + '\r' + end));
    assertFalse( "Valid path when [tab] character inserted.",
                 StringValidateEncodeUtils.validPath( begin + '\t' + end));
    assertFalse( "Valid path when [line separator] character inserted.",
                 StringValidateEncodeUtils.validPath( begin + '\u2028' + end));
    assertFalse( "Valid path when [paragraph separator] character inserted.",
                 StringValidateEncodeUtils.validPath( begin + '\u2029' + end));
    assertFalse( "Valid string when [bell] character inserted.",
                 StringValidateEncodeUtils.validPath( begin + '\u0007' + end ) );
  }

  @Test
  public void testValidFilePath()
  {
    String begin = "/a/really/co";
    String end = "ol/path";

    // The following should be valid file path strings.
    assertTrue( "Invalid path when no extra character inserted.",
                StringValidateEncodeUtils.validFilePath( begin + end ));
    assertTrue( "Invalid path when [Latin small letter eth] character inserted.",
                StringValidateEncodeUtils.validFilePath( begin + '\u00F0' + end));
    assertTrue( "Invalid path when [space] character inserted.",
                StringValidateEncodeUtils.validFilePath( begin + ' ' + end ) );
    assertTrue( "Invalid path when [less than] character inserted.",
                 StringValidateEncodeUtils.validFilePath( begin + '<' + end ) );
    assertTrue( "Invalid path when [greater than] character inserted.",
                 StringValidateEncodeUtils.validFilePath( begin + '>' + end ) );
    assertTrue( "Invalid path when [ampersand] character inserted.",
                StringValidateEncodeUtils.validFilePath( begin + '&' + end ) );
    assertTrue( "Invalid path when [backslash] character inserted.",
                 StringValidateEncodeUtils.validFilePath( begin + '\\' + end ) );

    // The following should be invalid file path strings.
    assertFalse( "Valid path when [java.io.File.pathSeparatorChar] character inserted.",
                 StringValidateEncodeUtils.validFilePath( begin + File.pathSeparatorChar + end));
    assertFalse( "Valid path when [up directory path] string inserted.",
                 StringValidateEncodeUtils.validFilePath( begin + "/../" + end ) );
    assertFalse( "Valid path when [line feed] character inserted.",
                 StringValidateEncodeUtils.validFilePath( begin + '\n' + end));
    assertFalse( "Valid path when [carriage return] character inserted.",
                 StringValidateEncodeUtils.validFilePath( begin + '\r' + end));
    assertFalse( "Valid path when [tab] character inserted.",
                 StringValidateEncodeUtils.validFilePath( begin + '\t' + end));
    assertFalse( "Valid path when [line separator] character inserted.",
                 StringValidateEncodeUtils.validFilePath( begin + '\u2028' + end));
    assertFalse( "Valid path when [paragraph separator] character inserted.",
                 StringValidateEncodeUtils.validFilePath( begin + '\u2029' + end));
    assertFalse( "Valid string when [bell] character inserted.",
                 StringValidateEncodeUtils.validFilePath( begin + '\u0007' + end ) );
  }

  @Test
  public void testValidUriString()
  {
    String begin = "/a/really/co";
    String end = "ol/path";

    // The following should be valid file path strings.
    assertTrue( "Invalid path when no extra character inserted.",
                StringValidateEncodeUtils.validUriString( begin + end ) );
    assertTrue( "Invalid path when [Latin small letter eth] character inserted.",
                StringValidateEncodeUtils.validUriString( begin + '\u00F0' + end ) );
    assertTrue( "Invalid path when [space] character inserted.",
                StringValidateEncodeUtils.validUriString( begin + ' ' + end ) );
    assertTrue( "Invalid path when [less than] character inserted.",
                 StringValidateEncodeUtils.validUriString( begin + '<' + end ) );
    assertTrue( "Invalid path when [greater than] character inserted.",
                 StringValidateEncodeUtils.validUriString( begin + '>' + end ) );
    assertTrue( "Invalid path when [ampersand] character inserted.",
                 StringValidateEncodeUtils.validUriString( begin + '&' + end ) );
    assertTrue( "Invalid path when [backslash] character inserted.",
                 StringValidateEncodeUtils.validUriString( begin + '\\' + end ) );
    assertTrue( "Invalid path when [java.io.File.pathSeparatorChar] character inserted.",
                StringValidateEncodeUtils.validUriString( begin + File.pathSeparatorChar + end ) );

    // The following should be invalid file path strings.
    assertFalse( "Valid path when [up directory path] string inserted.",
                 StringValidateEncodeUtils.validUriString( begin + "/../" + end ) );
    assertFalse( "Valid path when [line feed] character inserted.",
                 StringValidateEncodeUtils.validUriString( begin + '\n' + end ) );
    assertFalse( "Valid path when [carriage return] character inserted.",
                 StringValidateEncodeUtils.validUriString( begin + '\r' + end ) );
    assertFalse( "Valid path when [tab] character inserted.",
                 StringValidateEncodeUtils.validUriString( begin + '\t' + end ) );
    assertFalse( "Valid path when [line separator] character inserted.",
                 StringValidateEncodeUtils.validUriString( begin + '\u2028' + end ) );
    assertFalse( "Valid path when [paragraph separator] character inserted.",
                 StringValidateEncodeUtils.validUriString( begin + '\u2029' + end ) );
    assertFalse( "Valid string when [bell] character inserted.",
                 StringValidateEncodeUtils.validUriString( begin + '\u0007' + end ) );
  }

  @Test
  public void testValidIdString()
  {
    String begin = "/a/really/co";
    String end = "ol/path";

    // The following should be valid file path strings.
    assertTrue( "Invalid path when no extra character inserted.",
                StringValidateEncodeUtils.validIdString( begin + end ));
    assertTrue( "Invalid path when [Latin small letter eth] character inserted.",
                StringValidateEncodeUtils.validIdString( begin + '\u00F0' + end));
    assertTrue( "Valid path when [java.io.File.pathSeparatorChar] character inserted.",
                StringValidateEncodeUtils.validIdString( begin + File.pathSeparatorChar + end ) );
    assertTrue( "Valid path when [ampersand] character inserted.",
                StringValidateEncodeUtils.validIdString( begin + '&' + end ) );
    assertTrue( "Valid path when [less than] character inserted.",
                StringValidateEncodeUtils.validIdString( begin + '<' + end ) );
    assertTrue( "Valid path when [greater than] character inserted.",
                StringValidateEncodeUtils.validIdString( begin + '>' + end ) );
    assertTrue( "Valid path when [backslash] character inserted.",
                StringValidateEncodeUtils.validIdString( begin + '\\' + end ) );
    assertTrue( "Valid path when [up directory path] string inserted.",
                StringValidateEncodeUtils.validIdString( begin + "/../" + end ) );

    // The following should be invalid file path strings.
    assertFalse( "Valid path when [space] character inserted.",
                StringValidateEncodeUtils.validIdString( begin + ' ' + end ) );
    assertFalse( "Valid path when [line feed] character inserted.",
                 StringValidateEncodeUtils.validIdString( begin + '\n' + end));
    assertFalse( "Valid path when [carriage return] character inserted.",
                 StringValidateEncodeUtils.validIdString( begin + '\r' + end));
    assertFalse( "Valid path when [tab] character inserted.",
                 StringValidateEncodeUtils.validIdString( begin + '\t' + end));
    assertFalse( "Valid path when [line separator] character inserted.",
                 StringValidateEncodeUtils.validIdString( begin + '\u2028' + end));
    assertFalse( "Valid path when [paragraph separator] character inserted.",
                 StringValidateEncodeUtils.validIdString( begin + '\u2029' + end));
    assertFalse( "Valid string when [bell] character inserted.",
                 StringValidateEncodeUtils.validIdString( begin + '\u0007' + end ) );
  }

  @Test
  public void testValidDecimalNumber()
  {
    assertTrue( StringValidateEncodeUtils.validDecimalNumber( "123"));
    assertTrue( StringValidateEncodeUtils.validDecimalNumber( "0123"));
    assertTrue( StringValidateEncodeUtils.validDecimalNumber( "+123"));
    assertTrue( StringValidateEncodeUtils.validDecimalNumber( "-123"));

    assertFalse( StringValidateEncodeUtils.validDecimalNumber( "+-123"));
    assertFalse( StringValidateEncodeUtils.validDecimalNumber( "12+3"));
    assertFalse( StringValidateEncodeUtils.validDecimalNumber( "12-3"));
    assertFalse( StringValidateEncodeUtils.validDecimalNumber( "+"));
    assertFalse( StringValidateEncodeUtils.validDecimalNumber( "-"));
    assertFalse( StringValidateEncodeUtils.validDecimalNumber( "f"));
    assertFalse( StringValidateEncodeUtils.validDecimalNumber( "f123"));
    assertFalse( StringValidateEncodeUtils.validDecimalNumber( "12f3"));
  }

  @Test
  public void testValidBooleanString()
  {
    assertTrue( StringValidateEncodeUtils.validBooleanString( "true" ) );
    assertTrue( StringValidateEncodeUtils.validBooleanString( "True" ) );
    assertTrue( StringValidateEncodeUtils.validBooleanString( "TRue" ) );
    assertTrue( StringValidateEncodeUtils.validBooleanString( "TRUe" ) );
    assertTrue( StringValidateEncodeUtils.validBooleanString( "truE" ) );

    assertTrue( StringValidateEncodeUtils.validBooleanString( "false" ) );
    assertTrue( StringValidateEncodeUtils.validBooleanString( "False" ) );
    assertTrue( StringValidateEncodeUtils.validBooleanString( "FAlse" ) );
    assertTrue( StringValidateEncodeUtils.validBooleanString( "fAlse" ) );

    assertFalse( StringValidateEncodeUtils.validBooleanString( "fa&lse" ) );
    assertFalse( StringValidateEncodeUtils.validBooleanString( "fred" ) );
    assertFalse( StringValidateEncodeUtils.validBooleanString( "falte" ) );
  }

  @Test
  public void testValidAlphanumericString()
  {
    assertTrue( StringValidateEncodeUtils.validAlphanumericString( "true" ) );
    assertTrue( StringValidateEncodeUtils.validAlphanumericString( "abcdefghigklmnopqrstuvwxyz" ) );
    assertTrue( StringValidateEncodeUtils.validAlphanumericString( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" ) );
    assertTrue( StringValidateEncodeUtils.validAlphanumericString( "0123456789" ) );
    assertTrue( StringValidateEncodeUtils.validAlphanumericString( "1aB" ) );

    assertFalse( StringValidateEncodeUtils.validAlphanumericString( "1&aB" ) );
    assertFalse( StringValidateEncodeUtils.validAlphanumericString( "1 aB" ) );
  }

  @Test
  public void testValidAlphanumericStringConstrainedSet()
  {
    String[] set = {"true", "123abc", "F12G3a0", "false"};
    String[] badSet = {"tr-ue", "12&3abc", "F12 G3a0", "false"};

    assertTrue( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "true", set, false ) );
    assertFalse( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "tRue", set, false ) );
    assertTrue( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "true", set, true ) );
    assertTrue( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "tRue", set, true ) );

    assertTrue( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "F12G3a0", set, false ) );
    assertFalse( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "f12G3a0", set, false ) );
    assertTrue( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "F12G3a0", set, true ) );
    assertTrue( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "f12G3a0", set, true ) );

    assertFalse( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "true", badSet, false ) );
    assertFalse( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "tr-ue", badSet, false ) );
    assertFalse( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "truE", badSet, true ) );
    assertFalse( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "tr-uE", badSet, true ) );

    assertTrue( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "false", badSet, false ) );
    assertFalse( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "fAlse", badSet, false ) );
    assertTrue( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "false", badSet, true ) );
    assertTrue( StringValidateEncodeUtils.validAlphanumericStringConstrainedSet( "fAlse", badSet, true ) );
  }

  @Test
  public void testValidPercentHexOctetsString()
  {
    assertTrue( StringValidateEncodeUtils.validPercentHexOctetsString( "%46" ));
    assertTrue( StringValidateEncodeUtils.validPercentHexOctetsString( "%42%44" ));
    assertTrue( StringValidateEncodeUtils.validPercentHexOctetsString( "%42%44%46" ));
    assertTrue( StringValidateEncodeUtils.validPercentHexOctetsString( "%a2%F4%e6" ));

    assertFalse( StringValidateEncodeUtils.validPercentHexOctetsString( "%fff%a123" ));
    assertFalse( StringValidateEncodeUtils.validPercentHexOctetsString( "abcdef" ));
    assertFalse( StringValidateEncodeUtils.validPercentHexOctetsString( "123" ));
  }

  @Test
  public void testUnicodeCodePoint2PercentHexString()
  {
    // U+0007 (decimal code point: 7) - Bell
    assertEquals( "%07", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 7, "UTF-8" ));
    assertEquals( "%07", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 7, "ISO-8859-1" ));
    // U+0020 (decimal code point: 32) - Space (" ")
    assertEquals( "%20", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 32, "UTF-8" ));
    assertEquals( "%20", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 32, "ISO-8859-1" ));
    // U+0041 (decimal code point: 65) - LATIN CAPITAL LETTER A ("A")
    assertEquals( "%41", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 65, "UTF-8" ));
    assertEquals( "%41", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 65, "ISO-8859-1" ));
    // U+007f (decimal code point: 127) - Delete
    assertEquals( "%7f", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 127, "UTF-8" ));
    assertEquals( "%7f", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 127, "ISO-8859-1" ));
    // U+0080 (decimal code point: 128) - <control>
    assertEquals( "%c2%80", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 128, "UTF-8" ));
    assertEquals( "%80", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 128, "ISO-8859-1" ));
    // U+00c6 (decimal code point: 198) - Latin Capital Letter AE
    assertEquals( "%c3%86", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 198, "UTF-8" ));
    assertEquals( "%c6", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 198, "ISO-8859-1" ));
    // U+00ff (decimal code point: 255) - Latin Small letter Y with Diaresis
    assertEquals( "%c3%bf", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 255, "UTF-8" ));
    assertEquals( "%ff", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 255, "ISO-8859-1" ));

    // U+0800 (decimal code point: 2048) - SAMARITAN LETTER ALAF
    assertEquals( "%e0%a0%80", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 2048, "UTF-8" ) );
    // U+0901 (decimal code point: 2305) - DEVANAGARI SIGN CANDRABINDU
    assertEquals( "%e0%a4%81", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 2305, "UTF-8" ) );
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnicodeCodePoint2PercentHexStringOnInvalidCodePoint() {
    // From "Invalid code points" section of the UTF-8 Wikipedia article
    // (http://en.wikipedia.org/wiki/UTF-8#Invalid_code_points):

    //     According to the UTF-8 definition (RFC 3629) the high and low
    //     surrogate halves used by UTF-16 (U+D800 through U+DFFF) are not
    //     legal Unicode values, and their UTF-8 encoding should be treated
    //     as an invalid byte sequence.
    //
    //     Whether an actual application should do this is debatable, as it
    //     makes it impossible to store invalid UTF-16 (that is, UTF-16 with
    //     unpaired surrogate halves) in a UTF-8 string. This is necessary to
    //     store unchecked UTF-16 such as Windows filenames as UTF-8. It is
    //     also incompatible with CESU encoding (described below).

    // U+D800 (decimal code point: 55296) - a UTF-16 surrogate (U+D800 - U+DFFF)
    // Not a valid unicode code point. Expect an IllegalArgumentException
    StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 55296, "UTF-8" );
//    try {
//      assertNotNull( StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 55296, "UTF-8" ) );
//    } catch ( IllegalArgumentException e ) {
//      return;
//    }
//    fail( "Didn't catch invalid Unicode code point [55296].");

  }

  @Test
  public void testDescendOnlyFilePath()
  {
    assertTrue( StringValidateEncodeUtils.descendOnlyFilePath( "./fred/frank/john" ) );
    assertTrue( StringValidateEncodeUtils.descendOnlyFilePath( "./fred/frank/../john" ) );
    assertTrue( StringValidateEncodeUtils.descendOnlyFilePath( "./fred/frank/../../john" ) );
    assertFalse( StringValidateEncodeUtils.descendOnlyFilePath( "./fred/frank/../../../john" ) );
    assertFalse( StringValidateEncodeUtils.descendOnlyFilePath( "../fred/frank/john" ) );
    assertFalse( StringValidateEncodeUtils.descendOnlyFilePath( "../../fred/frank/john" ) );
    assertTrue( StringValidateEncodeUtils.descendOnlyFilePath( "./fred/frank/john/.." ) );
    assertTrue( StringValidateEncodeUtils.descendOnlyFilePath( "./fred/frank/john/../.." ) );
    assertTrue( StringValidateEncodeUtils.descendOnlyFilePath( "./fred/frank/john/../../.." ) );
    assertFalse( StringValidateEncodeUtils.descendOnlyFilePath( "./fred/frank/john/../../../.." ) );
    assertTrue( StringValidateEncodeUtils.descendOnlyFilePath( "./fred/../frank/john" ) );
    assertTrue( StringValidateEncodeUtils.descendOnlyFilePath( "./fred/../frank/../john" ) );
    assertFalse( StringValidateEncodeUtils.descendOnlyFilePath( "./fred/../frank/../../john" ) );
  }

  /**
   * My attempt to understand Unicode and how Java deals with it.
   */
  //@Test
  public void checkEntireUnicodeCodespace() {
    int numberOfUnicodeCodePoints = Character.MAX_CODE_POINT; // 1114112;
    int validCount = 0;
    int definedCount = 0;
    int unassignedCount = 0;
    int surrogateCount = 0;

    int surrogateDefinedCount = 0;
    int surrogateUndefinedCount = 0;
    for (int codePoint = 0; codePoint < numberOfUnicodeCodePoints; codePoint++ ) {
      if ( Character.isValidCodePoint( codePoint ) ) validCount++;
      if ( Character.isDefined( codePoint ) ) definedCount++;
      if ( Character.getType( codePoint ) == Character.UNASSIGNED ) unassignedCount++;
      if ( Character.getType( codePoint ) == Character.SURROGATE ) surrogateCount++;

      if ( Character.getType( codePoint ) == Character.SURROGATE && Character.isDefined( codePoint ) )
        surrogateDefinedCount++;
      if ( Character.getType( codePoint ) == Character.SURROGATE && ! Character.isDefined( codePoint ) )
        surrogateUndefinedCount++;
    }

    System.out.printf( "Number of valid code points: %d.%n", validCount );
    System.out.printf( "Number of defined code points: %d.%n", definedCount );
    System.out.printf( "Number of unassigned code points: %d.%n", unassignedCount );
    System.out.printf( "Number of surrogate code points: %d.%n%n", surrogateCount );

    System.out.printf( "Number of defined surrogate code points: %d.%n", surrogateDefinedCount );
    System.out.printf( "Number of undefined surrogate code points: %d.%n", surrogateUndefinedCount );

    // As of 18 Oct 2013, results are
    //     Number of valid code points:      1114112.
    //     Number of defined code points:     248965.
    //     Number of unassigned code points:  865147.
    //     Number of surrogate code points:     2048.

    //     Number of defined surrogate code points:   2048.
    //     Number of undefined surrogate code points:    0.
  }
}
