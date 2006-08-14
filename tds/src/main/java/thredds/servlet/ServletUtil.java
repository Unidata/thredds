/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.servlet;

import java.io.*;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.FactoryConfigurationError;

import org.apache.log4j.MDC;
import org.apache.log4j.xml.DOMConfigurator;
import ucar.unidata.util.StringUtil;
import ucar.unidata.io.FileCache;
import thredds.catalog.XMLEntityResolver;

public class ServletUtil {
  static private org.slf4j.Logger log;
  static private org.slf4j.Logger logStats;
  static private boolean isDebugInit = false;
  static private boolean isLogInit = false;

  public static final String CONTENT_TEXT = "text/plain; charset=iso-8859-1";

  public static void initDebugging(HttpServlet servlet) {
    if (isDebugInit) return;
    isDebugInit = true;

    ServletContext webapp = servlet.getServletContext();

    String debugOn = webapp.getInitParameter("DebugOn");
    if (debugOn != null) {
      StringTokenizer toker = new StringTokenizer(debugOn);
      while (toker.hasMoreTokens())
        Debug.set(toker.nextToken(), true);
    }
  }

  /**
   * Initialize logging for the web application context in which the given
   * servlet is running. Two types of logging are supported:
   *
   * 1) Regular logging using the SLF4J API.
   * 2) Access logging which can write Apache common logging format logs,
   *    use the ServletUtil.logServerSetup(String) method.
   *
   * The log directory is determined by the servlet containers content
   * directory. The configuration of logging is controlled by the log4j.xml
   * file. The log4j.xml file needs to setup at least two loggers: "thredds";
   * and "threddsAccessLogger"
   *
   * @param servlet - the servlet.
   */
  public static void initLogging(HttpServlet servlet) {
    // Initialize logging if not already done.
    if (isLogInit)
      return;

    System.out.println("+++ServletUtil.initLogging");
    ServletContext servletContext = servlet.getServletContext();

    // set up the log path
    String logPath = getContentPath(servlet) + "logs";
    File logPathFile = new File(logPath);
    if (!logPathFile.exists()) {
      if (!logPathFile.mkdirs()) {
        throw new RuntimeException("Creation of logfile directory failed."+logPath);
      }
    }

    // read in Log4J config file
    System.setProperty("logdir", logPath); // variable substitution
    try {
      String log4Jconfig = servletContext.getInitParameter("log4j-init-file");
      if (log4Jconfig == null)
        log4Jconfig = getRootPath(servlet) + "WEB-INF/log4j.xml";
      DOMConfigurator.configure(log4Jconfig);
      System.out.println("+++Log4j configured");
    } catch (FactoryConfigurationError t) {
      t.printStackTrace();
    }

    log = org.slf4j.LoggerFactory.getLogger(ServletUtil.class);
    logStats = org.slf4j.LoggerFactory.getLogger( "threddsAccessLogger");

    isLogInit = true;
  }

  /**
   * Gather information from the given HttpServletRequest for inclusion in both
   * regular logging messages and THREDDS access log messages. Call this method
   * at start of each doXXX() method (e.g., doGet(), doPut()) in any servlet
   * you implement.
   *
   * Use the SLF4J API to log a regular logging messages. Use the
   * logServerAccess() method to log a THREDDS access log message.
   *
   * This method gathers the following information:
   * 1) "ID" - an identifier for the current thread;
   * 2) "host" - the remote host (IP address or host name);
   * 3) "userid" - the id of the remote user;
   * 4) "startTime" - the system time in millis when this request is started (i.e., when this method is called); and
   * 5) "request" - The HTTP request, e.g., "GET /index.html HTTP/1.1".
   *
   * The appearance of the regular log messages and the THREDDS access log
   * messages are controlled in the log4j.xml configuration file. For the log
   * messages to look like an Apache server "common" log message, use the
   * following log4j pattern:
   *
   * "%X{host} %X{ident} %X{userid} [%d{dd/MMM/yyyy:HH:mm:ss}] %X{request} %m%n"
   *
   * @param req the current HttpServletRequest.
   */
  public static void logServerAccessSetup( HttpServletRequest req)
  {
    HttpSession session = req.getSession( false);

    // Setup context.
    synchronized( ServletUtil.class)
    {
      MDC.put( "ID", Long.toString( ++logServerAccessId));
    }
    MDC.put( "host", req.getRemoteHost());
    MDC.put( "ident", (session == null) ? "-" : session.getId());
    MDC.put( "userid", req.getRemoteUser() != null ? req.getRemoteUser() : "-");
    MDC.put( "startTime", new Long( System.currentTimeMillis()) );
    String query = req.getQueryString();
    query = (query != null ) ? "?" + query : "";
    StringBuffer request = new StringBuffer();
    request.append( "\"").append( req.getMethod()).append( " ")
            .append( req.getRequestURI() ).append( query)
            .append( " " ).append( req.getProtocol()).append( "\"" );

    MDC.put(  "request", request.toString() );
    log.info( "Remote host: " + req.getRemoteHost() + " - Request: " + request);
  }

