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

package thredds.servlet;

import java.io.*;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.*;
import javax.servlet.http.*;

import ucar.unidata.util.EscapeStrings;
import ucar.nc2.util.cache.FileCacheRaf;
import ucar.nc2.util.IO;
import thredds.catalog.XMLEntityResolver;
import thredds.util.RequestForwardUtils;
import ucar.unidata.util.StringUtil;

public class ServletUtil {

  public static final String CONTENT_TEXT = "text/plain; charset=utf-8";

  // bogus status returns for our logging
  public static final int STATUS_CLIENT_ABORT = 1000;
  public static final int STATUS_FORWARDED = 1001;
  public static final int STATUS_FORWARD_FAILURE = 1002;

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( ServletUtil.class );
  static private boolean isDebugInit = false;

  static private String contextPath = null;
  static private String rootPath = null;
  static private String contentPath = null;

  /**
   *
   * @param context the Servlet context.
   * @deprecated Now handled in TdsContext.init().
   */
  static public void initContext(ServletContext context)
{
//    setContextPath(context);
    if ( contextPath == null )
    {
      // Servlet 2.5 allows the following.
      //contextPath = servletContext.getContextPath();
      String tmpContextPath = context.getInitParameter( "ContextPath" );  // cannot be overridden in the ThreddsConfig file
      if ( tmpContextPath == null )
        tmpContextPath = "thredds";
      contextPath = "/" + tmpContextPath;
    }
//    setRootPath(context);
    if ( rootPath == null )
    {
      rootPath = context.getRealPath("/");
      rootPath = rootPath.replace('\\', '/');
    }

//    setContentPath();
    if ( contentPath == null )
    {
      String tmpContentPath = "../../content" + getContextPath() + "/";
      File cf = new File(getRootPath(), tmpContentPath);
      try {
        contentPath = cf.getCanonicalPath() + "/";
        contentPath = contentPath.replace('\\', '/');
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

//    initDebugging(context);
    initDebugging( context );
  }

  static public void setContextPath( String newContextPath )
  {
    contextPath = newContextPath;
  }

  static public void setRootPath( String newRootPath )
  {
    rootPath = newRootPath;
  }

  static public void setContentPath( String newContentPath)
  {
    contentPath = newContentPath;
    if (!contentPath.endsWith("/"))
      contentPath = contentPath + "/";
  }

  static public void initDebugging(ServletContext webapp) {
    if (isDebugInit) return;
    isDebugInit = true;

    String debugOn = webapp.getInitParameter("DebugOn");
    if (debugOn != null) {
      StringTokenizer toker = new StringTokenizer(debugOn);
      while (toker.hasMoreTokens())
        Debug.set(toker.nextToken(), true);
    }
  }

  /**
   * Return the real path on the servers file system that corresponds to the root document ("/") on the given servlet.
   * @return the real path on the servers file system that corresponds to the root document ("/") on the given servlet.
   */
  public static String getRootPath() {
    return rootPath;
  }

  /**
   * Return the context path for the given servlet.
   * Note - ToDo: Why not just use ServletContext.getServletContextName()?
   *
   * @return the context path for the given servlet.
   */
  public static String getContextPath() {
    return contextPath;
  }


  /**
   * Return the content path for the given servlet.
   *
   * @return the content path for the given servlet.
   */
  public static String getContentPath() {
    return contentPath;
  }

  /**
   * Return the default/initial content path for the given servlet. The
   * content of which is copied to the content path when the web app
   * is first installed.
   *
   * @return the default/initial content path for the given servlet.
   */
  public static String getInitialContentPath() {
    return getRootPath() + "/WEB-INF/altContent/startup/";
  }

  /**
   * Return the file path dealing with leading and trailing path
   * seperators (which must be a slash ("/")) for the given directory
   * and file paths.
   *
   * Note: Dealing with path strings is fragile.
   * ToDo: Switch from using path strings to java.io.Files.
   *
   * @param dirPath the directory path.
   * @param filePath the file path.
   * @return a full file path with the given directory and file paths.
   */
  public static String formFilename(String dirPath, String filePath) {
    if ((dirPath == null) || (filePath == null))
      return null;

    if (filePath.startsWith("/"))
      filePath = filePath.substring(1);

    return dirPath.endsWith("/") ? dirPath + filePath : dirPath + "/" + filePath;
  }

  /**
   * Handle a request for a raw/static file (i.e., not a catalog or dataset request).
   * <p/>
   * Look in the content (user) directory then the root (distribution) directory
   * for a file that matches the given path and, if found, return it as the
   * content of the HttpServletResponse. If the file is forbidden (i.e., the
   * path contains a "..", "WEB-INF", or "META-INF" directory), send a
   * HttpServletResponse.SC_FORBIDDEN response. If no file matches the request
   * (including an "index.html" file if the path ends in "/"), send an
   * HttpServletResponse.SC_NOT_FOUND..
   * <p/>
   * <ol>
   * <li>Make sure the path does not contain ".." directories. </li>
   * <li>Make sure the path does not contain "WEB-INF" or "META-INF". </li>
   * <li>Check for requested file in the content directory
   * (if the path is a directory, make sure the path ends with "/" and
   * check for an "index.html" file). </li>
   * <li>Check for requested file in the root directory
   * (if the path is a directory, make sure the path ends with "/" and
   * check for an "index.html" file).</li>
   * </ol
   *
   * @param path    the requested path
   * @param servlet the servlet handling the request
   * @param req     the HttpServletRequest
   * @param res     the HttpServletResponse
   * @throws IOException if can't complete request due to IO problems.
   */
  public static void handleRequestForRawFile(String path, HttpServlet servlet, HttpServletRequest req, HttpServletResponse res)
      throws IOException {
    // Don't allow ".." directories in path.
    if (path.indexOf("/../") != -1
        || path.equals("..")
        || path.startsWith("../")
        || path.endsWith("/..")) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Path cannot contain \"..\" directory.");
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, -1));
      return;
    }

    // Don't allow access to WEB-INF or META-INF directories.
    String upper = path.toUpperCase();
    if (upper.indexOf("WEB-INF") != -1
        || upper.indexOf("META-INF") != -1) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Path cannot contain \"WEB-INF\" or \"META-INF\".");
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, -1));
      return;
    }

    // Find a regular file
    File regFile = null;
    // Look in content directory for regular file.
    File cFile = new File(ServletUtil.formFilename( getContentPath(), path));
    if (cFile.exists()) {
      if (cFile.isDirectory()) {
        if (!path.endsWith("/")) {
          String newPath = req.getRequestURL().append("/").toString();
          ServletUtil.sendPermanentRedirect(newPath, req, res);
        }
        // If request path is a directory, check for index.html file.
        cFile = new File(cFile, "index.html");
        if (cFile.exists() && !cFile.isDirectory())
          regFile = cFile;
      }
      // If not a directory, use this file.
      else
        regFile = cFile;
    }

    if (regFile == null) {
      // Look in root directory.
      File rFile = new File( ServletUtil.formFilename(getRootPath(), path));
      if (rFile.exists()) {
        if (rFile.isDirectory()) {
          if (!path.endsWith("/")) {
            String newPath = req.getRequestURL().append("/").toString();
            ServletUtil.sendPermanentRedirect(newPath, req, res);
          }
          rFile = new File(rFile, "index.html");
          if (rFile.exists() && !rFile.isDirectory())
            regFile = rFile;
        } else
          regFile = rFile;
      }
    }

    if (regFile == null) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
      return;
    }

    ServletUtil.returnFile(servlet, req, res, regFile, null);
  }

  /**
   * Handle an explicit request for a content directory file (path must start
   * with "/content/".
   * <p/>
   * Note: As these requests will show the configuration files for the server,
   * these requests should be covered by security constraints.
   * <p/>
   * <ol>
   * <li>Make sure the path does not contain ".." directories. </li>
   * <li>Check for the requested file in the content directory. </li>
   * </ol
   *
   * @param path    the requested path (must start with "/content/")
   * @param servlet the servlet handling the request
   * @param req     the HttpServletRequest
   * @param res     the HttpServletResponse
   * @throws IOException if can't complete request due to IO problems.
   */
  public static void handleRequestForContentFile(String path, HttpServlet servlet, HttpServletRequest req, HttpServletResponse res)
      throws IOException {
    handleRequestForContentOrRootFile("/content/", path, servlet, req, res);
  }

  /**
   * Handle an explicit request for a root directory file (path must start
   * with "/root/".
   * <p/>
   * Note: As these requests will show the configuration files for the server,
   * these requests should be covered by security constraints.
   * <p/>
   * <ol>
   * <li>Make sure the path does not contain ".." directories. </li>
   * <li>Check for the requested file in the root directory. </li>
   * </ol
   *
   * @param path    the requested path (must start with "/root/")
   * @param servlet the servlet handling the request
   * @param req     the HttpServletRequest
   * @param res     the HttpServletResponse
   * @throws IOException if can't complete request due to IO problems.
   */
  public static void handleRequestForRootFile(String path, HttpServlet servlet, HttpServletRequest req, HttpServletResponse res)
      throws IOException {
    handleRequestForContentOrRootFile("/root/", path, servlet, req, res);
  }

  /**
   * Convenience routine used by handleRequestForContentFile()
   * and handleRequestForRootFile().
   *
   * @param pathPrefix
   * @param path
   * @param servlet
   * @param req request
   * @param res response
   * @throws IOException on IO error
   */
  private static void handleRequestForContentOrRootFile(String pathPrefix, String path, HttpServlet servlet, HttpServletRequest req, HttpServletResponse res)
      throws IOException {
    if (!pathPrefix.equals("/content/")
        && !pathPrefix.equals("/root/")) {
      log.error("handleRequestForContentFile(): The path prefix <" + pathPrefix + "> must be \"/content/\" or \"/root/\".");
      throw new IllegalArgumentException("Path prefix must be \"/content/\" or \"/root/\".");
    }

    if (!path.startsWith(pathPrefix)) {
      log.error("handleRequestForContentFile(): path <" + path + "> must start with \"" + pathPrefix + "\".");
      throw new IllegalArgumentException("Path must start with \"" + pathPrefix + "\".");
    }

    // Don't allow ".." directories in path.
    if (path.indexOf("/../") != -1
        || path.equals("..")
        || path.startsWith("../")
        || path.endsWith("/..")) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Path cannot contain \"..\" directory.");
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, -1));
      return;
    }

    // Find the requested file.
    File file = new File(ServletUtil.formFilename(getContentPath(), path.substring(pathPrefix.length() - 1)));
    if (file.exists()) {
      // Do not allow request for a directory.
      if (file.isDirectory()) {
        if (!path.endsWith("/")) {
          String redirectPath = req.getRequestURL().append("/").toString();
          ServletUtil.sendPermanentRedirect(redirectPath, req, res);
          return;
        }

        int i = HtmlWriter.getInstance().writeDirectory(res, file, path);
        int status = i == 0 ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_OK;
        log.info( UsageLog.closingMessageForRequestContext( status, i ) );

        return;
      }

      // Return the requested file.
      ServletUtil.returnFile(servlet, req, res, file, null);
    } else {
      // Requested file not found.
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
      res.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
    }
  }

  /**
   * Send a permanent redirect (HTTP status 301 "Moved Permanently") response
   * with the given target path.
   * <p/>
   * The given target path may be relative or absolute. If it is relative, it
   * will be resolved against the request URL.
   *
   * @param targetPath the path to which the client is redirected.
   * @param req        the HttpServletRequest
   * @param res        the HttpServletResponse
   * @throws IOException if can't write the response.
   */
  public static void sendPermanentRedirect(String targetPath, HttpServletRequest req, HttpServletResponse res)
      throws IOException {
    // Absolute URL needed so resolve the target path against the request URL.
    URI uri;
    try {
      uri = new URI(req.getRequestURL().toString());
    }
    catch (URISyntaxException e) {
      log.error("sendPermanentRedirect(): Bad syntax on request URL <" + req.getRequestURL() + ">.", e);
      log.info( "sendPermanentRedirect(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0));
      if ( ! res.isCommitted() ) res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }
    String absolutePath = uri.resolve(targetPath).toString();
    absolutePath = res.encodeRedirectURL(absolutePath);

    res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    res.addHeader("Location", absolutePath);

    String title = "Permanently Moved - 301";
    String body = new StringBuilder()
        .append("<p>")
        .append("The requested URL <").append(req.getRequestURL())
        .append("> has been permanently moved (HTTP status code 301).")
        .append(" Instead, please use the following URL: <a href=\"").append(absolutePath).append("\">").append(absolutePath).append("</a>.")
        .append("</p>")
        .toString();
    String htmlResp = new StringBuilder()
        .append(HtmlWriter.getInstance().getHtmlDoctypeAndOpenTag())
        .append("<head><title>")
        .append(title)
        .append("</title></head><body>")
        .append("<h1>").append(title).append("</h1>")
        .append(body)
        .append("</body></html>")
        .toString();

    log.info( "sendPermanentRedirect(): redirect to " + absolutePath);
    log.info( "sendPermanentRedirect(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_MOVED_PERMANENTLY, htmlResp.length()));

    // Write the catalog out.
    PrintWriter out = res.getWriter();
    res.setContentType("text/html");
    out.print(htmlResp);
    out.flush();
  }

  /**
   * Write a file to the response stream.
   *
   * @param servlet     called from here
   * @param contentPath file root path
   * @param path        file path reletive to the root
   * @param req         the request
   * @param res         the response
   * @param contentType content type, or null
   * @throws IOException on write error
   */
  public static void returnFile(HttpServlet servlet, String contentPath, String path,
                                HttpServletRequest req, HttpServletResponse res, String contentType) throws IOException {

    String filename = ServletUtil.formFilename(contentPath, path);

    log.debug("returnFile(): returning file <" + filename + ">.");
    // No file, nothing to view
    if (filename == null) {
      log.info( "returnFile(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // dontallow ..
    if (filename.indexOf("..") != -1) {
      log.info( "returnFile(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, 0));
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    // dont allow access to WEB-INF or META-INF
    String upper = filename.toUpperCase();
    if (upper.indexOf("WEB-INF") != -1 || upper.indexOf("META-INF") != -1) {
      log.info( "returnFile(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, 0));
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    returnFile(servlet, req, res, new File(filename), contentType);
  }

  static private FileCacheRaf fileCacheRaf;
  static public void setFileCache( FileCacheRaf fileCache) { fileCacheRaf = fileCache; }
  static public FileCacheRaf getFileCache( ) { return fileCacheRaf; }

  /**
   * Write a file to the response stream. Handles Range requests.
   *
   * @param servlet     called from here
   * @param req         the request
   * @param res         the response
   * @param file        to serve
   * @param contentType content type, if null, will try to guess
   * @throws IOException on write error
   */
  public static void returnFile(HttpServlet servlet, HttpServletRequest req, HttpServletResponse res, File file, String contentType)
      throws IOException {

    // No file, nothing to view
    if (file == null) {
      log.info( "returnFile(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // check that it exists
    if (!file.exists()) {
      log.info( "returnFile(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // not a directory
    if (!file.isFile()) {
      log.info( "returnFile(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, 0));
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
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
        contentType = servlet.getServletContext().getMimeType(filename);

      if (contentType == null) contentType = "application/octet-stream";
    }

    returnFile(req, res, file, contentType);
  }

  /**
   * Write a file to the response stream. Handles Range requests.
   *
   * @param req request
   * @param res response
   * @param file must exists and not be a directory
   * @param contentType must not be null
   * @throws IOException or error
   */
  public static void returnFile(HttpServletRequest req, HttpServletResponse res, File file, String contentType) throws IOException {
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
          String startString = rangeRequest.substring(pos + 1, pos2);
          String endString = rangeRequest.substring(pos2 + 1);
          startPos = Long.parseLong(startString);
          if (endString.length() > 0)
            endPos = Long.parseLong(endString) + 1;
          isRangeRequest = true;
        }
      }
    }

    // set content length
    long fileSize = file.length();
    long contentLength = fileSize;
    if (isRangeRequest) {
      endPos = Math.min(endPos, fileSize);
      contentLength = endPos - startPos;
    }

    res.addHeader("Content-Length", Long.toString(contentLength));
    // res.setContentLength( (int) contentLength);

    String filename = file.getPath();
    boolean debugRequest = Debug.isSet("returnFile");
    if (debugRequest) log.debug("returnFile(): filename = " + filename + " contentType = " + contentType +
        " contentLength = " + contentLength);

    // indicate we allow Range Requests
    res.addHeader("Accept-Ranges", "bytes");

    if (req.getMethod().equals("HEAD")) {
      log.info( "returnFile(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, 0));
      return;
    }

    try {

      if (isRangeRequest) {
        // set before content is sent
        res.addHeader("Content-Range", "bytes " + startPos + "-" + (endPos - 1) + "/" + fileSize);
        res.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        FileCacheRaf.Raf craf = null;
        try {
          craf = fileCacheRaf.acquire(filename);
          IO.copyRafB(craf.getRaf(), startPos, contentLength, res.getOutputStream(), new byte[60000]);
          log.info( "returnFile(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_PARTIAL_CONTENT, contentLength));
          return;
        } finally {
          if (craf != null) fileCacheRaf.release(craf);
        }
      }

      // Return the file
      ServletOutputStream out = res.getOutputStream();
      IO.copyFileB(file, out, 60000);
      res.flushBuffer();
      out.close();
      if (debugRequest) log.debug("returnFile(): returnFile ok = " + filename);
      log.info( "returnFile(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, contentLength));
    }

    // @todo Split up this exception handling: those from file access vs those from dealing with response
    //       File access: catch and res.sendError()
    //       response: don't catch (let bubble up out of doGet() etc)
    catch (FileNotFoundException e) {
      log.error("returnFile(): FileNotFoundException= " + filename);
      log.info( "returnFile(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      if ( ! res.isCommitted() ) res.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
    catch (java.net.SocketException e) {
      log.info("returnFile(): SocketException sending file: " + filename + " " + e.getMessage());
      log.info( "returnFile(): " + UsageLog.closingMessageForRequestContext(STATUS_CLIENT_ABORT, 0));
    }
    catch (IOException e) {
      String eName = e.getClass().getName(); // dont want compile time dependency on ClientAbortException
      if (eName.equals("org.apache.catalina.connector.ClientAbortException")) {
        log.info("returnFile(): ClientAbortException while sending file: " + filename + " " + e.getMessage());
        log.info( "returnFile(): " + UsageLog.closingMessageForRequestContext(STATUS_CLIENT_ABORT, 0));
        return;
      }

      log.error("returnFile(): IOException (" + e.getClass().getName() + ") sending file ", e);
      log.error( "returnFile(): " + UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      if ( ! res.isCommitted() ) res.sendError(HttpServletResponse.SC_NOT_FOUND, "Problem sending file: " + e.getMessage());
    }
  }

  /**
   * Send given content string as the HTTP response.
   *
   * @param contents the string to return as the HTTP response.
   * @param res the HttpServletResponse
   * @throws IOException if an I/O error occurs while writing the response.
   */
  public static void returnString(String contents, HttpServletResponse res)
      throws IOException
  {

    try {
      ServletOutputStream out = res.getOutputStream();
      IO.copy(new ByteArrayInputStream(contents.getBytes()), out);
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, contents.length()));
    }
    catch (IOException e) {
      log.error(" IOException sending string: ", e);
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, "Problem sending string: " + e.getMessage());
    }
  }

  /**
   * Return the request URL relative to the server (i.e., starting with the context path).
   *
   * @param req request
   * @return URL relative to the server
   */
  public static String getReletiveURL(HttpServletRequest req) {
    return req.getContextPath() + req.getServletPath() + req.getPathInfo();
  }

  /**
   * Forward this request to the CatalogServices servlet ("/catalog.html").
   * 
   * @param req request
   * @param res response
   * @throws IOException on IO error
   * @throws ServletException other error
   */
  public static void forwardToCatalogServices(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {

    String reqs = "catalog=" + getReletiveURL(req);
    String query = req.getQueryString();
    if (query != null)
      reqs = reqs + "&" + query;
    log.info("forwardToCatalogServices(): request string = \"/catalog.html?" + reqs + "\"");

    // dispatch to CatalogHtml servlet
    RequestForwardUtils.forwardRequestRelativeToCurrentContext( "/catalog.html?" + reqs, req, res );
  }


  public static boolean saveFile(HttpServlet servlet, String contentPath, String path, HttpServletRequest req,
                                 HttpServletResponse res) {

    // @todo Need to use logServerAccess() below here.
    boolean debugRequest = Debug.isSet("SaveFile");
    if (debugRequest) log.debug(" saveFile(): path= " + path);

    String filename = contentPath + path; // absolute path
    File want = new File(filename);

    // backup current version if it exists
    int version = getBackupVersion(want.getParent(), want.getName());
    String fileSave = filename + "~" + version;
    File file = new File(filename);
    if (file.exists()) {
      try {
        IO.copyFile(filename, fileSave);
      } catch (IOException e) {
        log.error("saveFile(): Unable to save copy of file " + filename + " to " + fileSave + "\n" + e.getMessage());
        return false;
      }
    }

    // save new file
    try {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
      IO.copy(req.getInputStream(), out);
      out.close();
      if (debugRequest) log.debug("saveFile(): ok= " + filename);
      res.setStatus(HttpServletResponse.SC_CREATED);
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_CREATED, -1));
      return true;
    } catch (IOException e) {
      log.error("saveFile(): Unable to PUT file " + filename + " to " + fileSave + "\n" + e.getMessage());
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

    for (String name : files) {
      if (name.indexOf(fileName) < 0) continue;
      int pos = name.indexOf('~');
      if (pos < 0) continue;
      String ver = name.substring(pos + 1);
      int n = 0;
      try {
        n = Integer.parseInt(ver);
      } catch (NumberFormatException e) {
        log.error("Format Integer error on backup filename= " + ver);
      }
      maxN = Math.max(n, maxN);
    }
    return maxN + 1;
  }

  static public boolean copyDir(String fromDir, String toDir) throws IOException {
    File contentFile = new File(toDir + ".INIT");
    if (!contentFile.exists()) {
      IO.copyDirTree(fromDir, toDir);
      contentFile.createNewFile();
      return true;
    }
    return false;
  }

  /**
   * ************************************************************************
   * Sends an error to the client.
   *
   * @param t   The exception that caused the problem.
   * @param res The <code>HttpServletResponse</code> for the client.
   */
  static public void handleException(Throwable t, HttpServletResponse res) {
    try {
      String message = t.getMessage();
      if (message == null) message = "NULL message " + t.getClass().getName();
      if (Debug.isSet("trustedMode")) { // security issue: only show stack if trusted
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bs);
        t.printStackTrace(ps);
        message = new String(bs.toByteArray());
      }
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, message.length()));
      log.error("handleException", t);
      t.printStackTrace(); // debugging - log.error not showing stack trace !!   
      if ( ! res.isCommitted() )
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, message);

    } catch (Throwable e) {
      log.error("handleException() had problem reporting Exception", e);
      t.printStackTrace();
    }
  }

  static public void showServerInfo(PrintStream out) {
    out.println("Server Info");
    out.println(" getDocumentBuilderFactoryVersion(): " + XMLEntityResolver.getDocumentBuilderFactoryVersion());
    out.println();

    Properties sysp = System.getProperties();
    Enumeration e = sysp.propertyNames();
    List<String> list = Collections.list(e);
    Collections.sort(list);

    out.println("System Properties:");
    for (String name : list) {
      String value = System.getProperty(name);
      out.println("  " + name + " = " + value);
    }
    out.println();
  }

  static public void showServletInfo(HttpServlet servlet, PrintStream out) {
    out.println("Servlet Info");
    out.println(" getServletName(): " + servlet.getServletName());
    out.println(" getRootPath(): " + getRootPath());
    out.println(" Init Parameters:");
    Enumeration params = servlet.getInitParameterNames();
    while (params.hasMoreElements()) {
      String name = (String) params.nextElement();
      out.println("  " + name + ": " + servlet.getInitParameter(name));
    }
    out.println();

    ServletContext context = servlet.getServletContext();
    out.println("Context Info");

    try {
      out.println(" context.getResource('/'): " + context.getResource("/"));
    } catch (java.net.MalformedURLException e) {
    } // cant happen
    out.println(" context.getServerInfo(): " + context.getServerInfo());
    out.println("  name: " + getServerInfoName(context.getServerInfo()));
    out.println("  version: " + getServerInfoVersion(context.getServerInfo()));

    out.println(" context.getInitParameterNames():");
    params = context.getInitParameterNames();
    while (params.hasMoreElements()) {
      String name = (String) params.nextElement();
      out.println("  " + name + ": " + context.getInitParameter(name));
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

  /**
   * Show the pieces of the request, for debugging
   *
   * @param req the HttpServletRequest
   * @return parsed request
   */
  public static String getRequestParsed(HttpServletRequest req) {
    return req.getRequestURI() + " = " + req.getContextPath() + "(context), " +
        req.getServletPath() + "(servletPath), " +
        req.getPathInfo() + "(pathInfo), " + req.getQueryString() + "(query)";
  }

  /**
   * This is the server part, eg http://motherlode:8080
   *
   * @param req the HttpServletRequest
   * @return request server
   */
  public static String getRequestServer(HttpServletRequest req) {
    return req.getScheme() + "://"+req.getServerName()+":"+ req.getServerPort();
  }

  /**
   * This is everything except the query string
   *
   * @param req the HttpServletRequest
   * @return parsed request base
   */
  public static String getRequestBase(HttpServletRequest req) {
    // return "http://"+req.getServerName()+":"+ req.getServerPort()+req.getRequestURI();
    return req.getRequestURL().toString();
  }

  /**
   * The request base as a URI
   * @param req the HttpServletRequest
   * @return parsed request as a URI
   */
  public static URI getRequestURI(HttpServletRequest req) {
    try {
      return new URI(getRequestBase(req));
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * servletPath + pathInfo
   * @param req the HttpServletRequest
   * @return parsed request servletPath + pathInfo
   */
  public static String getRequestPath(HttpServletRequest req) {
    StringBuffer buff = new StringBuffer();
    if (req.getServletPath() != null)
      buff.append(req.getServletPath());
    if (req.getPathInfo() != null)
      buff.append(req.getPathInfo());
    return buff.toString();
  }

  /**
   * The entire request including query string
   * @param req the HttpServletRequest
   * @return entire parsed request
   */
  public static String getRequest(HttpServletRequest req) {
    String query = req.getQueryString();
    return getRequestBase(req) + (query == null ? "" : "?" + query);
  }

  /**
   * Return the value of the given parameter for the given request. Should
   * only be used if the parameter is known to only have one value. If used
   * on a multi-valued parameter, the first value is returned.
   *
   * @param req the HttpServletRequest
   * @param paramName the name of the parameter to find.
   * @return the value of the given parameter for the given request.
   */
  public static String getParameterIgnoreCase(HttpServletRequest req, String paramName) {
    Enumeration e = req.getParameterNames();
    while (e.hasMoreElements()) {
      String s = (String) e.nextElement();
      if (s.equalsIgnoreCase(paramName))
        return req.getParameter(s);
    }
    return null;
  }

  /**
   * Return the values of the given parameter (ignoring case) for the given request.
   *
   * @param req the HttpServletRequest
   * @param paramName the name of the parameter to find.
   * @return the values of the given parameter for the given request.
   */
  public static String[] getParameterValuesIgnoreCase(HttpServletRequest req, String paramName) {
    Enumeration e = req.getParameterNames();
    while (e.hasMoreElements()) {
      String s = (String) e.nextElement();
      if (s.equalsIgnoreCase(paramName))
        return req.getParameterValues(s);
    }
    return null;
  }


  public static String getFileURL(String filename) {
    filename = filename.replace('\\', '/');
    filename = StringUtil.replace(filename, ' ', "+");
    return "file:" + filename;
  }

  /**
   * Show details about the request
   *
   * @param servlet used to get teh servlet context, may be null
   * @param req     the request
   * @return string showing the details of the request.
   */
  static public String showRequestDetail(HttpServlet servlet, HttpServletRequest req) {
    StringBuilder sbuff = new StringBuilder();

    sbuff.append("Request Info\n");
    sbuff.append(" req.getServerName(): ").append(req.getServerName()).append("\n");
    sbuff.append(" req.getServerPort(): ").append(req.getServerPort()).append("\n");
    sbuff.append(" req.getContextPath:").append(req.getContextPath()).append("\n");
    sbuff.append(" req.getServletPath:").append(req.getServletPath()).append("\n");
    sbuff.append(" req.getPathInfo:").append(req.getPathInfo()).append("\n");
    sbuff.append(" req.getQueryString:").append(req.getQueryString()).append("\n");
    sbuff.append(" getQueryStringDecoded:").append(EscapeStrings.urlDecode(req.getQueryString())).append("\n");
    /*try {
      sbuff.append(" getQueryStringDecoded:").append(URLDecoder.decode(req.getQueryString(), "UTF-8")).append("\n");
    } catch (UnsupportedEncodingException e1) {
      e1.printStackTrace();
    }*/
    sbuff.append(" req.getRequestURI:").append(req.getRequestURI()).append("\n");
    sbuff.append(" getRequestBase:").append(getRequestBase(req)).append("\n");
    sbuff.append(" getRequestServer:").append(getRequestServer(req)).append("\n");
    sbuff.append(" getRequest:").append(getRequest(req)).append("\n");
    sbuff.append("\n");

    sbuff.append(" req.getPathTranslated:").append(req.getPathTranslated()).append("\n");
    String path = req.getPathTranslated();
    if ((path != null) && (servlet != null)) {
      ServletContext context = servlet.getServletContext();
      sbuff.append(" getMimeType:").append(context.getMimeType(path)).append("\n");
    }
    sbuff.append("\n");
    sbuff.append(" req.getScheme:").append(req.getScheme()).append("\n");
    sbuff.append(" req.getProtocol:").append(req.getProtocol()).append("\n");
    sbuff.append(" req.getMethod:").append(req.getMethod()).append("\n");
    sbuff.append("\n");
    sbuff.append(" req.getContentType:").append(req.getContentType()).append("\n");
    sbuff.append(" req.getContentLength:").append(req.getContentLength()).append("\n");

    sbuff.append(" req.getRemoteAddr():").append(req.getRemoteAddr());
    try {
      sbuff.append(" getRemoteHost():").append(java.net.InetAddress.getByName(req.getRemoteHost()).getHostName()).append("\n");
    } catch (java.net.UnknownHostException e) {
      sbuff.append(" getRemoteHost():").append(e.getMessage()).append("\n");
    }
    sbuff.append(" getRemoteUser():").append(req.getRemoteUser()).append("\n");

    sbuff.append("\n");
    sbuff.append("Request Parameters:\n");
    Enumeration params = req.getParameterNames();
    while (params.hasMoreElements()) {
      String name = (String) params.nextElement();
      String values[] = req.getParameterValues(name);
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          sbuff.append("  ").append(name).append("  (").append(i).append("): ").append(values[i]).append("\n");
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
          sbuff.append("  ").append(name).append(": ").append(value).append("\n");
        }
      }
    }
    sbuff.append(" ------------------\n");

    return sbuff.toString();
  }

  static public String showRequestHeaders(HttpServletRequest req) {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("Request Headers:\n");
    Enumeration names = req.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      Enumeration values = req.getHeaders(name);  // support multiple values
      if (values != null) {
        while (values.hasMoreElements()) {
          String value = (String) values.nextElement();
          sbuff.append("  ").append(name).append(": ").append(value).append("\n");
        }
      }
    }
    return sbuff.toString();
  }

  static public void showSession(HttpServletRequest req, HttpServletResponse res,
                                 PrintStream out) {

    // res.setContentType("text/html");

    // Get the current session object, create one if necessary
    HttpSession session = req.getSession();

    // Increment the hit count for this page. The value is saved
    // in this client's session under the name "snoop.count".
    Integer count = (Integer) session.getAttribute("snoop.count");
    if (count == null) {
      count = 1;
    } else
      count = count + 1;
    session.setAttribute("snoop.count", count);

    out.println(HtmlWriter.getInstance().getHtmlDoctypeAndOpenTag());
    out.println("<HEAD><TITLE>SessionSnoop</TITLE></HEAD>");
    out.println("<BODY><H1>Session Snoop</H1>");

    // Display the hit count for this page
    out.println("You've visited this page " + count +
        ((!(count.intValue() != 1)) ? " time." : " times."));

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


  static public void showSession(HttpServletRequest req, PrintStream out) {

    // res.setContentType("text/html");

    // Get the current session object, create one if necessary
    HttpSession session = req.getSession();

    out.println("Session id: " + session.getId());
    out.println(" session.isNew(): " + session.isNew());
    out.println(" session.getMaxInactiveInterval(): " + session.getMaxInactiveInterval() + " secs");
    out.println(" session.getCreationTime(): " + session.getCreationTime() + " (" + new Date(session.getCreationTime()) + ")");
    out.println(" session.getLastAccessedTime(): " + session.getLastAccessedTime() + " (" + new Date(session.getLastAccessedTime()) + ")");
    out.println(" req.isRequestedSessionIdFromCookie: " + req.isRequestedSessionIdFromCookie());
    out.println(" req.isRequestedSessionIdFromURL: " + req.isRequestedSessionIdFromURL());
    out.println(" req.isRequestedSessionIdValid: " + req.isRequestedSessionIdValid());

    out.println("Saved session Attributes:");
    Enumeration atts = session.getAttributeNames();
    while (atts.hasMoreElements()) {
      String name = (String) atts.nextElement();
      out.println(" " + name + ": " + session.getAttribute(name) + "<BR>");
    }

  }

  static public String showSecurity(HttpServletRequest req, String role) {
    StringBuilder sbuff = new StringBuilder();

    sbuff.append("Security Info\n");
    sbuff.append(" req.getRemoteUser(): ").append(req.getRemoteUser()).append("\n");
    sbuff.append(" req.getUserPrincipal(): ").append(req.getUserPrincipal()).append("\n");
    sbuff.append(" req.isUserInRole(").append(role).append("):").append(req.isUserInRole(role)).append("\n");
    sbuff.append(" ------------------\n");

    return sbuff.toString();
  }

  /* from luca / ageci code, portResolver, portMapper not known
    static public void getSecureRedirect(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        String queryString = req.getQueryString();
        String contextPath = req.getContextPath();
        String destination = req.getServletPath() + ((pathInfo ==   null) ? "" : pathInfo)
            + ((queryString == null) ? "" : ("?" + queryString));
        String redirectUrl = contextPath;

        Integer httpPort = new Integer(portResolver.getServerPort(req));
        Integer httpsPort = portMapper.lookupHttpsPort(httpPort);
        if (httpsPort != null) {
            boolean includePort = true;
            if (httpsPort.intValue() == 443) {
                includePort = false;
            }
            redirectUrl = "https://" + req.getServerName() +   ((includePort) ? (":" + httpsPort) : "") + contextPath
                + destination;
        }
    } */

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
    showThreads(pw, group, current);
  }

  static private void showThreads(PrintStream pw, ThreadGroup g, Thread current) {
    int nthreads = g.activeCount();
    pw.println("\nThread Group = " + g.getName() + " activeCount= " + nthreads);
    Thread[] tarray = new Thread[nthreads];
    int n = g.enumerate(tarray, false);

    for (int i = 0; i < n; i++) {
      Thread thread = tarray[i];
      ClassLoader loader = thread.getContextClassLoader();
      String loaderName = (loader == null) ? "Default" : loader.getClass().getName();
      Thread.State state = thread.getState();
      long id = thread.getId();
      pw.print("   " + id + " " + thread.getName() + " " + state + " " + loaderName);
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

}