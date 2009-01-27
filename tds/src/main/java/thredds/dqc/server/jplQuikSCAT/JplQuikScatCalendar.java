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
// $Id: JplQuikScatCalendar.java 51 2006-07-12 17:13:13Z caron $
package thredds.dqc.server.jplQuikSCAT;

import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * A description
 *
 * User: edavis
 * Date: Feb 11, 2004
 * Time: 2:20:50 PM
 */
public class JplQuikScatCalendar
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( JplQuikScatCalendar.class );

  /** The reference epoch start date. */
  protected Date   epochStartDate = null;
  /** The ISO date/time string encoding of the reference epoch start date. */
  protected String epochStartDateTimeString = null;
  /** The ISO date/time string encoding of the default reference epoch start date. */
  protected String epochStartDateTimeStringDefault = "1970-01-01T00:00:00.000GMT";

  /** ISO date/time string format for parsing and formating ISO date/time strings. */
  protected String isoDateTimeStringFormat = "yyyy-MM-dd\'T\'HH:mm:ss.SSSz";

  /** ISO date string format for parsing and formating ISO date strings. */
  protected String isoDateStringFormat = "yyyy-MM-dd";

  protected Calendar calendar = null;

  /**
   * Constructor given an ISO date/time string (e.g., "1999-01-01T00:00:00.000GMT")
   * to set the reference epoch start date.
   *
   * @param epochStartDateTimeString - the start date of the reference epoch as a ISO date/time string.
   * @throws IllegalArgumentException - if given ISO date/time string can not be parsed.
   * @throws NullPointerException - if given ISO date/time string is null.
   */
  public JplQuikScatCalendar( String epochStartDateTimeString)
  {
    String tmpMsg = null;
    if ( epochStartDateTimeString == null )
    {
      tmpMsg = "JplQuikScatCalendar(): given date/time string was null.";
      log.debug( tmpMsg);
      throw( new IllegalArgumentException( tmpMsg ) );
    }
    this.epochStartDateTimeString = epochStartDateTimeString;

    this.calendar = Calendar.getInstance( TimeZone.getTimeZone("GMT"), Locale.US);
    try
    {
      this.epochStartDate = this.getDateFromIsoDateTimeString( this.epochStartDateTimeString);
    }
    catch (ParseException e)
    {
      tmpMsg = "JplQuikScatCalendar(): could not parse date/time string <" + this.epochStartDateTimeString + "> " +
              "with the ISO date/time format <" + this.isoDateTimeStringFormat + ">.";
      log.debug( tmpMsg);
      throw( (IllegalArgumentException) new IllegalArgumentException( tmpMsg ).initCause( e) );
    }
  }

  /** Return the start date of the reference epoch. */
  public Date getEpochStartDate() { return( this.epochStartDate); }

  /** Return the start  date of the reference epoch as an ISO date string. */
  public String getEpochStartDateString() { return( this.getIsoDateStringFromDate( this.epochStartDate) ); }

  /** Return the start  date of the reference epoch as an ISO date/time string. */
  public String getEpochStartDateTimeString() { return( this.getIsoDateTimeStringFromDate( this.epochStartDate) ); }

  /**
   * Return a java.util.Date given an ISO date/time string.
   *
   * @param isoDateString - ISO date/time string to be used to set java.util.Date.
   * @return The java.util.Date set by the given ISO date/time string.
   */
  public Date getDateFromIsoDateTimeString( String isoDateString)
          throws ParseException
  {
    //log.debug( "getDateFromIsoDateTimeString(): iso date format <" + this.isoDateTimeStringFormat + ">.");
    Date theDate = null;

    SimpleDateFormat dateFormat = new SimpleDateFormat( this.isoDateTimeStringFormat, Locale.US);
    dateFormat.setTimeZone( TimeZone.getTimeZone("GMT"));

    theDate = dateFormat.parse( isoDateString);
    //log.debug( "getDateFromIsoDateTimeString(): date is " + dateFormat.format( theDate));

    return( theDate);
  }

  /**
   * Return an ISO date/time string given a java.util.Date.
   *
   * @param date - the date to be formatted into an ISO date/time string.
   * @return The ISO date/time string formatted from the given Date.
   */
  public String getIsoDateTimeStringFromDate( Date date)
  {
    //log.debug( "getIsoDateTimeStringFromDate(): ISO date/time format <" + this.isoDateTimeStringFormat + ">.");

    SimpleDateFormat dateFormat = new SimpleDateFormat( this.isoDateTimeStringFormat, Locale.US);
    dateFormat.setTimeZone( TimeZone.getTimeZone("GMT"));

    String dateString = dateFormat.format( date);
    //log.debug( "getIsoDateTimeStringFromDate(): done <" + dateString + ">.");

    return( dateString);
  }

  /**
   * Return a java.util.Date given an ISO date string.
   *
   * @param isoDateString - ISO date string to be used to set java.util.Date.
   * @return The java.util.Date set by the given ISO date string.
   */
  public Date getDateFromIsoDateString( String isoDateString)
          throws ParseException
  {
    //log.debug( "getDateFromIsoDateString(): iso date format <" + this.isoDateStringFormat + ">.");
    Date theDate = null;

    SimpleDateFormat dateFormat = new SimpleDateFormat( this.isoDateStringFormat, Locale.US);
    dateFormat.setTimeZone( TimeZone.getTimeZone("GMT"));

    //log.debug( "getDateFromIsoDateString(): parsing date string <" + isoDateString + ">.");
    theDate = dateFormat.parse( isoDateString);
    //log.debug( "getDateFromIsoDateString(): date is " + dateFormat.format( theDate));

    return( theDate);
  }

  /**
   * Return an ISO date string given a java.util.Date.
   *
   * @param date - the date to be formatted into an ISO date string.
   * @return The ISO date string formatted from the given Date.
   */
  public String getIsoDateStringFromDate( Date date)
  {
    //log.debug( "getIsoDateStringFromDate(): ISO date format <" + this.isoDateStringFormat + ">.");

    SimpleDateFormat dateFormat = new SimpleDateFormat( this.isoDateStringFormat, Locale.US);
    dateFormat.setTimeZone( TimeZone.getTimeZone("GMT"));

    String dateString = dateFormat.format( date);
    //log.debug( "getIsoDateStringFromDate(): done <" + dateString + ">.");

    return( dateString);
  }

  /**
   * Given a number of seconds since the epoch, return a java.util.Date.
   *
   * This method is used on the DQC date range values which are given in seconds since the epoch.
   *
   * @param secSinceEpoch - the number of seconds since the epoch.
   * @return The java.util.Date set by the number of seconds since the epoch.
   */
  public Date getDateFromSecondsSinceEpoch( double secSinceEpoch)
  {
    this.calendar.clear();
    this.calendar.setTimeInMillis( this.epochStartDate.getTime() + (long) (secSinceEpoch * 1000.0));

    //log.debug( "getDateFromSecondsSinceEpoch() - given seconds since epoch <" + secSinceEpoch + "> results in date <" + retDate.toString() + ">.");

    return( this.calendar.getTime());
  }

  /**
   * Given a java.util.Date, return the seconds since the epoch.
   *
   * This method is used to determine the allowed date range values for the DQC.
   *
   * @param theDate - the date to represent as seconds since the epoch.
   * @return The number of seconds since the epoch of the java.util.Date.
   */
  public double getSecondsSinceEpochFromDate( Date theDate)
  {
    //log.debug( "getSecondsSinceEpochFromDate(): for date " + theDate.toString());

    //log.debug( "getSecondsSinceEpochFromDate() - epoch is <" +
    //              this.epochStartDate.toString() + " --- " + this.epochStartDate.getTime() + "> and the given date is <" +
    //              theDate.toString() + " --- " + theDate.getTime() +">.");
    double secSinceEpoch = (theDate.getTime() - this.epochStartDate.getTime()) / 1000.0;

    //log.debug( "getSecondsSinceEpochFromDate() - sec since epoch <" + secSinceEpoch + ">");
    return( secSinceEpoch);

  }

}

/*
 * $Log: JplQuikScatCalendar.java,v $
 * Revision 1.5  2006/03/01 23:13:06  edavis
 * Minor fix.
 *
 * Revision 1.4  2006/01/20 20:42:03  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.3  2005/04/05 22:37:02  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.2  2004/04/03 00:44:57  edavis
 * DqcServlet:
 * - Start adding a service that returns a catalog listing all the DQC docs
 *   available from a particular DqcServlet installation (i.e., DqcServlet
 *   config to catalog)
 * JplQuikSCAT:
 * - fix how the modulo nature of longitude selection is handled
 * - improve some log messages, remove some that drastically increase
 *   the size of the log file; fix some 
 * - fix some template strings
 *
 * Revision 1.1  2004/03/05 06:32:02  edavis
 * Add DqcHandler and backend storage classes for the JPL QuikSCAT
 * DODS File Server.
 *
 */