  /**
   * Gather current thread information for inclusion in regular logging
   * messages. Call this method only for non-request servlet activities, e.g.,
   * during the init() or destroy().
   *
   * Use the SLF4J API to log a regular logging messages.
   *
   * This method gathers the following information:
   * 1) "ID" - an identifier for the current thread; and
   * 2) "startTime" - the system time in millis when this method is called.
   *
   * The appearance of the regular log messages are controlled in the
   * log4j.xml configuration file.
   *
   * @param msg - the information log message logged when this method finishes. 
   */
  public static void logServerSetup( String msg )
  {
    // Setup context.
    synchronized ( ServletUtil.class )
    {
      MDC.put( "ID", Long.toString( ++logServerAccessId ) );
    }
    MDC.put( "startTime", new Long( System.currentTimeMillis() ) );
    log.info( msg );
  }
  private static volatile long logServerAccessId = 0;

  /**
   * Write log entry to THREDDS access log.
   *
   * @param resCode - the result code for this request.
   * @param resSizeInBytes - the number of bytes returned in this result, -1 if unknown.
   */
  public static void logServerAccess( int resCode, long resSizeInBytes )
  {
    long endTime = System.currentTimeMillis();
    long startTime = ( (Long) MDC.get( "startTime" )).longValue();
    long duration = endTime - startTime;

    logStats.info( resCode + " " + ( ( resSizeInBytes != -1 ) ? String.valueOf( resSizeInBytes ) : "-" ) + " " + duration );
    log.info( "Request Completed - " + resCode + " - " + resSizeInBytes + " - " + duration);
//    String path = req.getPathInfo();
//    String reqPath = req.getContextPath() + req.getServletPath() + ( path != null ? path : "" );
//    logStats.info( req.getRemoteHost() + " - - \"" + req.getMethod() + " " + reqPath + " " + req.getProtocol() + "\" "
//                   + resCode + " " + ( ( resSizeInBytes != -1 ) ? String.valueOf( resSizeInBytes ) : "-" ) );
  }

  public static String getRootPath(HttpServlet servlet) {
    ServletContext sc = servlet.getServletContext();
    String rootPath = sc.getRealPath("/");
    rootPath = rootPath.replace('\\','/');
    return rootPath;
  }

  public static String getPath(HttpServlet servlet, String path) {
    ServletContext sc = servlet.getServletContext();
    String spath = sc.getRealPath(path);
    spath = spath.replace('\\','/');
    return spath;
  }

  /* public static String getRootPathUrl(HttpServlet servlet) {
    String rootPath = getRootPath( servlet);
    rootPath = StringUtil.replace(rootPath, ' ', "%20");
    return rootPath;
  } */

  private static String contextPath = null;
  public static String getContextPath( HttpServlet servlet ) {
    if ( contextPath == null ) {
      ServletContext servletContext = servlet.getServletContext();
      String tmpContextPath = servletContext.getInitParameter( "ContextPath" );  // cannot be overridden in the ServletParams file
      if ( tmpContextPath == null ) tmpContextPath = "thredds";
      contextPath = "/"+tmpContextPath;
    }
    return contextPath;
  }

