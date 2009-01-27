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
// $Id: JplQuikScatUserQuery.java 51 2006-07-12 17:13:13Z caron $
package thredds.dqc.server.jplQuikSCAT;

import java.util.Date;

import thredds.dqc.UserQuery;

/**
 * A description
 *
 * User: edavis
 * Date: Jan 26, 2004
 * Time: 11:11:15 PM
 */
public class JplQuikScatUserQuery implements UserQuery
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( JplQuikScatUserQuery.class );

  /** Min point of date range (seconds since reference epoch). */
  protected double dateRangeMin = 0.0;
  /** Max point of date range (seconds since reference epoch). */
  protected double dateRangeMax = 0.0;
  protected boolean dateRangeSet = false;

  /** Min point of longitude range (degree_east). */
  protected double longitudeRangeMin = 0.0;
  /** Max point of longitude range (degree_east). */
  protected double longitudeRangeMax = 0.0;
  protected boolean longitudeRangeSet = false;

  protected JplQuikScatCalendar calendar = null;

  /** @noinspection UNUSED_SYMBOL*/
  private JplQuikScatUserQuery() {}

  public JplQuikScatUserQuery( JplQuikScatCalendar calendar )
  {
    if ( calendar == null )
    {
      String tmpMsg = "JplQuikScatUserQuery(): given calendar is null";
      log.debug( tmpMsg );
      throw( new IllegalArgumentException( tmpMsg ) );
    }

    this.calendar = calendar;
  }

  /**
   * Set the date range of this query. The date range minimum must be less than the
   * date range maximum. Both values can be null but not just one.
   *
   * @param minString - the date range minimum (seconds since reference epoch).
   * @param maxString - the date range maximum (seconds since reference epoch).
   * @throws NumberFormatException if min and max value strings do not represent doubles.
   * @throws IllegalArgumentException if one but not both of min and max are null or if min is greater than max.
   */
  public void setDateRange( String minString, String maxString)
  {
    String tmpMsg = null;
    if ( minString != null && maxString != null )
    {
      // Neither date string is null.
      dateRangeMin = Double.parseDouble( minString);
      dateRangeMax = Double.parseDouble( maxString);
      dateRangeSet = true;
      if ( dateRangeMin > dateRangeMax )
      {
        tmpMsg = "setDateRange(strings): date range min <" + dateRangeMin + "> greater than date range max <" + dateRangeMax + ">.";
        log.debug( tmpMsg);
        throw( new IllegalArgumentException( tmpMsg ) );
      }

      log.debug( "setDateRange(strings): date range (in seconds since " + this.calendar.getEpochStartDateString() + "), from " +
                    dateRangeMin + " to " + dateRangeMax + ".");
    }
    else if ( ! ( minString == null && maxString == null ) )
    {
      // One date string is null but not both.
      tmpMsg = "setDateRange(strings): one but not both date strings are null: min <" +
              minString + ">, max <" + maxString + ">.";
      log.debug( tmpMsg );
      throw( new IllegalArgumentException( tmpMsg));
    }
    else
    {
      // Both dates are null.
      dateRangeSet = false;
    }
  }

  /**
   * Set the date range of this query. The date range minimum must be less than the
   * date range maximum.
   *
   * @param min - the date range minimum (seconds since reference epoch).
   * @param max - the date range maximum (seconds since reference epoch).
   * @throws IllegalArgumentException if min is greater than max.
   */
  public void setDateRange( double min, double max)
  {
    String tmpMsg = null;

    dateRangeMin = min;
    dateRangeMax = max;
    dateRangeSet = true;

    if ( dateRangeMin > dateRangeMax )
    {
      tmpMsg = "setDateRange(strings): date range min <" + dateRangeMin + "> greater than date range max <" + dateRangeMax + ">.";
      log.debug( tmpMsg);
      throw( new IllegalArgumentException( tmpMsg ) );
    }

    log.debug( "setDateRange(doubles): date range (in seconds since " +
                  this.calendar.getEpochStartDateString() + "), from " +
                  dateRangeMin + " to " + dateRangeMax + ".");
  }

  public boolean isDateRangeSet() { return( this.dateRangeSet ); }
  public double  getDateRangeMin() { return( this.dateRangeMin ); }
  public double  getDateRangeMax() { return( this.dateRangeMax ); }

  public Date getDateRangeMinDate() { return( this.calendar.getDateFromSecondsSinceEpoch( this.dateRangeMin ) ); }
  public Date getDateRangeMaxDate() { return( this.calendar.getDateFromSecondsSinceEpoch( this.dateRangeMax ) ); }

  /**
   * Set the longitude range of this query. Both values can be null but not just one.
   *
   * @param minString - the longitude range minimum (degrees east).
   * @param maxString - the longitude range maximum (degrees east).
   * @throws NumberFormatException if min and max value strings do not represent doubles.
   * @throws IllegalArgumentException if one but not both of min and max are null.
   */
  public void setLongitudeRange( String minString, String maxString )
  {
    if ( minString != null && maxString != null)
    {
      longitudeRangeMin = Double.parseDouble( minString);
      longitudeRangeMax = Double.parseDouble( maxString);
      longitudeRangeSet = true;

      log.debug( "setLongitudeRange(strings): longitude range (degree_east), from " +
                    longitudeRangeMin + " to " + longitudeRangeMax + ".");
    }
    else if ( ! ( minString == null && maxString == null ) )
    {
      // One date string is null but not both.
      String tmp = "setLongitudeRange(strings): one but not both longitude strings are null: min <" +
              minString + ">, max <" + maxString + ">.";
      log.debug( tmp );
      throw( new IllegalArgumentException( tmp));
    }
    else
    {
      // Both longitudes are null.
      longitudeRangeSet = false;
    }
  }

  /**
   * Set the longitude range of this query.
   *
   * @param min - the longitude range minimum (degrees east).
   * @param max - the longitude range maximum (degrees east).
   */
  public void setLongitudeRange( double min, double max )
  {
    longitudeRangeMin = min;
    longitudeRangeMax = max;
    longitudeRangeSet = true;

    log.debug( "setLongitudeRange(dates): longitude range (degree_east), from " +
                  longitudeRangeMin + " to " + longitudeRangeMax + ".");
  }

  public boolean isLongitudeRangeSet() { return( this.longitudeRangeSet); }
  public double getLongitudeRangeMin() { return( this.longitudeRangeMin ); }
  public double getLongitudeRangeMax() { return( this.longitudeRangeMax ); }

  public boolean isSet()
  {
    if ( this.isDateRangeSet() || this.isLongitudeRangeSet() )
      return( true);
    return( false);
  }

}

/*
 * $Log: JplQuikScatUserQuery.java,v $
 * Revision 1.5  2006/03/01 23:15:26  edavis
 * Minor fix.
 *
 * Revision 1.4  2006/01/20 20:42:03  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.3  2005/04/05 22:37:03  edavis
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
 * Revision 1.1  2004/03/05 06:32:03  edavis
 * Add DqcHandler and backend storage classes for the JPL QuikSCAT
 * DODS File Server.
 *
 */