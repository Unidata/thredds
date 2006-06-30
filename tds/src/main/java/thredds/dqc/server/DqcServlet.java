// $Id: DqcServlet.java,v 1.10 2005/10/11 19:44:29 caron Exp $

package thredds.dqc.server;

import thredds.servlet.ServletUtil;
import thredds.servlet.AbstractServlet;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Iterator;


/**
 * Servlet for handling DQC requests.
 * To implement a DQC request handler specific to your data and DQC document,
 * you need to write your own DQC request handler by extending the abstract
 * class <tt>DqcHandler</tt>.
 *
 * @see thredds.dqc.server.DqcHandler
 */

public class DqcServlet extends AbstractServlet
{

  private File dqcRootPath;
  private File dqcContentPath, dqcConfigPath;
  private String configFileName;

  private DqcServletConfig mainConfig = null;
  private String servletName = "dqcServlet";
  private String dqcDocDirName = "doc";
  private String dqcConfigDirName = "config";

  private String dqcCatalog = "catalog.xml";


  /* Requests that will be handled (for the URL, prepend path with
  * http://<myserver>/thredds/dqc):
  *
  *  GET, PUT,
  *   OR POST       Path          Location
  *  ---------      ----          --------
  *    GET        /doc/README    rootPath/dqcServlet/README
  *    GET        /doc/NEWS      rootPath/dqcServlet/NEWS
  *    GET        /doc/*.html    rootPath/dqcServlet/docs/*.html
  *    GET        /doc/*         rootPath/dqcServlet/docs/*
  *    GET        ""             redirect to "/"
  *    GET        /              Create HTML doc on the fly that points to /catalog.xml and
  *                              has other information, e.g., links to documentation and THREDDS
  *                              servlet top level information.
  *    GET        /catalog.xml   Catalog that reflects datasets available here.
  *    GET        /<dqcHandlerName>.xml
  *                              DQC document for the DqcHandler named.
  *    GET        /<dqcHandlerName>*
  *                              Contents specific to each DqcHandler.
  */

  protected String getPath() { return ( servletName + "/" ); }

  protected void makeDebugActions() { }

  /** Initialize the servlet. */
  public void init()
    throws javax.servlet.ServletException
  {
    super.init();

    // Get various paths and file names.
    this.dqcRootPath = new File( this.rootPath,  this.servletName);

    this.dqcContentPath = new File( this.contentPath );
    this.dqcConfigPath = new File( this.dqcContentPath, this.dqcConfigDirName );

    this.configFileName = this.getInitParameter( "configFile");

    // Some debug info.
    log.debug( "init(): dqc root path    = " + this.dqcRootPath.toString() );
    log.debug( "init(): dqc content path = " + this.dqcContentPath.toString() );
    log.debug( "init(): dqc config path  = " + this.dqcConfigPath.toString() );
    log.debug( "init(): config file      = " + this.configFileName );

    try
    {
      this.mainConfig = this.readInConfigDoc();
    }
    catch ( java.io.IOException e )
    {
      String tmpMsg = "IOException thrown while reading DqcServlet config: " + e.getMessage();
      log.error( "init():" + tmpMsg, e );
      throw new javax.servlet.ServletException( tmpMsg, e );
    }

    log.debug( "init() done" );
  }

  /**
   *
   * @return
   * @throws IOException
   * @throws SecurityException if config document cannot be read.
   */
  private DqcServletConfig readInConfigDoc() throws IOException
  {
    DqcServletConfig retValue = null;

    // Instantiate the configuration for this servlet.
    retValue = new DqcServletConfig( this.dqcConfigPath, this.configFileName );

    return( retValue );
  }