  private static String contentPath = null;
  public static String getContentPath(HttpServlet servlet) {
    if (contentPath == null)
    {
      String tmpContentPath = "../../content" + getContextPath( servlet ) + "/";

      File cf = new File( getRootPath(servlet) + tmpContentPath );
      try{
        contentPath = cf.getCanonicalPath() +"/";
        contentPath = contentPath.replace('\\','/');
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return contentPath;
  }

  public static String getInitialContentPath(HttpServlet servlet) {
    return getRootPath(servlet) + "initialContent/";
  }

  public static String formFilename(String dirPath, String filePath) {
    if ((dirPath == null) || (filePath == null))
      return null;

    if (filePath.startsWith("/"))
      filePath = filePath.substring(1);

    return dirPath.endsWith("/") ? dirPath + filePath : dirPath + "/" + filePath;
  }

  /**
   * Handle a request for a raw/static file (i.e., not a catalog or dataset request).
   *
   * Look in the content (user) directory then the root (distribution) directory
   * for a file that matches the given path and, if found, return it as the
   * content of the HttpServletResponse. If the file is forbidden (i.e., the
   * path contains a "..", "WEB-INF", or "META-INF" directory), send a
   * HttpServletResponse.SC_FORBIDDEN response. If no file matches the request
   * (including an "index.html" file if the path ends in "/"), send an
   * HttpServletResponse.SC_NOT_FOUND..
   *
   * <ol>
   * <li>Make sure the path does not contain ".." directories. </li>
   * <li>Make sure the path does not contain "WEB-INF" or "META-INF". </li>
   * <li>Check for requested file in the content directory
   *   (if the path is a directory, make sure the path ends with "/" and
   *   check for an "index.html" file). </li>
   * <li>Check for requested file in the root directory
   *   (if the path is a directory, make sure the path ends with "/" and
   *   check for an "index.html" file).</li>
   * </ol
   *
   * @param path the requested path
   * @param servlet the servlet handling the request
   * @param req the HttpServletRequest
   * @param res the HttpServletResponse
   * @throws IOException if can't complete request due to IO problems.
   */
  public static void handleRequestForRawFile( String path, HttpServlet servlet, HttpServletRequest req, HttpServletResponse res )
          throws IOException
  {
    // Don't allow ".." directories in path.
    if ( path.indexOf( "/../" ) != -1
         || path.equals( ".." )
         || path.startsWith( "../" )
         || path.endsWith( "/.." ) )
    {
      res.sendError( HttpServletResponse.SC_FORBIDDEN, "Path cannot contain \"..\" directory." );
      ServletUtil.logServerAccess( HttpServletResponse.SC_FORBIDDEN, -1 );
      return;
    }

    // Don't allow access to WEB-INF or META-INF directories.
    String upper = path.toUpperCase();
    if ( upper.indexOf( "WEB-INF" ) != -1
         || upper.indexOf( "META-INF" ) != -1 )
    {
      res.sendError( HttpServletResponse.SC_FORBIDDEN, "Path cannot contain \"WEB-INF\" or \"META-INF\"." );
      ServletUtil.logServerAccess( HttpServletResponse.SC_FORBIDDEN, -1 );
      return;
    }

    // Find a regular file
    File regFile = null;
    // Look in content directory for regular file.
    File cFile = new File( ServletUtil.formFilename( getContentPath( servlet), path ) );
    if ( cFile.exists() )
    {
      if ( cFile.isDirectory() )
      {
        if ( ! path.endsWith( "/"))
        {
          String newPath = req.getRequestURL().append( "/").toString();
          ServletUtil.sendPermanentRedirect( newPath, req, res );
        }
        // If request path is a directory, check for index.html file.
        cFile = new File( cFile, "index.html" );
        if ( cFile.exists() && ! cFile.isDirectory() )
          regFile = cFile;
      }
      // If not a directory, use this file.
      else
        regFile = cFile;
    }

    if ( regFile == null )
    {
      // Look in root directory.
      File rFile = new File( ServletUtil.formFilename( getRootPath( servlet), path ) );
      if ( rFile.exists() )
      {
        if ( rFile.isDirectory() )
        {
          if ( ! path.endsWith( "/" ) )
          {
            String newPath = req.getRequestURL().append( "/").toString();
            ServletUtil.sendPermanentRedirect( newPath, req, res );
          }
          rFile = new File( rFile, "index.html" );
          if ( rFile.exists() && ! rFile.isDirectory() )
            regFile = rFile;
        }
        else
          regFile = rFile;
      }
    }

    if ( regFile == null )
    {
      res.sendError( HttpServletResponse.SC_NOT_FOUND ); // 404
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, -1 );
      return;
    }

    ServletUtil.returnFile( servlet, req, res, regFile, null );
  }

  /**
   * Handle an explicit request for a content directory file (path must start
   * with "/content/".
   *
   * Note: As these requests will show the configuration files for the server,
   * these requests should be covered by security constraints.
   *
   * <ol>
   * <li>Make sure the path does not contain ".." directories. </li>
   * <li>Check for the requested file in the content directory. </li>
   * </ol
   *
   * @param path the requested path (must start with "/content/")
   * @param servlet the servlet handling the request
   * @param req the HttpServletRequest
   * @param res the HttpServletResponse
   * @throws IOException if can't complete request due to IO problems.
   */
  public static void handleRequestForContentFile( String path, HttpServlet servlet, HttpServletRequest req, HttpServletResponse res )
          throws IOException
  {
    handleRequestForContentOrRootFile( "/content/", path, servlet, req, res);
  }

  /**
   * Handle an explicit request for a root directory file (path must start
   * with "/root/".
   *
   * Note: As these requests will show the configuration files for the server,
   * these requests should be covered by security constraints.
   *
   * <ol>
   * <li>Make sure the path does not contain ".." directories. </li>
   * <li>Check for the requested file in the root directory. </li>
   * </ol
   *
   * @param path the requested path (must start with "/root/")
   * @param servlet the servlet handling the request
   * @param req the HttpServletRequest
   * @param res the HttpServletResponse
   * @throws IOException if can't complete request due to IO problems.
   */
  public static void handleRequestForRootFile( String path, HttpServlet servlet, HttpServletRequest req, HttpServletResponse res )
          throws IOException
  {
    handleRequestForContentOrRootFile( "/root/", path, servlet, req, res);
  }

  /**
   * Convenience routine used by handleRequestForContentFile()
   * and handleRequestForRootFile().
   */
  private static void handleRequestForContentOrRootFile( String pathPrefix, String path, HttpServlet servlet, HttpServletRequest req, HttpServletResponse res )
          throws IOException
  {
    if ( ! pathPrefix.equals( "/content/")
         && ! pathPrefix.equals( "/root/"))
    {
      log.error( "handleRequestForContentFile(): The path prefix <" + pathPrefix + "> must be \"/content/\" or \"/root/\"." );
      throw new IllegalArgumentException( "Path prefix must be \"/content/\" or \"/root/\"." );
    }

    if ( ! path.startsWith( pathPrefix ))
    {
      log.error( "handleRequestForContentFile(): path <" + path + "> must start with \"" + pathPrefix + "\".");
      throw new IllegalArgumentException( "Path must start with \"" + pathPrefix + "\".");
    }

    // Don't allow ".." directories in path.
    if ( path.indexOf( "/../" ) != -1
         || path.equals( ".." )
         || path.startsWith( "../" )
         || path.endsWith( "/.." ) )
    {
      res.sendError( HttpServletResponse.SC_FORBIDDEN, "Path cannot contain \"..\" directory." );
      ServletUtil.logServerAccess( HttpServletResponse.SC_FORBIDDEN, -1 );
      return;
    }

    // Find the requested file.
    File file = new File( ServletUtil.formFilename( getContentPath( servlet), path.substring( pathPrefix.length() - 1 ) ) );
    if ( file.exists() )
    {
      // Do not allow request for a directory.
      if ( file.isDirectory() )
      {
        if ( ! path.endsWith( "/" ) )
        {
          String redirectPath = req.getRequestURL().append( "/").toString();
          ServletUtil.sendPermanentRedirect( redirectPath, req, res );
          return;
        }

        HtmlWriter.getInstance().writeDirectory( res, file, path );
        return;
      }

      // Return the requested file.
      ServletUtil.returnFile( servlet, req, res, file, null );
    }
    else
    {
      // Requested file not found.
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, -1 );
      res.sendError( HttpServletResponse.SC_NOT_FOUND ); // 404
    }
  }

