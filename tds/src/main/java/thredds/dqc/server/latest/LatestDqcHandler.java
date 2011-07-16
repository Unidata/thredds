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
package thredds.dqc.server.latest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.net.URL;

import ucar.unidata.util.EscapeStrings;
import thredds.catalog.*;
import thredds.catalog.parser.jdom.InvCatalogFactory10;
import thredds.catalog.query.*;
import thredds.servlet.UsageLog;
import thredds.dqc.server.DqcHandler;
import ucar.nc2.constants.FeatureType;

/**
 * _more_
 *
 * @author edavis
 * @since Sep 21, 2005 9:12:56 PM
 */
public class LatestDqcHandler extends DqcHandler {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( LatestDqcHandler.class );

  protected LatestConfig config;

  /**
   * Default constructor.
   */
  public LatestDqcHandler()
  {
  }

  /**
   * @param req
   * @param res
   * @throws java.io.IOException            if an input or output error is detected while the servlet handles the GET request.
   * @throws javax.servlet.ServletException if the request can't be handled for some reason.
   */
  public void handleRequest( HttpServletRequest req, HttpServletResponse res )
          throws IOException, ServletException
  {
    // Get request path.
    String reqPath = req.getPathInfo();
    // Get path after this handler's name.
    String extraPath = reqPath.substring( this.getHandlerInfo().getName().length() + 1 );

    // Deal with request for DQC document.
    if ( extraPath.equals( ".xml" ) )
    {
      // Create the DQC document.
      QueryCapability dqc = createDqcDocument( req.getContextPath() + req.getServletPath() + "/" + this.getHandlerInfo().getName() );

      // Write DQC doc as response
      DqcFactory dqcFactory = new DqcFactory( false );
      String dqcAsString = dqcFactory.writeXML( dqc );
      PrintWriter out = res.getWriter();
      res.setContentType( "text/xml" );
      res.setStatus( HttpServletResponse.SC_OK );
      out.print( dqcAsString );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, dqcAsString.length() ));
      return;
    }

    // If not a request for DQC document, should not be any extra path info.
    if ( extraPath.length() > 0 )
    {
      String tmpMsg = "Extra path information <" + extraPath + "> not understood.";
      //log.info( "handleRequest(): " + tmpMsg );
      res.sendError( HttpServletResponse.SC_BAD_REQUEST, tmpMsg );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, 0 ));
      return;
    }

    // Determine what LatestConfig.Item was requested.
    String reqItemId = EscapeStrings.unescapeOGC(req.getQueryString());
    if ( reqItemId == null )
    {
      // No LatestConfig.Item ID was given in request.
      String tmpMsg = "No latest request ID given.";
      //log.info( "handleRequest(): " + tmpMsg );
      res.sendError( HttpServletResponse.SC_NOT_FOUND,
                     "LatestDqcHandler.handleRequest(): " + tmpMsg );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0 ));
      return;
    }
    log.debug( "Request for the latest \"" + reqItemId + "\"." );

    // Determine if the requested Item is supported.
    LatestConfig.Item reqItem = this.config.getItem( reqItemId );
    if ( reqItem == null )
    {
      String tmpMsg = "The Item requested, " + reqItemId + ", is not supported.";
      //log.error( "handleRequest(): " + tmpMsg );
      res.sendError( HttpServletResponse.SC_NOT_FOUND,
                     "LatestDqcHandler.handleRequest(): " + tmpMsg );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0 ));
      return;
    }

    // Check that directory exists
    String reqItemName = reqItem.getName();
    File dir = new File( reqItem.getDirLocation() );
    log.debug( "handleRequest(): requested Item <id=" + reqItemId +
               ", name=" + reqItemName + ", dir=" + dir.getPath() + "> is supported." );
    if ( !dir.isDirectory() )
    {
      // The dataset location is not a directory.
      log.error( "handleRequest(): Directory <" + dir.getPath() +
                 "> given by configuration item <id=" + reqItemId +
                 "> is not a directory." );
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                     "Directory given by config item <id=" + reqItemId + "> is not a directory." );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ));
      return;
    }

    // Find the latest dataset.
    File mostRecentDs = findLatestDataset( reqItem, dir.listFiles() );

    if ( mostRecentDs != null )
    {
      // Create catalog for the latest dataset.
      String catalogAsString = createCatalog( mostRecentDs, reqItem );

      if ( catalogAsString == null )
      {
        String tmpMsg = "Could not generate a catalog, unsupported InvCat spec version <"
                        + reqItem.getInvCatSpecVersion() + ">.";
        log.error( "handleRequest(): " + tmpMsg );
        res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                       tmpMsg );
        log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ));
        return;
      }

      // Send catalog as response.
      PrintWriter out = res.getWriter();
      res.setContentType( "text/xml" );
      res.setStatus( HttpServletResponse.SC_OK );
      out.print( catalogAsString );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, catalogAsString.length() ));
      return;
    }
    else
    {
      // The requested Item is not available. Return an error.
      String tmpMsg = "No latest dataset found for request<" + reqItemId + ">.";
      //log.error( "handleRequest(): " + tmpMsg );
      res.sendError( HttpServletResponse.SC_NOT_FOUND, tmpMsg );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0 ));
      return;
    }
  }

  /**
   * Read the configuration from the config doc.
   */
  public void initWithHandlerConfigDoc( URL configDocURL )
          throws IOException
  {
    // Open the preferences file.
    log.debug( "initWithHandlerConfigDoc(): open config document <" + configDocURL.toString() + "> and read config information." );
    InputStream is = configDocURL.openStream();
    config = LatestConfigFactory.parseXML( is, configDocURL.toString());
    if ( config == null )
    {
      String tmpMsg = "Failed to parse config doc <" + configDocURL.toString() + ">.";
      log.error( "initWithHandlerConfigDoc(): " + tmpMsg );
      throw new IOException( tmpMsg );
    }

    // Check for validity of each config item.
    List badDirItemIDs = new ArrayList();
    for ( Iterator it = config.getIds().iterator(); it.hasNext(); )
    {
      LatestConfig.Item curItem = config.getItem( (String) it.next() );
      File dir = new File( curItem.getDirLocation() );
      if ( ! dir.isDirectory() )
      {
        log.warn( "initWithHandlerConfigDoc(): Directory <" + dir.getAbsolutePath() +
                  "> is not a directory; config item<id=" + curItem.getId() +
                  "> being removed." );
        badDirItemIDs.add( curItem.getId());
      }
    }

    // Remove any invalid items.
    for ( Iterator it = badDirItemIDs.iterator(); it.hasNext(); )
    {
      String curItemID = (String) it.next();
      if ( ! config.removeItem( curItemID ) )
      {
        log.warn( "initWithHandlerConfigDoc(): Bad Item <id=" + curItemID + "> not removed.");
      }
    }

    if ( config.isEmpty() )
    {
      String tmpMsg = "No configuration info.";
      log.error( "initWithHandlerConfigDoc(): " + tmpMsg );
      throw new IOException( tmpMsg);
    }

  }

  protected File findLatestDataset( LatestConfig.Item reqItem, File allFiles[] )
  {
    File mostRecentDs = null;
    Date mostRecentDsDate = null;

    File curDs = null;
    Date curDsDate = null;

    log.debug( "findLatestDataset(): getting dir listing." );
    for ( int i = 0; i < allFiles.length; i++ )
    {
      // Get the date for the current dataset.
      curDs = allFiles[i];
      curDsDate = this.getDatasetDate( curDs, reqItem );
      log.debug( "findLatestDataset(): current dataset is <" + curDs.getName() + ">." );

      // If this is the first dataset, make it the most recent one.
      if ( curDsDate != null )
      {
        if ( mostRecentDs == null )
        {
          mostRecentDs = curDs;
          mostRecentDsDate = curDsDate;
        }
        else
        {
          // Check if current dataset is most recent.
          if ( mostRecentDsDate.before( curDsDate ) )
          {
            mostRecentDs = curDs;
            mostRecentDsDate = curDsDate;
          }
        }
        log.debug( "findLatestDataset(): the most recent dataset <"
                   + mostRecentDs.getName() + "> has date <" + mostRecentDsDate + ">." );
      }
    }

    return ( mostRecentDs );
  }

  protected QueryCapability createDqcDocument( String baseURI )
  {
    // Create DQC root elements (QueryCapability and Query).
    QueryCapability dqc = new QueryCapability( null, this.getHandlerInfo().getName() + " DQC Document", "0.3" );
    Query query = new Query( baseURI, null, null );
    dqc.setQuery( query );

    // Create the SelectService and add to the DQC.
    SelectService selectService = new SelectService( "service", "Select service type." );
    selectService.addServiceChoice( "OpenDAP", "OPeNDAP/DODS", null, null, null );
    selectService.setRequired( "false" );
    dqc.addUniqueSelector( selectService );
    dqc.setServiceSelector( selectService );

    // Create the SelectList and add to the DQC
    SelectList selectList = new SelectList( "Model name", "models", null, "true", "false" );
    for ( Iterator it = config.getIds().iterator(); it.hasNext(); )
    {
      LatestConfig.Item curConfigItem = config.getItem( (String) it.next() );
      ListChoice curChoice = new ListChoice( selectList, curConfigItem.getId(), curConfigItem.getName(), null );
      selectList.addChoice( curChoice );
    }
    dqc.addUniqueSelector( selectList );

    return ( dqc );
  }

  /**
   * Generate a latest dataset catalog.
   */
  protected InvCatalogImpl createCatalog( String catName, String dsName, FeatureType dsType,
                                          String serviceType, String serviceName,
                                          String serviceBaseURL, String urlPath )
  {
    log.debug( "createCatalog(): creating createCatalog/dataset." );

    // Create the catalog, service, and top-level dataset.
    InvCatalogImpl catalog = new InvCatalogImpl( catName, null, null );
    InvService myService = new InvService( serviceName, serviceType, serviceBaseURL, null, null );
    InvDatasetImpl topDs = new InvDatasetImpl( null, dsName, dsType, serviceName, urlPath );
    // OR ( null, this.mainConfig.getDqcServletTitle() );
    // Add service and top-level dataset to the catalog.
    catalog.addService( myService );
    catalog.addDataset( topDs );

    catalog.finish();

    log.debug( "createCatalog(): createCatalog/dataset created." );
    return ( catalog );
  }

  protected String createCatalog( File mostRecentDs, LatestConfig.Item reqItem )
          throws IOException
  {
    InvCatalogImpl catalog;
    String urlPath = mostRecentDs.getName();

    // Get date of most recent dataset.
    Date mostRecentDsDate = this.getDatasetDate( mostRecentDs, reqItem );

    // Determine name of most recent dataset
    SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH'Z'", Locale.US );
    dateFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
    String mostRecentDsName = "Latest " + reqItem.getName() +
                              " (" + dateFormat.format( mostRecentDsDate ) + ")";

    log.debug( "handleRequest(): Latest dataset <" + mostRecentDs.getAbsolutePath() +
               "> named \"" + mostRecentDsName + "\"." );

    // Create catalog for the latest dataset
    String catalogName = null;
    if ( reqItem.getInvCatSpecVersion().equals( "0.6" ) )
    {
      catalogName = mostRecentDsName;
    }
    catalog = this.createCatalog( catalogName, mostRecentDsName, FeatureType.GRID, "DODS",
                                  "mlode", reqItem.getServiceBaseURL(), urlPath );

    // Write catalog as response
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( true );
    String catalogAsString = null;
    if ( reqItem.getInvCatSpecVersion().equals( "0.6" ) )
    {
      log.warn( "createCatalog(): LatestDqcHandler configured to generate \"0.6\" InvCatalogs. Version \"0.6\" no longer supported." );
      //catalogAsString = fac.writeXML_0_6( catalog );
    }
    else if ( reqItem.getInvCatSpecVersion().startsWith( "1.0" ) )
    {
      InvCatalogFactory10 fac10 = (InvCatalogFactory10) fac.getCatalogConverter( XMLEntityResolver.CATALOG_NAMESPACE_10 );
      fac10.setVersion( reqItem.getInvCatSpecVersion() );
      ByteArrayOutputStream osCat = new ByteArrayOutputStream( 10000 );
      fac10.writeXML( catalog, osCat );
      catalogAsString = osCat.toString();
    }
    return catalogAsString;
  }

  /**
   * Return the date of run time for the current dataset file.
   * If the file does not match the match pattern, null is returned.
   *
   * @param theFile   - the File representing the current dataset file.
   * @param reqItem - the LatestConfig.Item for the request.
   * @return Date of the current dataset files run time
   */
  protected Date getDatasetDate( File theFile, LatestConfig.Item reqItem )
  {
    java.util.regex.Pattern pattern
            = java.util.regex.Pattern.compile( reqItem.getDatasetNameMatchPattern() );
    java.util.regex.Matcher matcher = pattern.matcher( theFile.getName() );

    // Look for match in filename.
    if ( ! matcher.find() )
    {
      log.warn(  "getDatasetDate(): File name <" + theFile.getName() +
                  "> didn't match pattern <" + reqItem.getDatasetNameMatchPattern() + ">.");
      return null;
    }

    // Build time string from substitution pattern.
    // Note: since appendReplacement() isn't just for capturing groups,
    //       need to remove start of filename from resulting StringBuffer.
    StringBuffer dateString = new StringBuffer();
    matcher.appendReplacement( dateString, reqItem.getDatasetTimeSubstitutionPattern() );
    dateString.delete( 0, matcher.start() );

    Date theDate;
    if ( dateString.length() > 0 )
    {
      String dateFormatString = "yyyy-MM-dd'T'HH:mm:ss";
      String dateFormatAltString = "yyyy/MM/dd HH:mm";
      SimpleDateFormat dateFormat = new SimpleDateFormat( dateFormatString, Locale.US );
      dateFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

      try
      {
        theDate = dateFormat.parse( dateString.toString() );
      }
      catch ( java.text.ParseException e )
      {
        log.debug( "getDatasetDate(): Failed to parse date with format \"" + dateFormatString + "\": " + e.getMessage() );
        dateFormat = new SimpleDateFormat( dateFormatAltString, Locale.US );
        dateFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
        try
        {
          theDate = dateFormat.parse( dateString.toString() );
        }
        catch ( ParseException e1 )
        {
          log.warn( "getDatasetDate(): Failed to parse date with either format \"" + dateFormatString + "\" or \"" + dateFormatAltString + "\": " + e.getMessage() );
          return ( null );
        }
      }

      log.debug( "getDatasetDate(): Got date <" + dateString + " - " + theDate.toString() +
                 "> from file name <" + theFile.getName() + ">." );
      return theDate;
    }

    log.warn( "getDatasetDate(): File name <" + theFile.getName() +
              ">, match pattern <" + reqItem.getDatasetNameMatchPattern() +
              ">, and sub pattern <" + reqItem.getDatasetTimeSubstitutionPattern() +
              "> produced a zero length date string." );
    return ( null );
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer();
    buf.append( "LatestDqcHandler[" );
    buf.append( config.toString() );
    buf.append( "\n]");
    return ( buf.toString() );
  }
}