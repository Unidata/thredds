/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Test DateFromString
 *
 * @author edavis
 * @since Nov 29, 2005 6:14:37 PM
 */
public class TestDateFromString {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String fileName = "xzy_tds_20051129_1235_junk.grib";
  private String dateAsISOString = "2005-11-29T12:35";
  private long dateAsLong = 1133267700000L;

  @Test
  public void testGetDateUsingSimpleDateFormat() {
    String dateFormatString = "yyyyMMdd_HHmm";
    Date date = DateFromString.getDateUsingSimpleDateFormat(fileName, dateFormatString);
    Assert.assertEquals("Calculated date <" + date.toString() + " [" + date.getTime() + "]> not as expected <" + dateAsISOString + "[" + dateAsLong + "]>." +
                    "\nUsing fileName <" + fileName + "> and dateFormatString <" + dateFormatString + ">",
            date.getTime(), dateAsLong);
  }

  @Test
  public void testGetDateUsingCompleteDateFormat() {
    String dateFormatString = "'xzy_tds_'yyyyMMdd_HHmm'_junk.grib'";

    Date date = DateFromString.getDateUsingCompleteDateFormat(fileName, dateFormatString);
    System.out.printf("date = %s%n", date);
    Assert.assertEquals("Calculated date <" + date.toString() + " [" + date.getTime() + "]> not as expected <" + dateAsISOString + "[" + dateAsLong + "]>." +
                    "\nUsing fileName <" + fileName + "> and dateFormatString <" + dateFormatString + ">",
            date.getTime(),dateAsLong);
  }

  @Test
  public void testGetDateUsingRegExp() {
    String matchPattern = ".*([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2}).*grib";
    String substitutionPattern = "$1-$2-$3T$4:$5";
    Date date = DateFromString.getDateUsingRegExp(fileName, matchPattern, substitutionPattern);
    Assert.assertEquals("Calculated date <" + date.toString() + " [" + date.getTime() + "]> not as expected <" + dateAsISOString + "[" + dateAsLong + "]>." +
                    "\nUsing fileName <" + fileName + ">, matchPattern <" + matchPattern + ">, and substitutionPattern <" + substitutionPattern + ">",
            date.getTime(),dateAsLong);
  }

  @Test
  public void testGetDateUsingRegExpAndDateFormat() {
    String matchPattern = ".*([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2}).*grib";
    String substitutionPattern = "$1$2$3_$4$5";
    String dateFormatString = "yyyyMMdd_HHmm";

    Date date = DateFromString.getDateUsingRegExpAndDateFormat(fileName, matchPattern, substitutionPattern, dateFormatString);
    Assert.assertEquals("Calculated date <" + date.toString() + " [" + date.getTime() + "]> not as expected <" + dateAsISOString + "[" + dateAsLong + "]>." +
                    "\nUsing fileName <" + fileName + ">, matchPattern <" + matchPattern + ">, substitutionPattern <" + substitutionPattern + ">, and dateFormatString <" + dateFormatString + ">",
            date.getTime(),dateAsLong);
  }


  @Test
  public void testFromMain() throws ParseException {
   /*  dateString =  /data/anything/2006070611/wrfout_d01_2006-07-06_080000.nc
   *  dateFormatString =                    #wrfout_d01_#yyyy-MM-dd_HHmm
   *  would extract the date 2006-07-06T08:00
   *
   *  dateString =  /data/anything/2006070611/wrfout_d01_2006-07-06_080000.nc
   *  dateFormatString =          yyyyMM-ddHH#/wrfout_d01_#
   *  would extract the date 2006-07-06T11:00
   * </pre>
   *
   * @param dateString the String to be parsed
   * @param dateFormatString the date format String
   * @return the Date that was parsed.
   */

    DateFormatter formatter  = new DateFormatter();
    Date result = DateFromString.getDateUsingDemarkatedMatch("/data/anything/2006070611/wrfout_d01_2006-07-06_080000.nc", "#wrfout_d01_#yyyy-MM-dd_HHmm", '#');
    assert result != null;
    System.out.println(" 2006-07-06_080000 -> "+formatter.toDateTimeStringISO( result));

    result = DateFromString.getDateUsingDemarkatedMatch("C:\\data\\nomads\\gfs-hi\\gfs_3_20061129_0600", "#gfs_3_#yyyyMMdd_HH", '#');
    assert result != null;
    System.out.println(" 20061129_06 -> "+formatter.toDateTimeStringISO( result));

    System.out.println(new SimpleDateFormat("yyyyMMdd_HH").parse("20061129_06"));
    System.out.println(new SimpleDateFormat("yyyyMMdd_HH").parse("20061129_0600"));

  }
}
