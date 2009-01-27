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

import junit.framework.*;

import java.io.File;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestStringValidateEncodeUtils extends TestCase
{

  public TestStringValidateEncodeUtils( String name )
  {
    super( name );
  }

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

  public void testUnicodeCodePoint2PercentHexString()
  {
    // Bell
    assertEquals( "%07", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 7, "UTF-8" ));
    assertEquals( "%07", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 7, "ISO-8859-1" ));
    // Space (" ")
    assertEquals( "%20", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 32, "UTF-8" ));
    assertEquals( "%20", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 32, "ISO-8859-1" ));
    // "A"
    assertEquals( "%41", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 65, "UTF-8" ));
    assertEquals( "%41", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 65, "ISO-8859-1" ));
    // Delete
    assertEquals( "%7f", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 127, "UTF-8" ));
    assertEquals( "%7f", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 127, "ISO-8859-1" ));
    // <control>
    assertEquals( "%c2%80", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 128, "UTF-8" ));
    assertEquals( "%80", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 128, "ISO-8859-1" ));
    // Latin Capial Letter AE
    assertEquals( "%c3%86", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 198, "UTF-8" ));
    assertEquals( "%c6", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 198, "ISO-8859-1" ));
    // Latin Small letter Y with Diaresis
    assertEquals( "%c3%bf", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 255, "UTF-8" ));
    assertEquals( "%ff", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 255, "ISO-8859-1" ));
    // DEVANAGARI SIGN CANDRABINDU
    assertEquals( "%e0%a4%81", StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 2305, "UTF-8" ) );
    //   Not a valid unicode code point
    try
    {
      assertNotNull( StringValidateEncodeUtils.unicodeCodePoint2PercentHexString( 2048, "UTF-8" ) );
    }
    catch ( IllegalArgumentException e )
    {
      return;
    }
    fail( "Didn't catch invalid Unicode code point [2048].");
  }

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
}
