package ucar.unidata.util;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * _more_
 *
 * @author edavis
 * @since May 4, 2007 1:01:53 PM
 */
public class DateUtil
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( DateUtil.class );

  public static String getCurrentSystemTimeAsISO8601()
  {
    long curTime = System.currentTimeMillis();
    Calendar cal = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ) );
    cal.setTimeInMillis( curTime );
    Date curSysDate = cal.getTime();

    return DateFormatHandler.ISO_DATE_TIME.getDateTimeStringFromDate( curSysDate );
  }

  /**
   * Class for dealing with date/time formats.
   */
  static class DateFormatHandler
  {
    // Available date format handlers.
    public final static DateFormatHandler ISO_DATE = new DateFormatHandler( "yyyy-MM-dd" );
    public final static DateFormatHandler ISO_TIME = new DateFormatHandler( "HH:mm:ss.SSSz" );
    public final static DateFormatHandler ISO_DATE_TIME = new DateFormatHandler( "yyyy-MM-dd\'T\'HH:mm:ssz" );
    public final static DateFormatHandler ISO_DATE_TIME_MILLIS = new DateFormatHandler( "yyyy-MM-dd\'T\'HH:mm:ss.SSSz" );

    private String dateTimeFormatString = null;

    private DateFormatHandler( String dateTimeFormatString )
    {
      this.dateTimeFormatString = dateTimeFormatString;
    }

    public String getDateTimeFormatString()
    {
      return this.dateTimeFormatString;
    }

    /**
     * Return a java.util.Date given a date string using the date/time format string
     * or null if can't parse the given date string.
     *
     * @param dateTimeString - date/time string to be used to set java.util.Date.
     * @return The java.util.Date set by the given date/time string or null.
     */
    public Date getDateFromDateTimeString( String dateTimeString )
    {
      Date theDate = null;

      SimpleDateFormat dateFormat = new SimpleDateFormat( this.dateTimeFormatString, Locale.US );
      dateFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

      try
      {
        theDate = dateFormat.parse( dateTimeString );
      }
      catch ( ParseException e )
      {
        log.warn( e.getMessage() );
        return null;
      }

      return theDate;
    }

    /**
     * Return the date/time string that represents the given a java.util.Date
     * in the format of this DataFormatHandler.
     *
     * @param date - the Date to be formatted into a date/time string.
     * @return The date/time string formatted from the given Date.
     */
    public String getDateTimeStringFromDate( Date date )
    {
      SimpleDateFormat dateFormat = new SimpleDateFormat( this.dateTimeFormatString, Locale.US );
      dateFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

      String dateString = dateFormat.format( date );

      return ( dateString );
    }
  }

}
