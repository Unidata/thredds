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

import thredds.dqc.SelectFromRange;
import thredds.dqc.server.DqcHandler;
import thredds.catalog.*;
import thredds.catalog.query.*;
import thredds.servlet.UsageLog;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

/**
 * A description
 *
 * User: edavis
 * Date: Jan 2, 2004
 * Time: 3:24:38 PM
 */
public class JplQuikSCAT extends DqcHandler
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( JplQuikSCAT.class );

  // The URL of this DQC server.
  // @todo This should be in the config information.
  protected String dqcServletUrl = "http://dods.jpl.nasa.gov/dqcServlet/quikscat";

  /** Information about the JPL QuikSCAT DFS catalog. */
  protected JplQuikScatDodsFileServer jplQsDfs = null;

  // The two selectors in the JPL QuikSCAT DQC.
  // @todo The information to setup these should be in the config information.
  protected SelectFromRange allowedDateRange = null;
  protected SelectFromRange allowedLongitudeRange = null;

  protected List resultSortedList = null;
  protected Comparator comparator = null;

  // The epoch used for the date information stored as doubles with
  // units of "seconds since 1999-01-00".
  // @todo This should be in the config information.
  protected String epochStartDateTimeString = "1999-01-01T00:00:00.000GMT";

  protected JplQuikScatCalendar jplQuikScatCalendar = null;

  /**
   * JplQuikSCAT implementation of the DqcHandler initialization method.
   *
   * Remember, this method is called by the DqcHandler factory method.
   *
   * @param configDocURL - a URL to the config doc for this DqcHandler, not used (null) for this handler.
   * @throws IOException if trouble reading config document.
   * @throws IllegalArgumentException if config document does not contain the needed information.
   */
  public void initWithHandlerConfigDoc( URL configDocURL )
          throws IOException
  {
    log.debug( "initWithHandlerConfigDoc(): start.");
    this.jplQsDfs = new JplQuikScatDodsFileServer();
    this.allowedDateRange = new SelectFromRange();
    this.allowedLongitudeRange = new SelectFromRange();
    this.resultSortedList = new ArrayList(); // @todo Would LinkedList be better for adding elements to sort one at a time.

    this.jplQuikScatCalendar = new JplQuikScatCalendar( this.epochStartDateTimeString);
    log.debug( "initWithHandlerConfigDoc(): epoch start date set <" + this.jplQuikScatCalendar.getEpochStartDate().toString() + ">.");

    // Setup the allowed date range information.
    this.allowedDateRange.setTitle( "Select Date Range");
    this.allowedDateRange.setMultiple( false);
    this.allowedDateRange.setRequired( false);

    this.allowedDateRange.setAllowedRange(
            this.jplQuikScatCalendar.getSecondsSinceEpochFromDate(
                    this.jplQsDfs.getAllowedDateRangeMin() ),
            this.jplQuikScatCalendar.getSecondsSinceEpochFromDate(
                    this.jplQsDfs.getAllowedDateRangeMax() ) );

    this.allowedDateRange.setUnits( "seconds since " + this.jplQuikScatCalendar.getEpochStartDateString() );
    this.allowedDateRange.setTemplate( "minDate={minDate}&amp;maxDate={maxDate}");
    this.allowedDateRange.setModulo( false);
    log.debug( "initWithHandlerConfigDoc(): allowed date range info set: " +
                  "title=\"" + this.allowedDateRange.getTitle() + "\"; " +
                  "multiple=\"" + this.allowedDateRange.isMultiple() + "\"; " +
                  "required=\"" + this.allowedDateRange.isRequired() + "\";" +
                  " min=\"" + this.allowedDateRange.getAllowedRangeMin() + "\";" +
                  " max=\"" + this.allowedDateRange.getAllowedRangeMax() + "\"; " +
                  "units=\"" + this.allowedDateRange.getUnits() + "\"; " +
                  "modulo=\"" + this.allowedDateRange.isModulo() + "\"; " +
                  "template=\"" + this.allowedDateRange.getTemplate() + "\".");

    // Setup the allowed longitude range information.
    this.allowedLongitudeRange.setTitle( "Northerly Equatorial Crossing (longitude)");
    this.allowedLongitudeRange.setDescription(
            "The longitude at which the satellite crosses the equator from the" +
            "southern into the northern hemisphere." );
    this.allowedLongitudeRange.setMultiple( true);
    this.allowedLongitudeRange.setRequired( false);

    this.allowedLongitudeRange.setAllowedRange( this.jplQsDfs.getAllowedLongitudeRangeMin(),
                                                this.jplQsDfs.getAllowedLongitudeRangeMax() );
    this.allowedLongitudeRange.setUnits( "degree_east");  // @todo How do I know the units?
    this.allowedLongitudeRange.setTemplate( "minCross={minCross}&amp;maxCross={maxCross}" );
    this.allowedLongitudeRange.setModulo( true);
    log.debug( "initWithHandlerConfigDoc(): allowed longitude range info set: " +
                  "title=\"" + this.allowedLongitudeRange.getTitle() + "\"; " +
                  "multiple=\"" + this.allowedLongitudeRange.isMultiple() + "\"; " +
                  "required=\"" + this.allowedLongitudeRange.isRequired() + "\";" +
                  " min=\"" + this.allowedLongitudeRange.getAllowedRangeMin() + "\";" +
                  " max=\"" + this.allowedLongitudeRange.getAllowedRangeMax() + "\"; " +
                  "units=\"" + this.allowedLongitudeRange.getUnits() + "\"; " +
                  "modulo=\"" + this.allowedLongitudeRange.isModulo() + "\"; " +
                  "template=\"" + this.allowedLongitudeRange.getTemplate() + "\".");
  }

  /**
   *
   * @param req
   * @param res
   * @throws IOException if an input or output error is detected while the servlet handles the GET request.
   * @throws ServletException if the request can't be handled for some reason.
   */
  public void handleRequest( HttpServletRequest req, HttpServletResponse res )
          throws IOException, ServletException
  {
    String tmpMsg = null;

    // Get request path.
    String reqPath = req.getPathInfo();
    // Get path after this handler's name.
    String extraPath = reqPath.substring( this.getHandlerInfo().getName().length() + 1 );

    // Deal with request for DQC document.
    if ( extraPath.equals( ".xml" ) )
    {
      // Create the DQC document.
      QueryCapability dqc = createDqcDocument( req.getContextPath() + req.getServletPath() + "/" + this.getHandlerInfo().getName() );
      DqcFactory dqcFactory = new DqcFactory( false );
      //if ( this.dqcSpecVersion.equals( "0.3"))
      String dqcAsString = dqcFactory.writeXML( dqc );

      // Write DQC doc as response
      PrintWriter out = res.getWriter();
      res.setContentType( "text/xml" );
      res.setStatus( HttpServletResponse.SC_OK );
      out.print( dqcAsString );
      log.debug( "handleRequest(): done writing DQC doc as response." );
      return;
    }

    // If not a request for DQC document, should not be any extra path info.
    if ( extraPath.length() > 0 )
    {
      tmpMsg = "Extra path information <" + extraPath + "> not understood.";
      log.error( "handleRequest(): " + tmpMsg );
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ));
      return;
    }

    // Get parameters for DQC request.
    Enumeration params = req.getParameterNames();
    StringBuffer tmp = new StringBuffer( "Request parameter names: ** ");
    while ( params.hasMoreElements())
    {
      tmp.append( params.nextElement() + " ** ");
    }
    log.debug( "handleRequest(): " + tmp.toString());
    String requestedDateRangeMin = req.getParameter( "minDate");
    String requestedDateRangeMax = req.getParameter( "maxDate");
    String requestedLongitudeRangeMin = req.getParameter( "minCross");
    String requestedLongitudeRangeMax = req.getParameter( "maxCross");
    log.debug( "handleRequest(): handling request - " +
                  "minDate <" + requestedDateRangeMin + ">, maxDate <" + requestedDateRangeMax + ">, " +
                  "minCross <" + requestedLongitudeRangeMin + ">, maxCross <" + requestedLongitudeRangeMax + ">.");

    // Create an InvCatalog in response to the DQC request.
    InvCatalog resultingCatalog = null;
    double requestedDateRangeMinSecSinceEpoch;
    double requestedDateRangeMaxSecSinceEpoch;
    try
    {
      requestedDateRangeMinSecSinceEpoch = this.jplQuikScatCalendar.getSecondsSinceEpochFromDate(
                    this.jplQuikScatCalendar.getDateFromIsoDateTimeString( requestedDateRangeMin));
      requestedDateRangeMaxSecSinceEpoch = this.jplQuikScatCalendar.getSecondsSinceEpochFromDate(
                    this.jplQuikScatCalendar.getDateFromIsoDateTimeString( requestedDateRangeMax));
    }
    catch ( ParseException e )
    {
      tmpMsg = "Requested date range <" + requestedDateRangeMin + "-" + requestedDateRangeMax + "> " +
               "could not be parsed: " + e.getMessage() ;
      log.debug( "handleRequest(): " + tmpMsg, e);
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ));
      return;
    }

    try
    {
      resultingCatalog = this.buildCatalogFromRequest( String.valueOf( requestedDateRangeMinSecSinceEpoch),
                                                       String.valueOf( requestedDateRangeMax),
                                                       requestedLongitudeRangeMin, requestedLongitudeRangeMax );
    }
    catch (IOException e)
    {
      tmpMsg = "Failed to read needed information from backing store: " + e.getMessage();
      log.debug( "handleRequest(): " + tmpMsg, e);
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ));
      return;
    }
    catch (IllegalArgumentException e)
    {
      tmpMsg = "Invalid Request - requested date range <" + requestedDateRangeMin + "-" + requestedDateRangeMax + "> " +
              "outside allowed <" + this.allowedDateRange.getAllowedRangeMin() + "-" + this.allowedDateRange.getAllowedRangeMax() + "> " +
              "or min greater than max - requested longitude range <" + requestedLongitudeRangeMin + "-" + requestedLongitudeRangeMax + "> " +
              "outside allowed <" + this.allowedLongitudeRange.getAllowedRangeMin() + "-" + this.allowedLongitudeRange.getAllowedRangeMax() + ">: " + e.getMessage();
      log.debug( "handleRequest(): " + tmpMsg, e);
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ));
      return;
    }

    log.debug( "handleRequest(): successfully built catalog, writing response.");
    PrintWriter out = res.getWriter();
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( true );
    String catalogAsString = fac.writeXML_1_0( (InvCatalogImpl) resultingCatalog );

    res.setContentType( "text/xml" );
    res.setStatus( HttpServletResponse.SC_OK );
    out.print( catalogAsString );
    log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, catalogAsString.length() ));
    return;
  }

  protected QueryCapability createDqcDocument( String baseURI )
  {
    // Create DQC root elements (QueryCapability and Query).
    QueryCapability dqc = new QueryCapability( null, "NASA Jet Propulsion Lab, Global Level-2B QuikSCAT Archive", "0.3" );
    Query query = new Query( baseURI + "?", null, null );
    dqc.setQuery( query );

    // Create the SelectService and add to the DQC.
    SelectService selectService = new SelectService( "serviceType", "Select service type." );
    selectService.addServiceChoice( "OpenDAP", "OPeNDAP/DODS", null, null, null );
    selectService.setRequired( "false" );
    dqc.addUniqueSelector( selectService );
    dqc.setServiceSelector( selectService );

    // Create the SelectRangeDate and add to the DQC.
    //SelectRangeDate selectDateRange = new SelectRangeDate( "1999-07-19T19:00 GMT", "present", null, "P1H40M", null );
    SelectRangeDate selectDateRange = new SelectRangeDate(
            this.jplQuikScatCalendar.getIsoDateTimeStringFromDate(
                    this.jplQuikScatCalendar.getDateFromSecondsSinceEpoch( this.allowedDateRange.getAllowedRangeMin())),
         //   "present", null, "P1H40M", null );
            "present", null, "100 minutes", null );
    selectDateRange.setId( "dateRange" );
    selectDateRange.setTitle( "Select Date Range");
    selectDateRange.setTemplate( "minDate={start}&maxDate={end}&" );
    dqc.addUniqueSelector( selectDateRange );

    // Create the equatorial crossing SelectRange and add to the DQC
    SelectRange selectCrossingLongRange = new SelectRange( "0.0", "360.0", "degree_east", "true", null, null );
    selectCrossingLongRange.setId( "crossRange");
    selectCrossingLongRange.setTitle( "Northerly Equatorial Crossing (longitude)" );
    selectCrossingLongRange.setTemplate( "minCross={min}&maxCross={max}&" );
    dqc.addUniqueSelector( selectCrossingLongRange );

    return ( dqc );
  }

  /**
   * Given the request parameters as strings, return an InvCatalog.
   *
   * @param requestedDateRangeStart - the minimum point of the requested Date range (a string of a float in seconds since 1999-01-01).
   * @param requestedDateRangeEnd - the minimum point of the requested Date range (a string of a float in seconds since 1999-01-01).
   * @param requestedLongitudeRangeStart - the minimum point of the requested Longitude range (a string of a float in degrees east).
   * @param requestedLongitudeRangeEnd - the maximum point of the requested Longitude range (a string of a float in degrees east).
   * @return an InvCatalog containing the datasets from the requested date range and longitude range.
   * @throws java.io.IOException if reading information from backing store fails.
   * @throws IllegalArgumentException if the request parameters are invalid.
   */
  protected InvCatalog buildCatalogFromRequest( String requestedDateRangeStart, String requestedDateRangeEnd,
                                                String requestedLongitudeRangeStart, String requestedLongitudeRangeEnd )
          throws IOException
  {
    String tmpMsg;
    Iterator dsIt1 = null;
    Iterator dsIt2 = null;
    JplQuikScatEntry curEntry = null;

    JplQuikScatUserQuery request = new JplQuikScatUserQuery( this.jplQuikScatCalendar );
    request.setDateRange( requestedDateRangeStart, requestedDateRangeEnd );
    request.setLongitudeRange( requestedLongitudeRangeStart, requestedLongitudeRangeEnd );
    log.debug( "buildCatalogFromRequest(): request isDateSet <" + request.isDateRangeSet() + ">, " +
                  "minDate <" + request.getDateRangeMin() + ">, maxDate <" + request.getDateRangeMax() + ">, " +
                  "isCrossSet <" + request.isLongitudeRangeSet() + ">, " +
                  "minCross <" + request.getLongitudeRangeMin() + ">, maxCross <" + request.getLongitudeRangeMax() + ">.");

    if ( ! this.validateRequest( request ) )
    {
      tmpMsg = "Invalid request: minDate <" + request.getDateRangeMin() + "> " +
              "maxDate <" + request.getDateRangeMax() + "> minCross <" + request.getLongitudeRangeMin() + "> " +
              "maxCross <" + request.getLongitudeRangeMax() + ">.";
      log.debug( "buildCatalogFromRequest(): " + tmpMsg);
      throw( new IllegalArgumentException( tmpMsg));
    }

    // Set the comparator to sort the entries appropriately.
    if ( request.isDateRangeSet() && ! request.isLongitudeRangeSet() )
    {
      // Sort by date if date range specified but not longitude range.
      this.comparator = new JplQuikScatEntryComparator(
              JplQuikScatEntryComparator.JplQuikScatEntryComparatorType.DATE_REVERSE );
      tmpMsg = "buildCatalogFromRequest(): date range set, not long range - sort by date.";
    }
    else if ( request.isLongitudeRangeSet() && ! request.isDateRangeSet() )
    {
      // Sort by longitude if longitude range specified but not date range.
      this.comparator = new JplQuikScatEntryComparator(
              JplQuikScatEntryComparator.JplQuikScatEntryComparatorType.LONGITUDE );
      tmpMsg = "buildCatalogFromRequest(): long range set, not date range - sort by longitude.";
    }
    else if ( request.isDateRangeSet() && request.isLongitudeRangeSet() )
    {
      // Sort by longitude if both longitude and date ranges are specified.
      this.comparator = new JplQuikScatEntryComparator(
              JplQuikScatEntryComparator.JplQuikScatEntryComparatorType.LONGITUDE );
      tmpMsg = "buildCatalogFromRequest(): long and date ranges set - sort by longitude.";
    }
    else
    {
      // Sort by date if neither longitude nor date ranges are specified.
      this.comparator = new JplQuikScatEntryComparator(
              JplQuikScatEntryComparator.JplQuikScatEntryComparatorType.DATE_REVERSE );
      tmpMsg = "buildCatalogFromRequest(): neither long nor date ranges set - sort by date.";
    }
    log.debug( tmpMsg);

    // Deal with modulo stuff here????
    if ( request.isLongitudeRangeSet() &&
         this.allowedLongitudeRange.isModulo() &&
         request.getLongitudeRangeMin() > request.getLongitudeRangeMax())
    {
      // Request:
      // 0.0                                     360.0
      // |----------------------------------------|
      // |----------*                     *-------|
      //          maxReq                minReq

      // Result:
      // 0.0                                     360.0
      // |----------------------------------------|
      //                                  *-------|----------*
      //                          minReq-360.0   0.0       maxReq

      // Handle the starting longitude section of the request, i.e., from the minimum point
      // of the requested longitude range to the maximum point of the allowed longitude range.
      log.debug( "buildCatalogFromRequest(): longitude crosses modulo range boundary - dealing with minRequest to maxAllowed section.");
      JplQuikScatUserQuery request1 = new JplQuikScatUserQuery( this.jplQuikScatCalendar );
      // Check that date range is set before setting new request.
      if ( request.isDateRangeSet() )
      {
        request1.setDateRange( request.getDateRangeMin(), request.getDateRangeMax());
      }
      request1.setLongitudeRange( request.getLongitudeRangeMin(), this.allowedLongitudeRange.getAllowedRangeMax());
      // @todo This class shouldn't know about DODS CEs, fold buildConstraintExpression() into findMatchingCatalogEntries( request ).

      dsIt1 = this.jplQsDfs.findMatchingCatalogEntries( request1);
      while ( dsIt1.hasNext())
      {
        curEntry = (JplQuikScatEntry) dsIt1.next();
        curEntry.setLongitudeMinusModulo(); // So that values are monotonic and without gaps.
        this.addToResultsList( curEntry, this.comparator );
      }

      // Handle the ending longitude section of the request, i.e., from the minimum point
      // of the allowed longitude range to the maximum point of the requested longitude range.
      log.debug( "buildCatalogFromRequest(): longitude crosses modulo range boundary - dealing with minAllowed to maxRequested section.");
      JplQuikScatUserQuery request2 = new JplQuikScatUserQuery( this.jplQuikScatCalendar );
      // Check that date range is set before setting new request.
      if ( request.isDateRangeSet() )
      {
        request2.setDateRange( request.getDateRangeMin(), request.getDateRangeMax() );
      }
      request2.setLongitudeRange( this.allowedLongitudeRange.getAllowedRangeMin(), request.getLongitudeRangeMax() );
      dsIt2 = this.jplQsDfs.findMatchingCatalogEntries( request2);
      while ( dsIt2.hasNext())
      {
        curEntry = (JplQuikScatEntry) dsIt2.next();
        this.addToResultsList( curEntry, this.comparator );
      }
    }
    else
    {
      log.debug( "buildCatalogFromRequest(): longitude does not cross modulo range boundary.");
      JplQuikScatUserQuery reqPart1 = new JplQuikScatUserQuery( this.jplQuikScatCalendar );
      // Check that date range is set before setting new request.
      if ( request.isDateRangeSet() )
      {
        reqPart1.setDateRange( request.getDateRangeMin(), request.getDateRangeMax());
      }
      // @todo Check that longitude range is set.
      if ( request.isLongitudeRangeSet() )
      {
        reqPart1.setLongitudeRange( request.getLongitudeRangeMin(), request.getLongitudeRangeMax() );
      }
      //log.debug( " buildCatalogFromRequest(): ce is " );// + ce1);
      dsIt1 = this.jplQsDfs.findMatchingCatalogEntries( reqPart1);
      while ( dsIt1.hasNext())
      {
        curEntry = (JplQuikScatEntry) dsIt1.next();
        this.addToResultsList( curEntry, this.comparator );
      }
    }
    log.debug( "buildCatalogFromRequest(): list has " + this.resultSortedList.size() + " entries.");

    return( this.createCatalog( this.resultSortedList ) );
  }

  /**
   * Return true if given request is valid (i.e., the request values are within the
   * allowed ranges), otherwise, return false.
   *
   * @param request - The user request.
   * @return Return true if the given request is valid, otherwise, return false.
   * @throws NullPointerException if the given request is a null pointer.
   */
  protected boolean validateRequest( JplQuikScatUserQuery request)
  {
    log.debug( "validateRequest(): validate the user request - " +
                  "requested date range minimum <" + request.getDateRangeMin() + ">, " +
                  "requested date range maximum <" + request.getDateRangeMax() + ">, " +
                  "requested longitude range minimum <" + request.getLongitudeRangeMin() + ">, " +
                  "requested longitude range maximum <" + request.getLongitudeRangeMax() + ">.");

    // Validate the requested date range.
    if ( request.isDateRangeSet() )
    {
      // Validate that requested date range is within allowed date range.
      if ( request.getDateRangeMin() < this.allowedDateRange.getAllowedRangeMin() )
      {
        String tmp = "validateRequest(): requested date range minimum <" + request.getDateRangeMin() +
                "> falls before the allowed date range minimum <" + this.allowedDateRange.getAllowedRangeMin() + ">.";
        log.debug( tmp);
        return( false);
      }
      if ( request.getDateRangeMin() > this.allowedDateRange.getAllowedRangeMax())
      {
        String tmp = "validateRequest(): requested date range minimum <" + request.getDateRangeMin() +
                "> falls after the allowed date range maximum <" +
                this.allowedDateRange.getAllowedRangeMax() + ">.";
        log.debug( tmp);
        return( false);
      }
      if ( request.getDateRangeMax() < this.allowedDateRange.getAllowedRangeMin() )
      {
        String tmp = "validateRequest(): requested date range maximum <" + request.getDateRangeMax() +
                "> falls before the allowed date range minimum <" + this.allowedDateRange.getAllowedRangeMin() + ">.";
        log.debug( tmp);
        return( false);
      }
      if ( request.getDateRangeMax() > this.allowedDateRange.getAllowedRangeMax() )
      {
        String tmp = "validateRequest(): requested date range maximum <" + request.getDateRangeMax() +
                "> falls after the allowed date range maximum <" + this.allowedDateRange.getAllowedRangeMax() + ">.";
        log.debug( tmp);
        return( false);
      }

      // Validate that requested allowedRangeMin date is before requested allowedRangeMax date.
      if ( ! ( request.getDateRangeMin() < request.getDateRangeMax() ) )
      {
        String tmp = "validateRequest(): requested date range minimum <" + request.getDateRangeMin() +
                "> is greater than the requested date range maximum <" + request.getDateRangeMax() + ">.";
        log.debug( tmp);
        return( false);
      }
    }

    // Deal with allowed longitude range.
    if ( request.isLongitudeRangeSet() )
    {
      // Validate that requested longitude range is within allowed longitude range.
      if ( request.getLongitudeRangeMin() < this.allowedLongitudeRange.getAllowedRangeMin() )
      {
        String tmp = "validateRequest(): the requested longitude range minimum <" + request.getLongitudeRangeMin() +
                "> is less than the allowed longitude range minimum <" +
                this.allowedLongitudeRange.getAllowedRangeMin() + ">.";
        log.debug( tmp);
        return( false);
      }
      if ( request.getLongitudeRangeMin() > this.allowedLongitudeRange.getAllowedRangeMax() )
      {
        String tmp = "validateRequest(): the requested longitude range minimum <" +
                request.getLongitudeRangeMin() + "> is greater than the allowed longitude range maximum <" +
                this.allowedLongitudeRange.getAllowedRangeMax() + ">.";
        log.debug( tmp);
        return( false);
      }
      if ( request.getLongitudeRangeMax() < this.allowedLongitudeRange.getAllowedRangeMin() )
      {
        String tmp = "validateRequest(): the requested longitude range maximum <" + request.getLongitudeRangeMax() +
                "> is less than the allowed longitude range minimum<" + this.allowedLongitudeRange.getAllowedRangeMin() + ">.";
        log.debug( tmp);
        return( false);
      }
      if ( request.getLongitudeRangeMax() > this.allowedLongitudeRange.getAllowedRangeMax() )
      {
        String tmp = "validateRequest(): the requested longitude range maximum <" + request.getLongitudeRangeMax() +
                "> is greater than the allowed longitude range maximum <" + this.allowedLongitudeRange.getAllowedRangeMax() + ">.";
        log.debug( tmp);
        return( false);
      }
    }
    return( true);
  }

  /** Add the given entry to the results list. */
  protected void addToResultsList( JplQuikScatEntry entry,
                                   Comparator comparator )
  {
    if ( entry == null )
    {
      return;
    }
    if ( comparator == null )
    {
      this.resultSortedList.add( entry);
      return;
    }

    int index;
    index = Collections.binarySearch( this.resultSortedList, entry, comparator );

    if ( index < 0)
    {
      // No matching entry (in terms of the given comparator) was found, add the
      // given entry to the list at the returned insertion point (-index - 1).
      this.resultSortedList.add( -index - 1, entry);
    }
    else
    {
      // A matching entry (in terms of the given comparator) was found, add the
      // given entry to the list at the same point as the matching entry.
      this.resultSortedList.add( index, entry);
    }

  }

  protected InvCatalog createCatalog( List entries)
  {
    log.debug( "createCatalog(): allowedRangeMin - create resulting catalog.");

    String serviceName = "jplQuikSCAT";
    String serviceTypeName = ServiceType.DODS.toString();
    String serviceBaseURL = "http://dods.jpl.nasa.gov/cgi-bin/nph-dods/pub/ocean_wind/quikscat/L2B/data";

    // Create the catalog, service, and top-level dataset.
    InvCatalogImpl catalog = new InvCatalogImpl( null, null, null );
    InvService myService = new InvService( serviceName, serviceTypeName, serviceBaseURL, null, null );
    InvDatasetImpl topDs = new InvDatasetImpl( null, this.jplQsDfs.getCatalogTitle() );

    // Add service and top-level dataset to the catalog.
    catalog.addService( myService );
    catalog.addDataset( topDs );

    JplQuikScatEntry curEntry = null;
    InvDatasetImpl curDs = null;
    String curDsName = null;
    String curDsDodsUrl = null;

    Date curDsDate = null;
    String curDsDateString = null;
    float curDsLongitude;
    int curDsRevNum;
    int curDsWvcRows;

    InvProperty property = null;

    for( int i = 0; i < entries.size(); i++ )
    {
      curEntry = (JplQuikScatEntry) entries.get( i);

      curDsDodsUrl = curEntry.getDodsUrl();
      curDsDodsUrl = curDsDodsUrl.substring( curDsDodsUrl.indexOf( serviceBaseURL));
      curDsDate = curEntry.getDate();
      curDsDateString = this.jplQuikScatCalendar.getIsoDateTimeStringFromDate( curDsDate );
      curDsLongitude = curEntry.getLongitude();
      curDsRevNum = curEntry.getRevNum();
      curDsWvcRows = curEntry.getWvcRows();

      curDsName = "QuikSCAT Level-2B - " + curDsDateString + " - longitude " + curDsLongitude;
      curDs = new InvDatasetImpl( topDs, curDsName, null, serviceName, curDsDodsUrl);

      property = new InvProperty( "Date", curDsDateString);
      curDs.addProperty( property );
      property = new InvProperty( "Longitude", Float.toString( curDsLongitude ) );
      curDs.addProperty( property );
      property = new InvProperty( "Rev Number", Integer.toString( curDsRevNum ) );
      curDs.addProperty( property );
      property = new InvProperty( "WVC Rows", Integer.toString( curDsWvcRows ) );
      curDs.addProperty( property );
    }

    return( catalog );
  }

}