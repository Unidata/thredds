// $Id: TestDateFromString.java 51 2006-07-12 17:13:13Z caron $
package thredds.util;

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
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( TestDateFromString.class );

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