  /**
   * Send a permanent redirect (HTTP status 301 "Moved Permanently") response
   * with the given target path.
   *
   * The given target path may be relative or absolute. If it is relative, it
   * will be resolved against the request URL.
   *
   * @param targetPath the path to which the client is redirected.
   * @param req the HttpServletRequest
   * @param res the HttpServletResponse
   * @throws IOException if can't write the response.
   */
  public static void sendPermanentRedirect(String targetPath, HttpServletRequest req, HttpServletResponse res)
          throws IOException
  {
    // Absolute URL needed so resolve the target path against the request URL.
    URI uri = null;
    try
    {
      uri = new URI( req.getRequestURL().toString() );
    }
    catch ( URISyntaxException e )
    {
      log.error( "sendPermanentRedirect(): Bad syntax on request URL <" + req.getRequestURL() + ">.", e);
      ServletUtil.logServerAccess( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 );
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
      return;
    }
    String absolutePath = uri.resolve( targetPath ).toString();
    absolutePath = res.encodeRedirectURL( absolutePath );

    res.setStatus( HttpServletResponse.SC_MOVED_PERMANENTLY );
    res.addHeader( "Location", absolutePath );

    String title = "Permanently Moved - 301";
    String body = new StringBuffer()
            .append( "<p>" )
            .append( "The requested URL <" ).append( req.getRequestURL() )
            .append( "> has been permanently moved (HTTP status code 301)." )
            .append( " Instead, please use the following URL: <a href=\"" ).append( absolutePath ).append( "\">" ).append( absolutePath ).append( "</a>." )
            .append( "</p>" )
            .toString();
    String htmlResp = new StringBuffer()
            .append( "<html><head><title>" )
            .append( title )
            .append( "</title></head><body>" )
            .append( "<h1>" ).append( title ).append( "</h1>" )
            .append( body )
            .append( "</body></html>" )
            .toString();

    ServletUtil.logServerAccess( HttpServletResponse.SC_MOVED_PERMANENTLY, htmlResp.length() );

    // Write the catalog out.
    PrintWriter out = res.getWriter();
    res.setContentType( "text/html" );
    out.print( htmlResp );
    out.flush();
  }

