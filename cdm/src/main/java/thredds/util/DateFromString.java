// $Id:DateFromString.java 63 2006-07-12 21:50:51Z edavis $
package thredds.util;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;

/**
 * Convenience routines for parsing a String to produce a Date.
 *
 * @author edavis
 * @since Nov 29, 2005 4:53:46 PM
 */
public class DateFromString
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( DateFromString.class );

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
   * Parse the given date string (between the demarcation characters)
   * using the given date format string (as described in
   * java.text.SimpleDateFormat) and return a Date.
   *
   * @param dateString the String to be parsed
   * @param dateFormatString the date format String
   * @return the Date that was parsed.
   */
  public static Date getDateUsingDemarkatedDateFormat( String dateString, String dateFormatString, char demark )
  {
    // the first char of the dateFormatString is the demarcation
    int pos1 = dateFormatString.indexOf( demark);
    //int pos2 = dateString.indexOf( pos1, demark);

    dateFormatString = dateFormatString.substring( pos1+1);

    return getDateUsingCompleteDateFormatWithOffset( dateString, dateFormatString, pos1 );
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
   * Parse the given date string (starting at the given startIndex)using the
   * given date format string (as described in java.text.SimpleDateFormat) and
   * return a Date.
   *
   * @param dateString the String to be parsed
   * @param dateFormatString the date format String
   * @param startIndex the index at which to start parsing the date string
   * @return the Date that was parsed.
   */
  public static Date getDateUsingCompleteDateFormatWithOffset( String dateString, String dateFormatString, int startIndex )
  {
    SimpleDateFormat dateFormat = new SimpleDateFormat( dateFormatString, Locale.US );
    dateFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
    // Might want to do dateFormat.setLenient( false)
    return dateFormat.parse( dateString, new ParsePosition( startIndex ) );
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
    if ( dateStringFormatted == null || dateStringFormatted.length() == 0 )
    {
      return null;
    }

    return getDateUsingCompleteDateFormat( dateStringFormatted.toString(), dateFormatString );
  }
}
/*
 * $Log: DateFromString.java,v $
 * Revision 1.3  2005/12/15 00:52:55  caron
 * *** empty log message ***
 *
 * Revision 1.2  2005/12/02 00:21:36  caron
 * NcML Aggregation
 * WCS subset bug
 *
 * Revision 1.1  2005/11/30 21:01:47  edavis
 * Add thredds.util.DateFromString to provide convenience methods for getting
 * dates from strings.
 *
 */