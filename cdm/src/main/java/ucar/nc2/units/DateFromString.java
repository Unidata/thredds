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

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Convenience routines for parsing a String to produce a Date.
 *
 * @author edavis
 * @since Nov 29, 2005 4:53:46 PM
 */
public class DateFromString
{


  /**
   * Parse the given date string (starting at the first numeric character)
   * using the given date format string (as described in
   * java.text.SimpleDateFormat) and return a Date.
   *
   * @param dateString the String to be parsed
   * @param dateFormatString the date format String
   * @return the Date that was parsed.
   */
  public static Date getDateUsingSimpleDateFormat( String dateString, String dateFormatString )
  {
    // Determine first numeric character in dateString and drop proceeding characters.
    int smallestIndex = dateString.length();
    if ( smallestIndex == 0 ) return null;
    for ( int i = 0; i < 10; i++ )
    {
      int curIndex = dateString.indexOf( String.valueOf( i ) );
      if ( curIndex != -1 && smallestIndex > curIndex )
        smallestIndex = curIndex;
    }

    return getDateUsingCompleteDateFormatWithOffset( dateString, dateFormatString, smallestIndex );
  }

  /**
   * Parse the given date string, starting at a position given by the offset of the demark character in the dateFormatString.
   * The rest of the dateFormatString is the date format string (as described in java.text.SimpleDateFormat).
   * <pre> Example:
   *   dateString =        wrfout_d01_2006-07-06_080000.nc
   *   dateFormatString = wrfout_d01_#yyyy-MM-dd_HHmm
   * </pre>
   * This simple counts over "wrfout_d01_" number of chars in dateString, then applies the remaining dateFormatString.
   *
   * @param dateString the String to be parsed
   * @param dateFormatString the date format String
   * @param demark the demarkation character
   * @return the Date that was parsed.
   */
  public static Date getDateUsingDemarkatedCount( String dateString, String dateFormatString, char demark )
  {
    // the position of the demark char is where to start parsing the dateString
    int pos1 = dateFormatString.indexOf( demark);

    // the rest of the dateFormatString is the SimpleDateFOrmat
    dateFormatString = dateFormatString.substring( pos1+1);

    return getDateUsingCompleteDateFormatWithOffset( dateString, dateFormatString, pos1 );
  }

  /**
   * Parse the given date string (between the demarcation characters)
   * using the given date format string (as described in
   * java.text.SimpleDateFormat) and return a Date.
   * <pre>
   * Example:
   *  dateString =  /data/anything/2006070611/wrfout_d01_2006-07-06_080000.nc
   *  dateFormatString =                    #wrfout_d01_#yyyy-MM-dd_HHmm
   *  would extract the date 2006-07-06T08:00
   *
   *  dateString =  /data/anything/2006070611/wrfout_d01_2006-07-06_080000.nc
   *  dateFormatString =          yyyyMMddHH#/wrfout_d01_#
   *  would extract the date 2006-07-06T11:00
   * </pre>
   *
   * @param dateString the String to be parsed
   * @param dateFormatString the date format String
   * @param demark the demarkation character
   * @return the Date that was parsed.
   */
  public static Date getDateUsingDemarkatedMatch( String dateString, String dateFormatString, char demark )
  {
    // extract the match string
    int pos1 = dateFormatString.indexOf( demark);
    int pos2 = dateFormatString.indexOf( demark, pos1+1);
    if ((pos1 < 0) || (pos2 < 0)) return null;
    String match = dateFormatString.substring(pos1+1, pos2);

    int pos3 = dateString.indexOf(match);
    if (pos3 < 0) return null;

    if (pos1 > 0) {
      dateFormatString = dateFormatString.substring(0, pos1);
      dateString = dateString.substring(pos3-dateFormatString.length(), pos3);
    }  else {
      dateFormatString = dateFormatString.substring(pos2+1);
      dateString = dateString.substring(pos3+match.length());
    }

    return getDateUsingCompleteDateFormatWithOffset( dateString, dateFormatString, 0 );
  }

  public static Double getHourUsingDemarkatedMatch( String hourString, String formatString, char demark )
  {
    // extract the match string
    int pos1 = formatString.indexOf( demark);
    int pos2 = formatString.indexOf( demark, pos1+1);
    if ((pos1 < 0) || (pos2 < 0)) return null;
    String match = formatString.substring(pos1+1, pos2);

    // where does it live in the hour string ?
    int pos3 = hourString.indexOf(match);
    if (pos3 < 0) return null;

    // for now, just match the number of chars
    if (pos1 > 0) {
      hourString = hourString.substring(pos3-pos1, pos3);
    }  else {
      int len = formatString.length() - pos2 - 1;
      int start = pos3 + match.length();
      hourString = hourString.substring(start, start+len);
    }

    return Double.valueOf( hourString);
  }

