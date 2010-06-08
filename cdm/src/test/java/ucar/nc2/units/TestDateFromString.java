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
package ucar.nc2.units;

import junit.framework.*;

import java.util.Date;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 29, 2005 6:14:37 PM
 */
public class TestDateFromString extends TestCase
{


  private String fileName = "xzy_tds_20051129_1235_junk.grib";
  private String dateAsISOString = "2005-11-29T12:35";
  private long dateAsLong = 1133267700000L;



  public TestDateFromString( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testGetDateUsingSimpleDateFormat()
  {
    String dateFormatString = "yyyyMMdd_HHmm";
    Date date = DateFromString.getDateUsingSimpleDateFormat( fileName, dateFormatString );
    assertTrue( "Calculated date <" + date.toString() + " [" + date.getTime() + "]> not as expected <" + dateAsISOString + "[" + dateAsLong + "]>." +
                "\nUsing fileName <" + fileName + "> and dateFormatString <" + dateFormatString + ">",
                date.getTime() == dateAsLong );


  }

  public void testGetDateUsingCompleteDateFormat()
  {
    String dateFormatString = "'xzy_tds_'yyyyMMdd_HHmm'_junk.grib'";

    Date date = DateFromString.getDateUsingCompleteDateFormat( fileName, dateFormatString );
    assertTrue( "Calculated date <" + date.toString() + " [" + date.getTime() + "]> not as expected <" + dateAsISOString + "[" + dateAsLong + "]>." +
                "\nUsing fileName <" + fileName + "> and dateFormatString <" + dateFormatString + ">",
                date.getTime() == dateAsLong );
  }

  public void testGetDateUsingRegExp()
  {
    String matchPattern = ".*([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2}).*grib";
    String substitutionPattern = "$1-$2-$3T$4:$5";
    Date date = DateFromString.getDateUsingRegExp( fileName, matchPattern, substitutionPattern );
    assertTrue( "Calculated date <" + date.toString() + " [" + date.getTime() + "]> not as expected <" + dateAsISOString + "[" + dateAsLong + "]>." +
                "\nUsing fileName <" + fileName + ">, matchPattern <" + matchPattern + ">, and substitutionPattern <" + substitutionPattern + ">",
                date.getTime() == dateAsLong );
  }

  public void testGetDateUsingRegExpAndDateFormat()
  {
    String matchPattern = ".*([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2}).*grib";
    String substitutionPattern = "$1$2$3_$4$5";
    String dateFormatString = "yyyyMMdd_HHmm";

    Date date = DateFromString.getDateUsingRegExpAndDateFormat( fileName, matchPattern, substitutionPattern, dateFormatString );
    assertTrue( "Calculated date <" + date.toString() + " [" + date.getTime() + "]> not as expected <" + dateAsISOString + "[" + dateAsLong + "]>." +
                "\nUsing fileName <" + fileName + ">, matchPattern <" + matchPattern + ">, substitutionPattern <" + substitutionPattern + ">, and dateFormatString <" + dateFormatString + ">",
                date.getTime() == dateAsLong );
  }
}
/*
 * $Log: TestDateFromString.java,v $
 * Revision 1.1  2005/11/30 21:01:47  edavis
 * Add thredds.util.DateFromString to provide convenience methods for getting
 * dates from strings.
 *
 */