  /**
   * Write a file to the response stream.
   *
   * @param servlet called from here
   * @param contentPath file root path
   * @param path file path reletive to the root
   * @param res the response
   * @param contentType content type, or null
   *
   * @throws IOException
   */
  public static void returnFile( HttpServlet servlet, String contentPath, String path,
                                 HttpServletRequest req, HttpServletResponse res, String contentType ) throws IOException {

    String filename = ServletUtil.formFilename( contentPath, path);

    log.debug( "returnFile(): returning file <" + filename + ">.");
    // No file, nothing to view
    if (filename == null) {
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // dontallow ..
    if (filename.indexOf("..") != -1) {
      ServletUtil.logServerAccess( HttpServletResponse.SC_FORBIDDEN, 0 );
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    // dont allow access to WEB-INF or META-INF
    String upper = filename.toUpperCase();
    if (upper.indexOf("WEB-INF") != -1 || upper.indexOf("META-INF") != -1) {
      ServletUtil.logServerAccess( HttpServletResponse.SC_FORBIDDEN, 0 );
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    returnFile( servlet, req, res, new File(filename), contentType);
  }

  /**
   * Write a file to the response stream.
   *
   * @param servlet called from here
   * @param req the request
   * @param res the response
   * @param file to serve
   * @param contentType content type, or null
   *
   * @throws IOException
   */
  public static void returnFile( HttpServlet servlet, HttpServletRequest req, HttpServletResponse res, File file, String contentType)
          throws IOException
  {
    // No file, nothing to view
    if (file == null) {
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // check that it exists
    if (!file.exists()) {
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // Set the type of the file
    String filename = file.getPath();
    if (null == contentType) {
      if (filename.endsWith(".html"))
        contentType = "text/html; charset=iso-8859-1";
      else if (filename.endsWith(".xml"))
        contentType = "text/xml; charset=iso-8859-1";
      else if (filename.endsWith(".txt") || (filename.endsWith(".log")))
        contentType = CONTENT_TEXT;
      else if (filename.indexOf(".log.") > 0)
        contentType = CONTENT_TEXT;
      else if (filename.endsWith(".nc"))
        contentType = "application/x-netcdf";
      else
        contentType = servlet.getServletContext().getMimeType( filename);

      if (contentType == null) contentType = "application/octet-stream";
    }
    res.setContentType(contentType);

    // see if its a Range Request
    boolean isRangeRequest = false;
    long startPos = 0, endPos = Long.MAX_VALUE;
    String rangeRequest = req.getHeader("Range");
    if (rangeRequest != null) { // bytes=12-34 or bytes=12-
      int pos = rangeRequest.indexOf("=");
      if (pos > 0) {
        int pos2 = rangeRequest.indexOf("-");
        if (pos2 > 0) {
          String startString = rangeRequest.substring(pos+1, pos2);
          String endString = rangeRequest.substring(pos2+1);
          startPos = Long.parseLong(startString);
          if (endString.length() > 0)
            endPos = Long.parseLong(endString) + 1;
          isRangeRequest = true;
        }
      }
    }

    // set content length
    long  fileSize = file.length();
    int contentLength = (int) fileSize;
    if (isRangeRequest) {
      endPos = Math.min( endPos, fileSize);
      contentLength = (int) (endPos - startPos);
    }
    res.setContentLength( contentLength);

    boolean debugRequest = Debug.isSet("returnFile");
    if (debugRequest) log.debug("returnFile(): filename = "+filename+" contentType = "+contentType +
        " contentLength = "+file.length());

    // indicate we allow Range Requests
    res.addHeader("Accept-Ranges","bytes");

    if (req.getMethod().equals("HEAD")) {
      ServletUtil.logServerAccess( HttpServletResponse.SC_OK, 0);
      return;
    }

    try {

      if (isRangeRequest) {
        // set before content is sent
        res.addHeader("Content-Range","bytes "+startPos+"-"+(endPos-1)+"/"+fileSize);
        res.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        ucar.unidata.io.RandomAccessFile raf = null;
        try {
          raf = FileCache.acquire(filename);
          thredds.util.IO.copyRafB(raf, startPos, contentLength, res.getOutputStream(), new byte[60000]);
          ServletUtil.logServerAccess(HttpServletResponse.SC_PARTIAL_CONTENT, contentLength);
          return;
        } finally {
          if (raf != null) FileCache.release(raf);
        }
      }

    // Return the file
      ServletOutputStream out = res.getOutputStream();
      thredds.util.IO.copyFileB(file, out, 60000);
      res.flushBuffer();
      out.close();
      if ( debugRequest ) log.debug("returnFile(): returnFile ok = "+filename);
      ServletUtil.logServerAccess( HttpServletResponse.SC_OK, contentLength );
    }
    // @todo Split up this exception handling: those from file access vs those from dealing with response
    //       File access: catch and res.sendError()
    //       response: don't catch (let bubble up out of doGet() etc)
    catch (FileNotFoundException e) {
      log.error("returnFile(): FileNotFoundException= "+filename);
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
    catch (java.net.SocketException e) {
      log.info("returnFile(): SocketException sending file: " + filename+" "+e.getMessage());
      ServletUtil.logServerAccess( 1000, 0 ); // dunno what error code to log
    }
    catch (IOException e) {
      String eName = e.getClass().getName(); // dont want compile time dependency on ClientAbortException
      if (eName.equals("org.apache.catalina.connector.ClientAbortException")) {
        log.info("returnFile(): ClientAbortException while sending file: " + filename+" "+e.getMessage());
        ServletUtil.logServerAccess( 1000, 0 ); // dunno what error code to log
        return;
      }

      log.error("returnFile(): IOException ("+ e.getClass().getName() +") sending file ", e);
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      res.sendError(HttpServletResponse.SC_NOT_FOUND,  "Problem sending file: " + e.getMessage());
    }
  }

  public static void returnString( String contents, HttpServletResponse res )
    throws IOException {

    try {
      ServletOutputStream out = res.getOutputStream();
      thredds.util.IO.copy( new ByteArrayInputStream(contents.getBytes()), out);
      ServletUtil.logServerAccess( HttpServletResponse.SC_OK, contents.length() );
    }
    catch (IOException e) {
      log.error(" IOException sending string: ", e);
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      res.sendError(HttpServletResponse.SC_NOT_FOUND,  "Problem sending string: " + e.getMessage());
    }
  }

  public static String getReletiveURL( HttpServletRequest req) {
    return req.getContextPath()+req.getServletPath()+req.getPathInfo();
  }

  public static void forwardToCatalogServices(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {

    String reqs = "catalog="+getReletiveURL( req);
    String query = req.getQueryString();
    if (query != null)
      reqs = reqs+"&"+query;
    log.info( "forwardToCatalogServices(): request string = \"/catalog.html?" + reqs + "\"");

    // dispatch to CatalogHtml servlet
    // "The pathname specified may be relative, although it cannot extend outside the current servlet context.
    // "If the path begins with a "/" it is interpreted as relative to the current context root."
    RequestDispatcher dispatch = req.getRequestDispatcher("/catalog.html?"+reqs);
    if (dispatch != null)
      dispatch.forward(req, res);
    else
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
  }


  public static boolean saveFile(HttpServlet servlet, String contentPath, String path, HttpServletRequest req,
                                 HttpServletResponse res) {

    // @todo Need to use logServerAccess() below here.
    boolean debugRequest = Debug.isSet("SaveFile");
    if (debugRequest) log.debug( " saveFile(): path= " + path );

    String filename = contentPath + path; // absolute path
    File want = new File(filename);

    // backup current version if it exists
    int version = getBackupVersion(want.getParent(), want.getName());
    String fileSave = filename + "~"+version;
    File file = new File( filename);
    if (file.exists()) {
      try {
        thredds.util.IO.copyFile(filename, fileSave);
      } catch (IOException e) {
        log.error("saveFile(): Unable to save copy of file "+filename+" to "+fileSave+"\n"+e.getMessage());
        return false;
      }
    }

    // save new file
    try {
      OutputStream out = new BufferedOutputStream( new FileOutputStream( filename));
      thredds.util.IO.copy(req.getInputStream(), out);
      out.close();
      if ( debugRequest ) log.debug("saveFile(): ok= "+filename);
      res.setStatus( HttpServletResponse.SC_OK);
      ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1);
      return true;
    } catch (IOException e) {
      log.error("saveFile(): Unable to PUT file "+filename+" to "+fileSave+"\n"+e.getMessage());
      return false;
    }

  }

  private static int getBackupVersion(String dirName, String fileName) {
    int maxN = 0;
    File dir = new File(dirName);
    if (!dir.exists())
      return -1;

    String[] files = dir.list();
    if (null == files)
      return -1;

    for (int i=0; i< files.length; i++) {
      String name = files[i];
      if (name.indexOf(fileName) < 0) continue;
      int pos = name.indexOf('~');
      if (pos < 0) continue;
      String ver = name.substring(pos+1);
      int n=0;
      try {
        n = Integer.parseInt(ver);
      } catch (NumberFormatException e) {
        log.error("Format Integer error on backup filename= "+ver);
      }
      maxN = Math.max( n, maxN);
    }
    return maxN+1;
  }

  static public boolean copyDir(String fromDir, String toDir) throws IOException {
    File contentFile = new File(toDir+".INIT");
    if (!contentFile.exists()) {
      thredds.util.IO.copyDirTree(fromDir, toDir);
      contentFile.createNewFile();
      return true;
    }
    return false;
  }

  /***************************************************************************
   * Sends an error to the client.
   *
   * @param t The exception that caused the problem.
   * @param res The <code>HttpServletResponse</code> for the client.
   */
  static public void handleException(Throwable t, HttpServletResponse res) {
    try {
      String message = t.getMessage();
      if (message == null) message = "NULL message "+t.getClass().getName();
      if (Debug.isSet("trustedMode")) { // security issue: only show stack if trusted
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bs);
        t.printStackTrace(ps);
        message = new String( bs.toByteArray());
      }
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, message.length());
      log.error("handleException", t);
      t.printStackTrace(); // debugging - log.error not showing stack trace !!      
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
    } catch (IOException e) {
      log.error("handleException(): IOException", e);
      t.printStackTrace();
    }
  }

  static public void showServerInfo(HttpServlet servlet, PrintStream out) {
    out.println("Server Info");
    out.println(" getDocumentBuilderFactoryVersion(): " + XMLEntityResolver.getDocumentBuilderFactoryVersion());
    out.println();

    Properties sysp = System.getProperties();
    Enumeration e = sysp.propertyNames();
    ArrayList list = Collections.list( e);
    Collections.sort(list);

    out.println("System Properties:");
    for (int i = 0; i < list.size(); i++) {
      String name =  (String) list.get(i);
      String value = System.getProperty(name);
      out.println("  "+name+" = "+value);
    }
    out.println();
  }

  static public void showServletInfo(HttpServlet servlet, PrintStream out) {
   out.println("Servlet Info");
    out.println(" getServletName(): " + servlet.getServletName());
    out.println(" getRootPath(): " + getRootPath(servlet));
    out.println(" Init Parameters:");
    Enumeration params = servlet.getInitParameterNames();
    while (params.hasMoreElements()) {
      String name = (String) params.nextElement();
      out.println("  "+name + ": " + servlet.getInitParameter(name));
    }
    out.println();

    ServletContext context = servlet.getServletContext();
    out.println("Context Info");

    try {
      out.println(" context.getResource('/'): " + context.getResource("/"));
    } catch (java.net.MalformedURLException e) { } // cant happen
    out.println(" context.getServerInfo(): " + context.getServerInfo());
    out.println("  name: " + getServerInfoName(context.getServerInfo()));
    out.println("  version: " + getServerInfoVersion(context.getServerInfo()));

    out.println(" context.getInitParameterNames():");
    params = context.getInitParameterNames();
    while (params.hasMoreElements()) {
      String name = (String) params.nextElement();
      out.println("  "+name + ": " + context.getInitParameter(name));
    }

    out.println(" context.getAttributeNames():");
    params = context.getAttributeNames();
    while (params.hasMoreElements()) {
      String name = (String) params.nextElement();
      out.println("  context.getAttribute(\"" + name + "\"): " +
                     context.getAttribute(name));
    }
    out.println();
  }

  /** Show the pieces of the request, for debugging */
  public static String getRequestParsed(HttpServletRequest req) {
    return req.getRequestURI()+" = "+req.getContextPath()+"(context), "+
        req.getServletPath()+"(servletPath), "+
        req.getPathInfo()+"(pathInfo), "+req.getQueryString()+"(query)";
  }

  /** This is everything except the query string */
  public static String getRequestBase(HttpServletRequest req) {
    // return "http://"+req.getServerName()+":"+ req.getServerPort()+req.getRequestURI();
    return req.getRequestURL().toString();
  }

  /** The request base as a URI */
  public static URI getRequestURI(HttpServletRequest req) {
    try {
      return new URI( getRequestBase( req));
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return null;
    }
  }

  /** servletPath + pathInfo */
  public static String getRequestPath(HttpServletRequest req) {
    StringBuffer buff = new StringBuffer();
    if (req.getServletPath() != null )
      buff.append( req.getServletPath());
    if (req.getPathInfo() != null )
      buff.append( req.getPathInfo());
    return buff.toString();
  }

  /** The entire request including query string */
  public static String getRequest(HttpServletRequest req) {
    String query = req.getQueryString();
    return getRequestBase(req)+(query == null ? "" : "?"+req.getQueryString());
  }

  public static String getParameterIgnoreCase(HttpServletRequest req, String paramName) {
    Enumeration e = req.getParameterNames();
    while (e.hasMoreElements()) {
      String s = (String) e.nextElement();
      if (s.equalsIgnoreCase(paramName))
        return req.getParameter( s);
    }
    return null;
  }

  public static String[] getParameterValuesIgnoreCase(HttpServletRequest req, String paramName) {
    Enumeration e = req.getParameterNames();
    while (e.hasMoreElements()) {
      String s = (String) e.nextElement();
      if (s.equalsIgnoreCase(paramName))
        return req.getParameterValues( s);
    }
    return null;
  }


  public static String getFileURL(String filename) {
    filename = filename.replace('\\','/');
    filename = StringUtil.replace(filename, ' ', "+");
    return "file:"+filename;
  }

  public static String getFileURL2(String filename) {
    File f = new File(filename);
    try {
      return f.toURL().toString();
    } catch (java.net.MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  static public String showRequestDetail(HttpServlet servlet, HttpServletRequest req) {
    StringBuffer sbuff = new StringBuffer();

    sbuff.append("Request Info\n");
    sbuff.append(" req.getServerName(): " + req.getServerName()+"\n");
    sbuff.append(" req.getServerPort(): " + req.getServerPort()+"\n");
    sbuff.append(" req.getContextPath:"+ req.getContextPath()+"\n");
    sbuff.append(" req.getServletPath:"+ req.getServletPath()+"\n");
    sbuff.append(" req.getPathInfo:"+ req.getPathInfo()+"\n");
    sbuff.append(" req.getQueryString:"+ req.getQueryString()+"\n");
    sbuff.append(" req.getRequestURI:"+ req.getRequestURI()+"\n");
    sbuff.append(" getRequestBase:"+ getRequestBase(req)+"\n");
    sbuff.append(" getRequest:"+ getRequest(req)+"\n");
    sbuff.append("\n");

    sbuff.append(" req.getPathTranslated:"+ req.getPathTranslated()+"\n");
    String path = req.getPathTranslated();
    if ( path != null) {
      ServletContext context = servlet.getServletContext();
      sbuff.append(" getMimeType:"+ context.getMimeType(path)+"\n");
    }
    sbuff.append("\n");
    sbuff.append(" req.getScheme:"+ req.getScheme()+"\n");
    sbuff.append(" req.getProtocol:"+ req.getProtocol()+"\n");
    sbuff.append(" req.getMethod:"+ req.getMethod()+"\n");
    sbuff.append("\n");
    sbuff.append(" req.getContentType:"+ req.getContentType()+"\n");
    sbuff.append(" req.getContentLength:"+ req.getContentLength()+"\n");

    sbuff.append(" req.getRemoteAddr():"+req.getRemoteAddr());
    try {
      sbuff.append(" getRemoteHost():"+ java.net.InetAddress.getByName(req.getRemoteHost()).getHostName()+"\n");
    } catch (java.net.UnknownHostException e) {
      sbuff.append(" getRemoteHost():"+ e.getMessage()+"\n");
    }
    sbuff.append(" getRemoteUser():"+req.getRemoteUser()+"\n");

    sbuff.append("\n");
    sbuff.append("Request Parameters:\n");
    Enumeration params = req.getParameterNames();
    while (params.hasMoreElements()) {
      String name = (String) params.nextElement();
      String values[] = req.getParameterValues(name);
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          sbuff.append("  "+name + "  (" + i + "): " + values[i]+"\n");
        }
      }
    }
    sbuff.append("\n");

    sbuff.append("Request Headers:\n");
    Enumeration names = req.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      Enumeration values = req.getHeaders(name);  // support multiple values
      if (values != null) {
        while (values.hasMoreElements()) {
          String value = (String) values.nextElement();
          sbuff.append("  "+name + ": " + value+"\n");
        }
      }
    }
    sbuff.append(" ------------------\n");

    return sbuff.toString();
  }

  static public void showSession(HttpServletRequest req, HttpServletResponse res,
                                 PrintStream out)  {

    // res.setContentType("text/html");

    // Get the current session object, create one if necessary
    HttpSession session = req.getSession();

    // Increment the hit count for this page. The value is saved
    // in this client's session under the name "snoop.count".
    Integer count = (Integer)session.getAttribute("snoop.count");
    if (count == null)
      count = new Integer(1);
    else
      count = new Integer(count.intValue() + 1);
    session.setAttribute("snoop.count", count);

    out.println("<HTML><HEAD><TITLE>SessionSnoop</TITLE></HEAD>");
    out.println("<BODY><H1>Session Snoop</H1>");

    // Display the hit count for this page
    out.println("You've visited this page " + count +
      ((count.intValue() == 1) ? " time." : " times."));

    out.println("<P>");

    out.println("<H3>Here is your saved session data:</H3>");
    Enumeration atts = session.getAttributeNames();
    while (atts.hasMoreElements()) {
      String name = (String) atts.nextElement();
      out.println(name + ": " + session.getAttribute(name) + "<BR>");
    }

    out.println("<H3>Here are some vital stats on your session:</H3>");
    out.println("Session id: " + session.getId() +
                " <I>(keep it secret)</I><BR>");
    out.println("New session: " + session.isNew() + "<BR>");
    out.println("Timeout: " + session.getMaxInactiveInterval());
    out.println("<I>(" + session.getMaxInactiveInterval() / 60 +
                " minutes)</I><BR>");
    out.println("Creation time: " + session.getCreationTime());
    out.println("<I>(" + new Date(session.getCreationTime()) + ")</I><BR>");
    out.println("Last access time: " + session.getLastAccessedTime());
    out.println("<I>(" + new Date(session.getLastAccessedTime()) +
                ")</I><BR>");

    out.println("Requested session ID from cookie: " +
                req.isRequestedSessionIdFromCookie() + "<BR>");
    out.println("Requested session ID from URL: " +
                req.isRequestedSessionIdFromURL() + "<BR>");
    out.println("Requested session ID valid: " +
                 req.isRequestedSessionIdValid() + "<BR>");

    out.println("<H3>Test URL Rewriting</H3>");
    out.println("Click <A HREF=\"" +
                res.encodeURL(req.getRequestURI()) + "\">here</A>");
    out.println("to test that session tracking works via URL");
    out.println("rewriting even when cookies aren't supported.");

    out.println("</BODY></HTML>");
  }



  static public void showSession(HttpServletRequest req, PrintStream out)  {

    // res.setContentType("text/html");

    // Get the current session object, create one if necessary
    HttpSession session = req.getSession();

    out.println("Session id: " + session.getId());
    out.println(" session.isNew(): " + session.isNew());
    out.println(" session.getMaxInactiveInterval(): " + session.getMaxInactiveInterval()+" secs");
    out.println(" session.getCreationTime(): " + session.getCreationTime()+" ("+new Date(session.getCreationTime()) + ")");
    out.println(" session.getLastAccessedTime(): " + session.getLastAccessedTime()+" ("+new Date(session.getLastAccessedTime()) + ")");
    out.println(" req.isRequestedSessionIdFromCookie: " + req.isRequestedSessionIdFromCookie());
    out.println(" req.isRequestedSessionIdFromURL: " + req.isRequestedSessionIdFromURL());
    out.println(" req.isRequestedSessionIdValid: " + req.isRequestedSessionIdValid());

    out.println("Saved session Attributes:");
    Enumeration atts = session.getAttributeNames();
    while (atts.hasMoreElements()) {
      String name = (String) atts.nextElement();
      out.println(" "+name + ": " + session.getAttribute(name) + "<BR>");
    }

  }

  static public String showSecurity(HttpServletRequest req) {
    StringBuffer sbuff = new StringBuffer();

    sbuff.append("Security Info\n");
    sbuff.append(" req.getRemoteUser(): " + req.getRemoteUser()+"\n");
    sbuff.append(" req.getUserPrincipal(): " + req.getUserPrincipal()+"\n");
    sbuff.append(" req.isUserInRole(admin):"+ req.isUserInRole("admin")+"\n");
    sbuff.append(" req.isUserInRole(tdsConfig):"+ req.isUserInRole("tdsConfig")+"\n");
    sbuff.append(" req.isUserInRole(resourceControl):"+ req.isUserInRole("resourceControl")+"\n");
    sbuff.append(" req.isUserInRole(badRole):"+ req.isUserInRole("badRole")+"\n");
    sbuff.append(" ------------------\n");

    return sbuff.toString();
  }

  static private String getServerInfoName(String serverInfo) {
    int slash = serverInfo.indexOf('/');
    if (slash == -1) return serverInfo;
    else return serverInfo.substring(0, slash);
  }

  static private String getServerInfoVersion(String serverInfo) {
    // Version info is everything between the slash and the space
    int slash = serverInfo.indexOf('/');
    if (slash == -1) return null;
    int space = serverInfo.indexOf(' ', slash);
    if (space == -1) space = serverInfo.length();
    return serverInfo.substring(slash + 1, space);
  }

  static public void showThreads(PrintStream pw) {
    Thread current = Thread.currentThread();
    ThreadGroup group = current.getThreadGroup();
    while (true) {
      if (group.getParent() == null) break;
      group = group.getParent();
    }
    showThreads( pw, group, current);
  }

  static private void showThreads(PrintStream pw, ThreadGroup g, Thread current) {
    int nthreads = g.activeCount();
    pw.println("\nThread Group = "+g.getName()+" activeCount= "+nthreads);
    Thread[] tarray = new Thread[nthreads];
    int n = g.enumerate(tarray, false);
    for (int i = 0; i < n; i++) {
      Thread thread = tarray[i];
      ClassLoader loader = thread.getContextClassLoader();
      String loaderName = (loader == null) ? "Default" : loader.getClass().getName();
      Thread.State state = thread.getState(); // LOOK JDK 1.5
      pw.print("   "+thread.getId() +" "+thread.getName() +" "+state +" "+loaderName);
      if (thread == current)
        pw.println(" **** CURRENT ***");
      else
        pw.println();
    }

    int ngroups = g.activeGroupCount();
    ThreadGroup[] garray = new ThreadGroup[ngroups];
    int ng = g.enumerate(garray, false);
    for (int i = 0; i < ng; i++) {
      ThreadGroup nested = garray[i];
      showThreads(pw, nested, current);
    }

  }

  public static void main(String[] args) {
    String s = "C:/Program Files/you";
    System.out.println("FileURL = "+getFileURL(s));
    System.out.println("FileURL2 = "+getFileURL2(s));
  }

}