  /**
   * Parse the given date string using the given date format string (as
   * described in java.text.SimpleDateFormat) and return a Date.
   *
   * @param dateString the String to be parsed
   * @param dateFormatString the date format String
   * @return the Date that was parsed.
   */
  public static Date getDateUsingCompleteDateFormat( String dateString, String dateFormatString )
  {
    return getDateUsingCompleteDateFormatWithOffset( dateString, dateFormatString, 0 );
  }

  /**
   * Parse the given date string (starting at the given startIndex)  using the
   * given date format string (as described in java.text.SimpleDateFormat) and
   * return a Date. Assumes TimeZone is GMT.
   *
   * @param dateString the String to be parsed
   * @param dateFormatString the date format String
   * @param startIndex the index at which to start parsing the date string
   * @return the Date that was parsed.
   */
  public static Date getDateUsingCompleteDateFormatWithOffset( String dateString, String dateFormatString, int startIndex )
  {
    try {
      SimpleDateFormat dateFormat = new SimpleDateFormat( dateFormatString, Locale.US );
      dateFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

      // We have to cut off the dateString, so that it doesnt grab extra characters.
      // ie  new SimpleDateFormat("yyyyMMdd_HH").parse("20061129_06") -> 2006-12-24T00:00:00Z (WRONG!)
      String s = dateString.substring( startIndex, startIndex + dateFormatString.length());
      Date result = dateFormat.parse( s );
      if (result == null)
        throw new RuntimeException("SimpleDateFormat bad ="+dateFormatString+" working on ="+s);
      return result;

    } catch (ParseException e) {
      throw new RuntimeException("SimpleDateFormat = "+dateFormatString+" fails on "+ dateString+ " ParseException:"+e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("SimpleDateFormat = "+dateFormatString+" fails on "+ dateString+ " IllegalArgumentException:"+e.getMessage());
    }
  }

  /**
   * Use regular expression capture group replacement to construct a date
   * string and return the Date that is obtained by parseing the constructed
   * date string using the date format string "yyyy-MM-dd'T'HH:mm".
   *
   * The date string is constructed by matching the given dateString against
   * the given regular expression matchPattern and then using the capture
   * groups from the match to replace the capture group references, i.e., "$n", where n is an
   * integer.
   *
   * @param dateString the String to be parsed
   * @param matchPattern the regular expression String on which to match.
   * @param substitutionPattern the String to use in the capture group replacement.
   * @return the calculated Date
   */
  public static Date getDateUsingRegExp( String dateString,
                                         String matchPattern,
                                         String substitutionPattern )
  {
    String dateFormatString = "yyyy-MM-dd'T'HH:mm";
    return getDateUsingRegExpAndDateFormat( dateString,
                                            matchPattern,
                                            substitutionPattern,
                                            dateFormatString );
  }

  /**
   * The same as getDateUsingRegExp() except the date format string to be used
   * must be specified.
   *
   * @param dateString the String to be parsed
   * @param matchPattern the regular expression String on which to match.
   * @param substitutionPattern the String to use in the capture group replacement.
   * @param dateFormatString the date format string to use in the parsing of the date string.
   * @return the calculated Date
   */
  public static Date getDateUsingRegExpAndDateFormat( String dateString,
                                                      String matchPattern,
                                                      String substitutionPattern,
                                                      String dateFormatString )
  {
    // Match the given date string against the regular expression.
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile( matchPattern );
    java.util.regex.Matcher matcher = pattern.matcher( dateString );
    if ( ! matcher.matches() )
    {
      return null;
    }

    // Build date string to use with date format string by
    // substituting the capture groups into the substitution pattern.
    StringBuffer dateStringFormatted = new StringBuffer();
    matcher.appendReplacement( dateStringFormatted, substitutionPattern );
    if ( dateStringFormatted.length() == 0 )
    {
      return null;
    }

    return getDateUsingCompleteDateFormat( dateStringFormatted.toString(), dateFormatString );
  }

  public static void main(String args[]) throws ParseException {
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
    Date result = getDateUsingDemarkatedMatch( "/data/anything/2006070611/wrfout_d01_2006-07-06_080000.nc", "#wrfout_d01_#yyyy-MM-dd_HHmm", '#' );
    System.out.println(" 2006-07-06_080000 -> "+formatter.toDateTimeStringISO( result));

    result = getDateUsingDemarkatedMatch( "C:\\data\\nomads\\gfs-hi\\gfs_3_20061129_0600", "#gfs_3_#yyyyMMdd_HH", '#' );
    System.out.println(" 20061129_06 -> "+formatter.toDateTimeStringISO( result));

    System.out.println(new SimpleDateFormat("yyyyMMdd_HH").parse("20061129_06"));
    System.out.println(new SimpleDateFormat("yyyyMMdd_HH").parse("20061129_0600"));

  }

}