  /**
   * Handle all GET requests. This includes requests for: documentation files,
   * a top-level catalog listing each DQC described dataset available through
   * this servlet installation, the DQC documents for those datasets, and
   * the queries for each of those DQC documents.
   *
   * @param req - the HttpServletRequest
   * @param res - the HttpServletResponse
   * @throws ServletException if the request could not be handled for some reason.
   * @throws IOException if an I/O error is detected (when communicating with client not for servlet internal IO problems?).
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    ServletUtil.logServerAccessSetup( req );

    String tmpMsg = null;
    PrintWriter out = null;

    String handlerName;
    DqcServletConfigItem reqHandlerInfo = null;
    DqcHandler reqHandler = null;

    // Get the request path information.
    String reqPath = req.getPathInfo();

    // Redirect empty path request to the root request (i.e., add a "/" to end of URL).
    if ( reqPath == null )
    {
      res.sendRedirect( res.encodeRedirectURL( req.getContextPath() + req.getServletPath() + "/" ) );
      ServletUtil.logServerAccess( HttpServletResponse.SC_MOVED_PERMANENTLY, 0 );
      return;
    }
    // Handle root request: create HTML page that lists each available DQC described dataset.
    else if ( reqPath.equals( "/" ) )
    {
      out = res.getWriter();
      res.setContentType( "text/html" );
      String resString = this.htmlOfConfig( req.getContextPath() + req.getServletPath() );
      out.print( resString);
      res.setStatus( HttpServletResponse.SC_OK );
      ServletUtil.logServerAccess( HttpServletResponse.SC_OK, resString.length() );
      return;
    }
    // Handle requests for documentation files.
    else if ( reqPath.startsWith( "/" + this.dqcDocDirName + "/" ) )
    {
      // @todo Would like this to handle root/doc/* or content/doc/* files. For instance:
      //    String altPaths[] = { this.dqcRootPath.getAbsolutePath(),
      //                          this.dqcContentPath.getAbsolutePath() };
      //    ServletUtil.returnFile( this, altPaths, reqPath, res, null);
      ServletUtil.returnFile( this, this.dqcRootPath.getAbsolutePath(), reqPath, req, res, null);
    }
    // Handle requests for config files.
    else if ( reqPath.startsWith( "/" + this.dqcConfigDirName + "/" ) )
    {
      ServletUtil.returnFile( this, this.dqcContentPath.getAbsolutePath(), reqPath, req, res, null );
    }
    // Handle requests for a catalog representation of the datasets DQC-ified by this servlet.
    else if (reqPath.equals( "/" + this.dqcCatalog ) )
    {
      // Get the catalog as a string.
      InvCatalogFactory catFactory = new InvCatalogFactory( "default", true );
      String catalogAsString = catFactory.writeXML_1_0( (InvCatalogImpl) this.mainConfig.createCatalogRepresentation( req.getContextPath() + req.getServletPath() ) );

      // Write the catalog out.
      out = res.getWriter();
      res.setContentType( "text/xml");
      out.print( catalogAsString );
      res.setStatus( HttpServletResponse.SC_OK );
      ServletUtil.logServerAccess( HttpServletResponse.SC_OK, catalogAsString.length() );
      return;
    }
    else
    {
      // Determine which handler to use for this request.
      reqPath = reqPath.substring( 1 ); // Remove leading slash ('/').

      // Check whether full path is the handler name
      handlerName = reqPath;
      log.debug( "doGet(): Attempt to find \"" + handlerName + "\" handler (1).");
      reqHandlerInfo = this.mainConfig.findItem( handlerName );

      // Check if DQC document is being requested, i.e., <handler name>.xml
      if ( reqHandlerInfo == null && reqPath.endsWith( ".xml" ) )
      {
        handlerName = reqPath.substring( 0, reqPath.length() - 4);
        log.debug( "doGet(): Attempt to find \"" + handlerName + "\" handler (2)." );
        reqHandlerInfo = this.mainConfig.findItem( handlerName );
      }
      // Check if handler name is first part of path before slash ('/').
      if ( reqHandlerInfo == null && reqPath.indexOf( '/') != -1 )
      {
        handlerName = reqPath.substring( 0, reqPath.indexOf( '/' ) );
        log.debug( "doGet(): Attempt to find \"" + handlerName + "\" handler. (3)" );
        reqHandlerInfo = this.mainConfig.findItem( handlerName );
      }
      if ( reqHandlerInfo == null )
      {
        tmpMsg = "No DQC Handler available for path <" + reqPath + ">.";
        log.warn( "doGet(): " + tmpMsg );
        res.sendError( HttpServletResponse.SC_BAD_REQUEST, tmpMsg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
        return;
        // @todo Loop through all config items checking if path starts with name.
      }

      // Try to create the requested DqcHandler.
      log.debug( "doGet(): creating handler for " + reqHandlerInfo.getHandlerClassName() );

      try
      {
        reqHandler = DqcHandler.factory( reqHandlerInfo, this.dqcConfigPath.getAbsolutePath() );
      }
      catch ( DqcHandlerInstantiationException e )
      {
        tmpMsg = "Handler could not be constructed for " + reqHandlerInfo.getHandlerClassName() + ": " + e.getMessage();
        log.error( "doGet(): " + tmpMsg, e );
        res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 );
        return;
      }
      
      // Was the requested DqcHandler created?
      if ( reqHandler != null )
      {
        // Hand the request to the just created DqcHandler.
        log.debug( "doGet(): handing query to handler" );
        reqHandler.handleRequest( req, res );

        return;
      }
      else
      {
        // No handler available, throw ServletException.
        tmpMsg = "No handler for " + reqHandlerInfo.getHandlerClassName();
        log.error( "doGet(): " + tmpMsg );
        res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 );
        return;
      }
    }
  }

  public void doPut( HttpServletRequest req, HttpServletResponse res )
          throws IOException, ServletException
  {
    ServletUtil.logServerAccessSetup( req );

    File tmpFile = null;
    String tmpMsg = null;

    String reqPath = req.getPathInfo();

    // Null request cannot be PUT.
    if ( reqPath == null )
    {
      tmpMsg = "PUT to empty path (\"\") not allowed.";
      log.debug( "doPut(): " + tmpMsg );
      res.setHeader( "Allow", "GET");
      res.sendError( HttpServletResponse.SC_METHOD_NOT_ALLOWED, tmpMsg );
      ServletUtil.logServerAccess( HttpServletResponse.SC_METHOD_NOT_ALLOWED, 0 );
      return;
    }

    // Requests to PUT outside the config/ directory are not allowed.
    if ( ! reqPath.startsWith( "/" + this.dqcConfigDirName + "/" ) )
    {
      tmpMsg = "Cannot PUT a document outside the " + this.dqcConfigDirName + "/ directory";
      log.debug( "doPut(): " + tmpMsg );
      res.sendError( HttpServletResponse.SC_FORBIDDEN, tmpMsg );
      ServletUtil.logServerAccess( HttpServletResponse.SC_FORBIDDEN, 0 );
      return;
    }

    tmpFile = new File( this.dqcContentPath, reqPath );

    log.debug( "doPut(): putting DqcServlet Config file - " + reqPath );

    // Handle PUT of main config file.
    if ( reqPath.equals( "/" + this.dqcConfigDirName + "/" + this.configFileName ) )
    {
      // Save the PUT document to the config file location.
      // @todo Make sure new config file is valid before writing over old config file.
      // @todo OR roll back to previous config file.
      if ( ServletUtil.saveFile( this, this.dqcContentPath.getAbsolutePath(),
                                 reqPath, req, res ) )
      {
        log.debug( "doPut(): file saved <" + reqPath + ">." );

        // Create a new servlet config with the newly PUT config file.
        try
        {
          this.mainConfig = this.readInConfigDoc();
        }
        catch ( IOException e )
        {
          tmpMsg = "IOException thrown while reading newly PUT DqcServlet config file: " + e.getMessage();
          log.error( "initConfig():" + tmpMsg, e );
          res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
          ServletUtil.logServerAccess( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 );
          return;
        }
        res.setStatus( HttpServletResponse.SC_OK );
        ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1 );
        return;
      }
      else
      {
        tmpMsg = "File not saved <" + reqPath + ">";
        log.error( "doPut(): " + tmpMsg );
        res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 );
        return;
      }
    }
    // Handle PUT of all other config files (DqcHandler config files).
    else
    {
      boolean creatingNewFile = true;
      if ( tmpFile.exists() )
      {
        creatingNewFile = false;
      }
      if ( ServletUtil.saveFile( this, this.dqcContentPath.getAbsolutePath(),
                                 reqPath, req, res ) )
      {
        log.debug( "doPut(): file saved <" + reqPath + ">." );

        if ( creatingNewFile )
        {
          res.setStatus( HttpServletResponse.SC_CREATED );
          ServletUtil.logServerAccess( HttpServletResponse.SC_CREATED, 0 );
        }
        else
        {
          res.setStatus( HttpServletResponse.SC_OK );
          ServletUtil.logServerAccess( HttpServletResponse.SC_OK, 0 );
        }
        return;
      }
      else
      {
        tmpMsg = "File not saved <" + reqPath + ">";
        log.error( "doPut(): " + tmpMsg );
        res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
        ServletUtil.logServerAccess( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 );
        return;
      }
    }
  }

  /**
   * Build an HTML page that lists the DQC documents handled by this servlet.
   *
   * @param contextServletPath - the Context servlet path
   * @return An HTML page that lists the DQC documents handled by this servlet.
   */
  private String htmlOfConfig( String contextServletPath)
  {
    // @todo Add links to other things, e.g., docs and THREDDS server top-level
    StringBuffer buf = new StringBuffer();

    buf.append( "<html>" + "\n");
    buf.append( "<head><title>DQC Servlet - Available Datasets</title></head>" + "\n");
    buf.append( "<body>" + "\n");
    buf.append( "<h1>DQC Servlet - Available Datasets</h1>" + "\n");

    buf.append( "<table border=\"1\">" + "\n");
    buf.append( "<tr>" + "\n");
    buf.append( "<th> Name</th>" + "\n");
    buf.append( "<th> Description</th>" + "\n");
    buf.append( "<th> DQC Document</th>" + "\n");
    buf.append( "</tr>" + "\n");

    Iterator iter = null;
    DqcServletConfigItem curItem = null;
    iter = this.mainConfig.getIterator();
    while( iter.hasNext())
    {
      curItem = (DqcServletConfigItem) iter.next();

      buf.append( "<tr>\n");
      buf.append( "<td>" + curItem.getName() + "</td>\n");
      buf.append( "<td>" + curItem.getDescription() + "</td>\n");
      buf.append( "<td><a href=\"" + contextServletPath + "/" + curItem.getName() + ".xml\">"
                  + curItem.getName() + "</a></td>\n");
      buf.append( "<tr>\n");
    }

    buf.append( "<table>\n");

    buf.append( "<p>\n");
    buf.append( "This listing is also available as a <a href=\"catalog.xml\">THREDDS catalog</a>.\n");
    buf.append( "</p>\n");

    buf.append( "</body>\n");
    buf.append( "</html>\n");

    return( buf.toString());
  }

}
/*
 * $Log: DqcServlet.java,v $
 * Revision 1.10  2005/10/11 19:44:29  caron
 * release 3.3
 *
 * Revision 1.9  2005/08/22 19:39:12  edavis
 * Changes to switch /thredds/dqcServlet URLs to /thredds/dqc.
 * Expand testing for server installations: TestServerSiteFirstInstall
 * and TestServerSite. Fix problem with compound services breaking
 * the filtering of datasets.
 *
 * Revision 1.8  2005/07/18 23:32:39  caron
 * static file serving
 *
 * Revision 1.7  2005/07/13 22:48:07  edavis
 * Improve server logging, includes adding a final log message
 * containing the response time for each request.
 *
 * Revision 1.6  2005/07/13 16:14:00  caron
 * cleanup logging
 * add static param to ServletUtil.returnFile()
 *
 * Revision 1.5  2005/04/12 20:52:36  edavis
 * Setup to handle logging of the response status for each
 * servlet request handled (logging similar to Apache web
 * server access_log).
 *
 * Revision 1.4  2005/04/06 23:21:43  edavis
 * Update CatGenServlet and DqcServlet to inherit from AbstractServlet.
 *
 * Revision 1.3  2005/04/05 22:37:03  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.2  2004/08/23 16:45:20  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 * Revision 1.1  2004/05/11 19:33:58  edavis
 * Moved here from thredds.servlet.DqcServlet.java.
 *
 * Revision 1.6  2004/04/03 00:44:58  edavis
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
 * Revision 1.5  2004/03/05 06:35:07  edavis
 * Add more exception handling and error messages.
 *
 * Revision 1.4  2003/12/11 01:37:37  edavis
 * Added logging. Switched to using java.io.File rather than using file name strings.
 * Also changed how config file is handled.
 *
 * Revision 1.3  2003/10/31 22:26:24  edavis
 * Minor change to comment.
 *
 * Revision 1.2  2003/05/06 22:12:46  edavis
 * Add response for empty path requests and for dqc.xml requests.
 *
 * Revision 1.1  2003/04/28 17:57:13  edavis
 * Initial checkin of THREDDS DqcServlet.
 *
 */
