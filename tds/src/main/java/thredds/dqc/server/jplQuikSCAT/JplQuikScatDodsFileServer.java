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
package thredds.dqc.server.jplQuikSCAT;

import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.dods.DODSStructure;
import ucar.nc2.Attribute;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;

import java.io.IOException;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * A description
 *
 * User: edavis
 * Date: Jan 28, 2004
 * Time: 1:21:36 PM
 */
public class JplQuikScatDodsFileServer
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( JplQuikScatDodsFileServer.class );

  // Information about the JPL QuikSCAT catalog (DODS File Server).
  protected String dfsUrl = null;
  protected DODSNetcdfFile theDfsCatalog = null;
  protected DODSStructure catalogSeq = null;
  protected Attribute catTitleAtt = null;
  protected Attribute catAllowedDateRangeStartAtt = null;
  protected Attribute catAllowedDateRangeEndAtt = null;

  protected String catSeqName = null;
  protected String catTitleAttName = null;
  protected String catAllowedDateRangeStartAttName = null;
  protected String catAllowedDateRangeEndAttName = null;

  protected String catTitle = null;
  protected Date allowedDateRangeMin = null;
  protected Date allowedDateRangeMax = null;

  protected double allowedLongitudeRangeMin = 0.0;
  protected double allowedLongitudeRangeMax = 0.0;

  /** The date format used in the DFS DAS for the allowed date range. */
  protected String dfsDateStringFormat = "yyyy/MM/dd";
  /** The date/time format used with the DFS date_time() server-side function. */
  protected String dfsDateTimeFunctionParameterFormat = "yyyy-MM-dd:HH:mm";
  /** The date/time format used in the DFS DODS_Date_Time variable. */
  protected String dfsDateTimeStringFormat = "yyyy/MM/dd:HH:mm:ss z";
  /** I don't think this date format is ever used. */
  protected String dfsYearDayStringFormat = "yyyy/DDD"; // @todo Is this ever used?

  protected Calendar calendar = null;

  /** Constructor */
  /**
   * Constructor.
   * @throws IOException if DODS file server is not reachable or not returning expected objects.
   */
  public JplQuikScatDodsFileServer( ) throws IOException
  {
    String tmpMsg = null;

    this.calendar = Calendar.getInstance( TimeZone.getTimeZone("GMT"), Locale.US);

    // @todo Should get all this information from a config file.
    dfsUrl = "http://dods.jpl.nasa.gov/dods-bin/nph-dods/catalogs/quikscat/L2B/quikscat_L2.dat";
    catSeqName = "QuikSCAT_L2B";
    catTitleAttName = "DODS_Global.DODS_Title";
    catAllowedDateRangeStartAttName = "DODS_Global.DODS_StartDate";
    catAllowedDateRangeEndAttName = "DODS_Global.DODS_EndDate";

    allowedLongitudeRangeMin = 0.0;    // @todo This isn't in DFS DAS so put it here.
    allowedLongitudeRangeMax = 360.0;

    // Open the DODS dataset for this DFS catalog.
    log.debug( "JplQuikScatDodsFileServer(): opening DODS dataset for this DFS catalog <" + this.dfsUrl + ">.");
    try
    {
      this.theDfsCatalog = new DODSNetcdfFile( this.dfsUrl);
    }
    catch (IOException e)
    {
      tmpMsg = "JplQuikScatDodsFileServer(): IO exception thrown while opening DODS dataset <" +
                    this.dfsUrl + "> for this DFS catalog: " + e.getMessage();
      log.debug( tmpMsg, e);
      throw( (IOException) new IOException( tmpMsg).initCause( e));
    }

    // Get the DODS structure for this DFS catalog.
    log.debug( "JplQuikScatDodsFileServer(): getting DODS sequence <" + this.catSeqName + "> for this DFS catalog.");
    this.catalogSeq = (DODSStructure) this.theDfsCatalog.findVariable( this.catSeqName);
    if ( catalogSeq == null)
    {
      tmpMsg = "JplQuikScatDodsFileServer(): DODS Sequence <" + this.catSeqName + "> not found.";
      log.debug( tmpMsg);
      throw( new IOException( tmpMsg));
    }
    catTitleAtt = this.theDfsCatalog.findGlobalAttribute( catTitleAttName );
    // Gather various DFS catalog information: title and allowedRangeMin/allowedRangeMax dates.
    log.debug( "JplQuikScatDodsFileServer(): getting DODS attrib info for this DFS catalog, title and allowed date range.");

    catTitleAtt = this.theDfsCatalog.findGlobalAttribute( catTitleAttName );
    catAllowedDateRangeStartAtt = this.theDfsCatalog.findGlobalAttribute( catAllowedDateRangeStartAttName );
    catAllowedDateRangeEndAtt = this.theDfsCatalog.findGlobalAttribute( catAllowedDateRangeEndAttName );

    if ( catTitleAtt == null ||
            catAllowedDateRangeStartAtt == null ||
            catAllowedDateRangeEndAtt == null)
    {
      tmpMsg = "JplQuikScatDodsFileServer(): title <" + this.catTitleAtt.getStringValue() + ">, allowed date range" +
              " minimum <" + this.catAllowedDateRangeStartAtt.getStringValue() + "> or" +
              " maximum <" + this.catAllowedDateRangeEndAtt.getStringValue() + "> is null.";
      log.debug( tmpMsg );
      throw(new IllegalArgumentException( tmpMsg ));
    }

    this.catTitle = this.catTitleAtt.getStringValue();
    this.allowedDateRangeMin = this.getDateFromDfsDateString( this.catAllowedDateRangeStartAtt.getStringValue() );
    this.allowedDateRangeMax = this.getDateFromDfsDateString( this.catAllowedDateRangeEndAtt.getStringValue() );

    log.debug( "JplQuikScatDodsFileServer(): done.");
  }

  /** Return the title of the DFS catalog. */
  public String getCatalogTitle()
  {
    return( this.catTitle );
  }

  /** Return the date of the minimum point in the allowed date range. */
  public Date getAllowedDateRangeMin()
  {
    return ( this.allowedDateRangeMin );
  }

  /** Return the date of the maximum point in the allowed date range. */
  public Date getAllowedDateRangeMax()
  {
    return ( this.allowedDateRangeMax );
  }

  /** Return the inimum point in the allowed longitude range. */
  public double getAllowedLongitudeRangeMin()
  {
    return ( this.allowedLongitudeRangeMin );
  }

  /** Return the maximum point in the allowed longitude range. */
  public double getAllowedLongitudeRangeMax()
  {
    return ( this.allowedLongitudeRangeMax );
  }

  /**
   * Build an OPeNDAP constraint expression for the DFS catalog given a user request.
   *
   * @param request - the given user request.
   * @return The OPeNDAP constraint expression that will satisfy the request.
   * @throws NullPointerException if given argument is null.
   */
  String buildConstraintExpression( JplQuikScatUserQuery request )
  {
    log.debug( "buildConstraintExpression(): requested date range minimum <" + request.getDateRangeMinDate().toString() +
                  ">, requested date range maximum <" + request.getDateRangeMaxDate().toString() + ">, " +
                  "requested longitude range minimum <" + request.getLongitudeRangeMin() + ">, " +
                  "requested longitude range maximum <" + request.getLongitudeRangeMax() + ">.");

    String ce = null;

    StringBuffer ceSel = new StringBuffer();

    String dateSelCE = null;
    String longSelCE = null;

    // Nothing is needed for the projection part of the CE because the
    // netCDF interface already adds the sequence variable name. Don't
    // request the DODS_Date_Time() manufactured variable because the
    // netCDF interface doesn't know it exists (because it isn't in the
    // DDS). If did request DODS_Date_Time() would add
    // ",DODS_Date_Time(QuikSCAT_L2B)" to the front of the CE.
    // @todo This is a OPeNDAP issue. A non-standard extension to deal with DODS File Servers.

    // Deal with the requested date range.
    if ( request.isDateRangeSet() )
    {
      String startDateString =
              this.getDfsDateTimeFunctionParameterStringFromDate( request.getDateRangeMinDate() );
      String endDateString =
              this.getDfsDateTimeFunctionParameterStringFromDate( request.getDateRangeMaxDate() );

      log.debug( "buildConstraintExpression(): date range as ISO date string, from " +
                    startDateString + " to " + endDateString + ".");

      // Create the selection part of the CE that specifies the date range.
      dateSelCE = "date_time(\"" + startDateString + "\",\"" +  endDateString + "\")";
    }

    // Deal with requested longitude range.
    if ( request.isLongitudeRangeSet())
    {
      // If requested longitude min/max does not define a single point or
      // the full allowed range, build the constraint. Otherwise, leave the
      // longitude part of the constrain blank.
      if ( request.getLongitudeRangeMin() != request.getLongitudeRangeMax() &&
             ( request.getLongitudeRangeMin() != allowedLongitudeRangeMin &&
                 request.getLongitudeRangeMax() != allowedLongitudeRangeMin ) )
      {
        longSelCE = this.catSeqName + ".longitude>" + request.getLongitudeRangeMin() + "&" +
                this.catSeqName + ".longitude<" + request.getLongitudeRangeMax();
        log.debug( "buildConstraintExpression(): got requested longitude range CE <" + longSelCE + ">.");
      }
    }

    // Piece together the CE.
    if ( dateSelCE != null)
    {
      // Add the date selection.
      ceSel.append( dateSelCE);
    }
    if ( longSelCE != null)
    {
      // Add the longitude selection.
      if ( ceSel.length() == 0)
      {
        ceSel.append( longSelCE);
      }
      else
      {
        ceSel.append( "&" ).append( longSelCE );
      }
    }

    // Complete the CE.
    if ( ceSel.length() > 0 )
    {
      ce = "&" + ceSel.toString();
    }

    log.debug( "buildConstraintExpression(): the CE is <" + ce + ">." );
    return( ce);
  }

  /**
   * Find the catalog entries that match the given user request.
   *
   * @param request - the user request.
   * @return An iterator over the sequence of JplQuikScatEntry items.
   * @throws IOException
   */
  public Iterator findMatchingCatalogEntries( JplQuikScatUserQuery request) throws IOException
  {
    Iterator retIt = null;
    String ce = null;

    // Build the DODS constraint expression to match the user request.
    log.debug( "findMatchingCatalogEntries(): build DODS CE.");
    ce = this.buildConstraintExpression( request );

    log.debug( "findMatchingCatalogEntries(): request catalog entries with DODS CE <" + ce + ">." );
    StructureDataIterator dodsIt = this.catalogSeq.getStructureIterator();
    retIt = new DfsIterator( dodsIt );

    return( retIt);
  }

  /**
   * Return a java.util.Date given a DODS File Server (DFS) DODS_Date "yyyy/mm/dd".
   *
   * This method is used on the allowed date range strings given in the DFS DAS.
   *
   * @param theDateString - a date string as given in the DFS catalogs DAS for start and end dates.
   * @return The java.util.Date set with the given date string.
   */
  protected Date getDateFromDfsDateString( String theDateString)
  {
    //log.debug( "getDateFromDfsDateString(): allowedRangeMin - format <" + this.dfsDateStringFormat + ">.");

    SimpleDateFormat dateFormat = new SimpleDateFormat( this.dfsDateStringFormat, Locale.US);
    dateFormat.setTimeZone( TimeZone.getTimeZone("GMT"));

    //log.debug( "getDateFromDfsDateString(): parsing date string <" + theDateString + ">.");
    Date theDate;
    try
    {
      theDate = dateFormat.parse( theDateString);
    }
    catch ( java.text.ParseException e)
    {
      log.debug( "getDateFromDfsDateString(): parsing of date string threw exception: " + e.getMessage());
      return( null);
    }
    this.calendar.clear();
    this.calendar.setTime( theDate);
    this.calendar.set( Calendar.HOUR_OF_DAY, 0);
    this.calendar.set( Calendar.MINUTE, 0);
    this.calendar.set( Calendar.SECOND, 0);
    this.calendar.set( Calendar.MILLISECOND, 0);
    theDate = this.calendar.getTime();
    //log.debug( "getDateFromDfsDateString(): date is " + theDate.toString());

    return( theDate);
  }

  /**
   * Return a java.util.Date given a DODS File Server DODS_Date_Time string.
   *
   * This method is used on the date string returned with each catalog entry.
   *
   * @param dfsDateString - the date string as returned with each catalog entry.
   * @return The java.util.Date set with the given date string.
   * @throws NullPointerException if given argument is null.
   */
  protected Date getDateFromDfsDateTimeString( String dfsDateString)
  {
    if ( dfsDateString == null )
    {
      throw( new IllegalArgumentException() );
    }
    // @todo I don't think this method is actually used. Check then remove if true.
    log.debug( "getDateFromDfsDateTimeString(): allowedRangeMin - format <" + this.dfsDateTimeStringFormat + ">.");
    Date theDate = null;

    SimpleDateFormat dateFormat = new SimpleDateFormat( this.dfsDateTimeStringFormat, Locale.US);
    dateFormat.setTimeZone( TimeZone.getTimeZone("GMT"));

    log.debug( "getDateFromDfsDateTimeString(): parsing date string <" + dfsDateString + ">.");
    try
    {
      theDate = dateFormat.parse( dfsDateString);
    }
    catch ( java.text.ParseException e)
    {
      log.debug( "getDateFromDfsDateTimeString(): parsing of date string threw exception: " + e.getMessage());
      return( null);
    }
    this.calendar.clear();
    this.calendar.setTime( theDate);
    this.calendar.set( Calendar.MILLISECOND, 0);
    theDate = this.calendar.getTime();

    log.debug( "getDateFromDfsDateTimeString(): date is " + dateFormat.format( theDate));

    return( theDate);
  }

  /**
   * Return a date/time string as used in the DFS date_time() server-side function given a java.util.Date.
   *
   * This method is used on the Dates that define the requested date range, i.e., when the CE is being built.
   *
   * @param theDate - one of the dates to be encoded for use in the DFS date_time() server-side function.
   * @return The date string to be used in the DFS date_time() server-side function.
   * @throws NullPointerException if the given date is null.
   */
  protected String getDfsDateTimeFunctionParameterStringFromDate( Date theDate)
  {
    log.debug( "getDfsDateTimeFunctionParameterStringFromDate(): format the given date <" +
                  theDate.toString() + "> with the DFS date_time() server-side function string format <" +
                  this.dfsDateTimeFunctionParameterFormat + ">.");

    SimpleDateFormat dateFormat = new SimpleDateFormat( this.dfsDateTimeFunctionParameterFormat, Locale.US);
    dateFormat.setTimeZone( TimeZone.getTimeZone("GMT"));

    String dateString = dateFormat.format( theDate);
    log.debug( "getDfsDateTimeFunctionParameterStringFromDate(): done <" + dateString + ">.");

    return( dateString);
  }

  /**
   * Private non-static inner class to encapsulate iterator of DODSStructures
   * (from DODSSequence.iterator()) with an iterator of JplQuikScatEntry elements.
   * This iterator will not return any null items.
   */
  private class DfsIterator implements Iterator
  {
    /** The iterator of DODSStructures to be encapsulated. **/
    private ucar.ma2.StructureDataIterator dfsIterator = null;

    /** Hold next entry. */
    private JplQuikScatEntry nextEntry = null;

    /** Indicate if this iterator has no more elements. (Mostly
     *  for if backing iterator contains wrong information.) */
    private boolean done = false;

    /** The number of items retrieved from the backing store iterator (used for debug/logging only). */
    private int curItemNumFromBackingStore = 0;
    /** The number of items returned from this iterator (used for debug/logging only). */
    private int curItemNum = 0;

    protected DfsIterator( ucar.ma2.StructureDataIterator dfsIterator)
    {
      // Set backing storage to the given iterator.
      this.dfsIterator = dfsIterator;
      if ( this.dfsIterator == null )
      {
        // If the backing storage is null, this iterator is done.
        this.done = true;
      }
    }

    /** Return true if there is another element, false otherwise. */
    public boolean hasNext()
    {
      String tmpMsg = null;

      // If this iterator is marked as done, return false.
      if ( this.done )
      {
        return( false);
      }

      // Check for next entry.
      if ( this.nextEntry != null )
      {
        // Already have next entry.
        return( true);
      }
      else
      {
        try {
          while ( this.dfsIterator.hasNext())
          {
            // More items in backing iterator.
            StructureData ds = null;
            try
            {
              this.curItemNumFromBackingStore++;
              ds = this.dfsIterator.next();
            }
            catch( IOException e)
            {
              tmpMsg = "IOException accessing next item in iterator " +
                      "< item numbers - " + this.curItemNum + " - " + this.curItemNumFromBackingStore + ">: " + e.getMessage();
              log.debug( tmpMsg);
              continue;
            }
            catch( ClassCastException e)
            {
              // Item from backing iterator was not DODSStructure, try next item.
              tmpMsg = "DfsIterator.next(): item not a DODSStructure " +
                      "< item numbers - " + this.curItemNum + " - " + this.curItemNumFromBackingStore + ">: " + e.getMessage();
              log.debug( tmpMsg);
              continue;
            }

            if ( ds == null )
            {
              // Item from backing iterator was null, try next item.
              // @todo Can this happen or will catch of ClassCastException above deal w/ this situation.
              tmpMsg = "DfsIterator.next(): entry was null " +
                      "< item numbers - " + this.curItemNum + " - " + this.curItemNumFromBackingStore +
                      ">.";
              log.debug( tmpMsg);
              continue;
            }
            // Construct a JplQuikScatEntry from the current entry.
            try
            {
              this.nextEntry = new JplQuikScatEntry( ds);
            }
            catch (IllegalArgumentException e)
            {
              // Item from backing iterator was not a JPL QuikSCAT DFS DODSStructure catalog entry, skip to next item.
              // @todo Is there a better way to deal with this than catch a RuntimeException?
              tmpMsg = "DfsIterator.next(): IllegalArgumentException while reading entry (i.e., " +
                      "entry not a JPL QuikSCAT DFS DODSStructure catalog entry)" +
                      "< item numbers - " + this.curItemNum + " - " + this.curItemNumFromBackingStore +
                      ">: " + e.getMessage();
              log.debug( tmpMsg);
              continue;
            }
            catch ( Exception e )
            {
              // Item from backing iterator could not be made into a JplQuikScatEntry, try next item.
              tmpMsg = "DfsIterator.next(): Exception while creating entry " +
                      "< item numbers - " + this.curItemNum + " - " + this.curItemNumFromBackingStore +
                      ">: " + e.getMessage();
              log.debug( tmpMsg);
              continue;
            }

            // Got a valid item from backing iterator, return true;
            this.curItemNum++;
            return( true);
          }
        } catch (IOException e) {
              tmpMsg = "IOException accessing next item in iterator " +
                      "< item numbers - " + this.curItemNum + " - " + this.curItemNumFromBackingStore + ">: " + e.getMessage();
              log.debug( tmpMsg);
        }

        // No more items in backing iterator, mark as done and return false.
        this.done = true;
        this.nextEntry = null;
        tmpMsg = "DfsIterator.next(): no more items in backing iterator" +
                " < item numbers - " + this.curItemNum + " - " + this.curItemNumFromBackingStore + ">.";
        log.debug( tmpMsg);
        return( false );
      }
    }

    /** Return the next element in the iteration. */
    public Object next()
    {
      String tmpMsg = null;

      // If this iterator is done, throw NoSuchElementException.
      if ( this.done )
      {
        tmpMsg = "DfsIterator.next(): iteration is done < item numbers - " + this.curItemNum + " - " +
                this.curItemNumFromBackingStore + ">, throwing NoSuchElementException.";
        log.debug( tmpMsg);
        throw( new NoSuchElementException( tmpMsg));
      }

      JplQuikScatEntry retVal = null;
      if ( this.nextEntry != null)
      {
        // User called hasNext() and got a 'true' response,, return the current item.
        retVal = this.nextEntry;
        this.nextEntry = null;
        return( retVal);
      }
      else
      {
        if ( this.hasNext())
        {
          // User didn't call hasNext() but there are still items in the backing iterator,
          // return the next item from backing store.
          retVal = this.nextEntry;
          this.nextEntry = null;
          return( retVal);
        }
        else
        {
          // User didn't call hasNext() and there are no items remaining
          // in backing iterator, throw a NoSuchElementException.
          this.done = true;
          tmpMsg = "DfsIterator.next(): iteration is done <item numbers - " + this.curItemNum + " - " +
                this.curItemNumFromBackingStore + ">, throwing NoSuchElementException.";
          log.debug( tmpMsg);
          throw( new NoSuchElementException( tmpMsg));
        }
      }
    }

    public void remove()
    {
      String tmpMsg = "DfsIterator.remove(): unsupported operation - backing store unknown (part of ucar.nc2.dods package).";
      log.debug( tmpMsg);
      throw( new UnsupportedOperationException( tmpMsg));
    }

  }
}