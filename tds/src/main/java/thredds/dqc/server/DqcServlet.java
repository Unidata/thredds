package thredds.dqc.server;

import thredds.servlet.ServletUtil;
import thredds.servlet.AbstractServlet;
import thredds.servlet.ThreddsConfig;
import thredds.servlet.UsageLog;
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
  private boolean allow;
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

    this.allow = ThreddsConfig.getBoolean( "DqcService.allow", false );
    if ( ! this.allow )
    {
      String msg = "DqcServlet not enabled in threddsConfig.xml.";
      log.info( "init(): " + msg );
      log.info( "init(): " + UsageLog.closingMessageNonRequestContext() );
      return;
    }

    // Get various paths and file names.
    this.dqcRootPath = new File( ServletUtil.getRootPath(),  this.servletName);

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

    log.info( "init(): " + UsageLog.closingMessageNonRequestContext() );
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
    log.info( "doGet(): " + UsageLog.setupRequestContext( req) );

    if ( ! this.allow )
    {
      String msg = "DQC service not supported.";
      log.info( "doGet(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, msg.length() ));
      res.sendError( HttpServletResponse.SC_FORBIDDEN, msg );
      return;
    }

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
      res.setStatus( HttpServletResponse.SC_OK );
      out.print( resString);
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
      res.setStatus( HttpServletResponse.SC_OK );
      out.print( catalogAsString );
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
    log.info( "doPut(): " + UsageLog.setupRequestContext( req ) );

    if ( !this.allow )
    {
      String msg = "DQC service not supported.";
      log.info( "doPut(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, msg.length() ) );
      res.sendError( HttpServletResponse.SC_FORBIDDEN, msg );
      return;
    }

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
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_METHOD_NOT_ALLOWED, tmpMsg.length() ));
      return;
    }

    // Requests to PUT outside the config/ directory are not allowed.
    if ( ! reqPath.startsWith( "/" + this.dqcConfigDirName + "/" ) )
    {
      tmpMsg = "Cannot PUT a document outside the " + this.dqcConfigDirName + "/ directory";
      log.debug( "doPut(): " + tmpMsg );
      res.sendError( HttpServletResponse.SC_FORBIDDEN, tmpMsg );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, tmpMsg.length() ) );
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
          log.debug( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -1) );
          return;
        }
        res.setStatus( HttpServletResponse.SC_OK );
        log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, -1 ));
        return;
      }
      else
      {
        tmpMsg = "File not saved <" + reqPath + ">";
        log.error( "doPut(): " + tmpMsg );
        res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
        log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ));
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
          log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_CREATED, 0 ));
        }
        else
        {
          res.setStatus( HttpServletResponse.SC_OK );
          log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, 0 ));
        }
        return;
      }
      else
      {
        tmpMsg = "File not saved <" + reqPath + ">";
        log.error( "doPut(): " + tmpMsg );
        res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
        log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ));
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

    buf.append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" )
       .append( "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" )
       .append( "<html>\n" );
    buf.append( "<head><title>DQC Servlet - Available Datasets</title></head>\n");
    buf.append( "<body>" + "\n");
    buf.append( "<h1>DQC Servlet - Available Datasets</h1>\n");

    buf.append( "<table border=\"1\">\n");
    buf.append( "<tr>\n");
    buf.append( "<th> Name</th>\n");
    buf.append( "<th> Description</th>\n");
    buf.append( "<th> DQC Document</th>\n");
    buf.append( "</tr>\n");

    Iterator iter = null;
    DqcServletConfigItem curItem = null;
    iter = this.mainConfig.getIterator();
    while( iter.hasNext())
    {
      curItem = (DqcServletConfigItem) iter.next();

      buf.append( "<tr>\n")
         .append( "  <td>" ).append( curItem.getName() ).append( "</td>\n" )
         .append( "  <td>" ).append( curItem.getDescription() ).append( "</td>\n" )
         .append( "  <td><a href=\"" ).append( contextServletPath ).append( "/" ).append( curItem.getName() ).append( ".xml\">" ).append( curItem.getName() ).append( "</a></td>\n" )
         .append( "<tr>\n");
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
