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
// $Id: JplQuikScatEntry.java 51 2006-07-12 17:13:13Z caron $
package thredds.dqc.server.jplQuikSCAT;

import ucar.ma2.StructureData;
import ucar.ma2.ArrayChar;
import ucar.ma2.Array;
import ucar.ma2.Index;

import java.io.IOException;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Locale;

/**
 * A description
 *
 * User: edavis
 * Date: Feb 6, 2004
 * Time: 4:45:52 PM
 */
public class JplQuikScatEntry
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( JplQuikScatEntry.class );

  private Calendar calendar = null;

  private String dodsUrlVarName =     "QuikSCAT_L2B.DODS_URL";
  private String yearVarName =        "QuikSCAT_L2B.year";
  private String dayOfYearVarName =   "QuikSCAT_L2B.day";
  private String hourVarName =        "QuikSCAT_L2B.hours";
  private String minuteVarName =      "QuikSCAT_L2B.minutes";
  private String secondVarName =      "QuikSCAT_L2B.seconds";
  private String millisecondVarName = "QuikSCAT_L2B.m_seconds";
  private String longitudeVarName =   "QuikSCAT_L2B.longitude";
  private String revNumVarName =      "QuikSCAT_L2B.rev_num";
  private String wvcRowsVarName =     "QuikSCAT_L2B.wvc_rows";

  private String dodsUrl = null;
  private Date date = null;
  private float longitude = 0.0F;
  private int revNum = -999;
  private int wvcRows = -999;

  private StructureData entry = null;

  /**
   * Construct a JPL QuikSCAT catalog entry from a JPL QuikSCAT DFS catalog DODSStructure entry.
   *
   * @param entry - a JPL QuikSCAT DFS catalog StructureData entry.
   *
   * @throws NullPointerException if given catalog entry is null.
   * @throws IllegalArgumentException if given catalog entry is not a JPL QuikSCAT DFS catalog DODSStructure entry.
   */
  public JplQuikScatEntry( StructureData entry)
  {
    if ( entry == null )
    {
      String tmpMsg = "JPlQuikScatEntry(): given DODSStructure backing store is null.";
      //log.debug( tmpMsg);
      throw( new IllegalArgumentException( tmpMsg));
    }

    this.entry = entry;

    this.calendar = Calendar.getInstance( TimeZone.getTimeZone("GMT"), Locale.US);

    try
    {
      this.dodsUrl = this.getCatalogEntryDodsUrl();
      this.date = this.getCatalogEntryDate();
      this.longitude = this.getCatalogEntryLongitude();
      this.revNum = this.getCatalogEntryRevNum();
      this.wvcRows = this.getCatalogEntryWvcRows();
    }
    catch ( IOException e)
    {
      String tmpMsg = "JPlQuikScatEntry(): given DODSStructure did not contain necessary variables.";
      log.debug( tmpMsg);
      throw( new IllegalArgumentException( tmpMsg));

    }
  }

  /** Return this catalog entry's DODS URL. */
  public String getDodsUrl() { return( this.dodsUrl); }

  /** Return this catalog entry's date. */
  public Date getDate() { return( this.date); }

  /** Return this catalog entry's longitude. */
  public float getLongitude() { return( this.longitude); }

  /** Return this catalog entry's rev number. */
  public int getRevNum() { return( this.revNum); }

  /** Return this catalog entry's WVC rows. */
  public int getWvcRows() { return( this.wvcRows); }

  public float setLongitudeMinusModulo()
  {
    this.longitude -= 360.0; // @todo How should this 360 value be known?
    return( this.longitude );
  }

  /**
   * Return the DODS URL for the catalog entry.
   *
   * @return The DODS URL for this catalog entry.
   * @throws java.io.IOException - If data cannot be read from the given catalog entry.
   */
  private String getCatalogEntryDodsUrl( ) throws IOException
  {
    ArrayChar a = (ArrayChar) this.getCatalogEntryVariableArray( this.dodsUrlVarName );

    return ( a.getString() );
  }

  /**
   * Return the date for that catalog entry.
   *
   * @return The date of this catalog entry.
   * @throws java.io.IOException - If data cannot be read from the given catalog entry.
   */
  private Date getCatalogEntryDate( ) throws IOException
  {
    Date retDate = null;
    Array a = null;
    Index index = null;

    // Get the year.
    a = this.getCatalogEntryVariableArray( this.yearVarName );
    index = a.getIndex().set( 0);
    int year = a.getInt( index);

    // Get the day of year.
    a = this.getCatalogEntryVariableArray( this.dayOfYearVarName );
    index = a.getIndex().set( 0);
    int dayOfYear = a.getInt( index);

    // Get the hour.
    a = this.getCatalogEntryVariableArray( this.hourVarName );
    index = a.getIndex().set( 0);
    int hour = a.getInt( index);

    // Get the minute.
    a = this.getCatalogEntryVariableArray( this.minuteVarName );
    index = a.getIndex().set( 0);
    int minute = a.getInt( index);

    // Get the second.
    a = this.getCatalogEntryVariableArray( this.secondVarName );
    index = a.getIndex().set( 0);
    int second = a.getInt( index);

    // Get the millisecond.
    a = this.getCatalogEntryVariableArray( this.millisecondVarName );
    index = a.getIndex().set( 0);
    int millisecond = a.getInt( index);


    retDate = this.getDateFromYearDayHourMinSecMillisecValues( year, dayOfYear,
                                                               hour, minute,
                                                               second, millisecond );
    return( retDate);
  }

  /**
   * Return the longitude for the catalog entry.
   *
   * @return The longitude for this catalog entry.
   * @throws java.io.IOException - If data cannot be read from the given catalog entry.
   */
  private float getCatalogEntryLongitude( ) throws IOException
  {
    Array a = null;
    Index index = null;

    // Get the longitude.
    a = this.getCatalogEntryVariableArray( this.longitudeVarName );
    index = a.getIndex().set( 0);
    float longitude = a.getFloat( index);

    return( longitude);
  }

  /**
   * Return the rev number for the catalog entry.
   *
   * @return The rev number for this catalog entry.
   * @throws java.io.IOException - If data cannot be read from the given catalog entry.
   */
  private int getCatalogEntryRevNum( ) throws IOException
  {
    Array a = null;
    Index index = null;

    // Get the rev number.
    a = this.getCatalogEntryVariableArray( this.revNumVarName );
    index = a.getIndex().set( 0);
    int revNum = a.getInt( index);

    return( revNum);
  }

  /**
   * Return the WVC rows for the catalog entry.
   *
   * @return The WVC rows for this catalog entry.
   * @throws java.io.IOException - If data cannot be read from the given catalog entry.
   */
  private int getCatalogEntryWvcRows( ) throws IOException
  {
    Array a = null;
    Index index = null;

    // Get the WVC rows.
    a = this.getCatalogEntryVariableArray( this.wvcRowsVarName );
    index = a.getIndex().set( 0);
    int wvcRows = a.getInt( index);

    return( wvcRows);
  }

  /**
   * Given the name of a variable in the catalog entry structure,
   * return that variable as a ucar.ma2.Array.
   *
   * @param varName - the name of a variable in the catalog entry structure.
   * @return The requested variable as a ucar.ma2.Array.
   * @throws java.io.IOException - If data cannot be read from the given catalog entry.
   */
  private Array getCatalogEntryVariableArray( String varName )
          throws IOException
  {
    Array a = this.entry.getArray( varName );
    if ( a == null )
    {
      String tmpMsg = "getCatalogEntryVariableArray(): backing store does not contain \"" +
              varName + "\" variable (entry is probably not a JPL QuikSCAT DFS entry).";
      log.debug( tmpMsg);
      throw( new IOException( tmpMsg));
    }

    return ( a );
  }

  /**
   * Return a java.util.Date given the year, day of year, hour, minute, second, and millisecond.
   *
   * This method is used on the date/time values returned with each catalog entry.
   *
   * @param year - the year.
   * @param dayOfYear - the day of the year.
   * @param hour - the hour of the day (0-23).
   * @param minute - the minute of the hour (0-59).
   * @param second - the second  of the minute (0-59).
   * @param millisecond - the millisecond of the second (0-999).
   * @return The java.util.Date set with the given values.
   */
  private Date getDateFromYearDayHourMinSecMillisecValues( int year, int dayOfYear,
                                                           int hour, int minute, int second, int millisecond )
  {
    //log.debug( "getDateFromYearDayHourMinSecMillisecValues(): get date from year <" + year + ">, " +
    //              "day <" + dayOfYear + ">, hour <" + hour + ">, minute <" + minute + ">, second <" + second + ">, " +
    //              "and millisecond  <" + millisecond + ">.");
    Date theDate = null;

    this.calendar.clear();
    this.calendar.set( Calendar.YEAR, year);
    this.calendar.set( Calendar.DAY_OF_YEAR, dayOfYear);
    this.calendar.set( Calendar.HOUR_OF_DAY, hour);
    this.calendar.set( Calendar.MINUTE, minute);
    this.calendar.set( Calendar.SECOND, second);
    this.calendar.set( Calendar.MILLISECOND, millisecond);
    theDate = this.calendar.getTime();

    //log.debug( "getDateFromYearDayHourMinSecMillisecValues(): date is " + theDate.toString() + ".");

    return( theDate);
  }

}

/*
 * $Log: JplQuikScatEntry.java,v $
 * Revision 1.7  2006/03/01 23:14:40  edavis
 * Minor fix.
 *
 * Revision 1.6  2006/01/20 20:42:03  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.5  2005/05/20 01:16:43  caron
 * propagate StructureData, DataType changes
 *
 * Revision 1.4  2005/04/05 22:37:03  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.3  2004/11/19 23:41:53  edavis
 * Fixes to use netCDF-java 2.2 instead of 2.1.
 *
 * Revision 1.2  2004/04/03 00:44:58  edavis
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