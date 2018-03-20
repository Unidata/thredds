/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class DateFromString {
  static private final Logger logger = LoggerFactory.getLogger(DateFromString.class);

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
   * This simply counts over "wrfout_d01_" number of chars in dateString, then applies the remaining dateFormatString.
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

    // the rest of the dateFormatString is the SimpleDateFormat
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
    if ((pos1 < 0) || (pos2 < 0)) {
      logger.error("Must delineate Date between 2 '#' chars, dateFormatString = '"+ dateFormatString+"'", new Throwable());
      return null;
    }
    String match = dateFormatString.substring(pos1+1, pos2);

    int pos3 = dateString.indexOf(match);
    if (pos3 < 0) return null;

    if (pos1 > 0) {  // pos1 > 0, date is before the match: "yyyyMMddHH#/wrfout_d01_#"
      dateFormatString = dateFormatString.substring(0, pos1);
      dateString = dateString.substring(pos3-dateFormatString.length(), pos3);

    }  else {        // pos1 == 0, date is after the match: "#wrfout_d01_#yyyy-MM-dd_HHmm"
      dateFormatString = dateFormatString.substring(pos2+1);
      dateString = dateString.substring(pos3+match.length());
    }

    // any leading or trailing "." in the dateFormatString means trim
    int posDot1 = 0;
    while (dateFormatString.charAt(posDot1) == '.')
      posDot1++;
    int posDot2 = dateFormatString.length();
    while (dateFormatString.charAt(posDot2-1) == '.')
      posDot2--;
    if (posDot1 != 0 || posDot2 != dateFormatString.length()) {
      dateFormatString = dateFormatString.substring(posDot1, posDot2);
      dateString = dateString.substring(posDot1, posDot2);
    }

    return getDateUsingCompleteDateFormatWithOffset( dateString, dateFormatString, 0 );
  }

  public static Date getDateUsingDemarkatedMatchOld( String dateString, String dateFormatString, char demark )
  {
    // extract the match string
    int pos1 = dateFormatString.indexOf( demark);
    int pos2 = dateFormatString.indexOf( demark, pos1+1);
    if ((pos1 < 0) || (pos2 < 0)) {
      logger.error("Must delineate Date between 2 '#' chars, dateFormatString = '"+ dateFormatString+"'", new Throwable());
      return null;
    }
    String match = dateFormatString.substring(pos1+1, pos2);

    int pos3 = dateString.indexOf(match);
    if (pos3 < 0) return null;

    if (pos1 > 0) {  // pos1 > 0, date is before the match: "yyyyMMddHH#/wrfout_d01_#"
      dateFormatString = dateFormatString.substring(0, pos1);
      dateString = dateString.substring(pos3-dateFormatString.length(), pos3);

    }  else {        // pos1 == 0, date is after the match: "#wrfout_d01_#yyyy-MM-dd_HHmm"
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
      // eg  new SimpleDateFormat("yyyyMMdd_HH").parse("20061129_06") -> 2006-12-24T00:00:00Z (WRONG!)
      String s;
      if (startIndex + dateFormatString.length() <= dateString.length())
        s = dateString.substring( startIndex, startIndex + dateFormatString.length());
      else
        s = dateString;

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


}