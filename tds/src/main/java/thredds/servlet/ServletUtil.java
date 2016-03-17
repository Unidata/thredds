/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.servlet;

import java.io.*;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.*;
import javax.servlet.http.*;

import thredds.core.ConfigCatalogHtmlWriter;
import thredds.util.ContentType;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.IO;
import thredds.util.RequestForwardUtils;
import ucar.nc2.util.EscapeStrings;
import ucar.unidata.io.RandomAccessFile;

public class ServletUtil {
  public static final org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServletUtil.class);

  /**
   * Return the file path dealing with leading and trailing path
   * seperators (which must be a slash ("/")) for the given directory
   * and file paths.
   * <p/>
   * Note: Dealing with path strings is fragile.
   * ToDo: Switch from using path strings to java.io.Files.
   *
   * @param dirPath  the directory path.
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
   * Write a file to the response stream.
   *
   * @param servlet     called from this servlet, may be null
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
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // dontallow ..
    if (filename.contains("..")) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    // dont allow access to WEB-INF or META-INF
    String upper = filename.toUpperCase();
    if (upper.contains("WEB-INF") || upper.contains("META-INF")) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    returnFile(servlet, req, res, new File(filename), contentType);
  }

  /**
   * Write a file to the response stream. Handles Range requests.
   *
   * @param servlet     called from this servlet, may be null
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
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // check that it exists
    if (!file.exists()) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // not a directory
    if (!file.isFile()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // Set the type of the file
    String filename = file.getPath();
    if (null == contentType) {
      if (filename.endsWith(".html"))
        contentType = ContentType.html.getContentHeader();
      else if (filename.endsWith(".xml"))
        contentType = ContentType.xml.getContentHeader();
      else if (filename.endsWith(".txt") || (filename.endsWith(".log")))
        contentType = ContentType.text.getContentHeader();
      else if (filename.indexOf(".log.") > 0)
        contentType = ContentType.text.getContentHeader();
      else if (filename.endsWith(".nc"))
        contentType = ContentType.netcdf.getContentHeader();
      else if (filename.endsWith(".nc4"))
        contentType = ContentType.netcdf.getContentHeader();
      else if (servlet != null)
        contentType = servlet.getServletContext().getMimeType(filename);

      if (contentType == null) contentType = ContentType.binary.getContentHeader();
    }

    returnFile(req, res, file, contentType);
  }

  /**
   * Write a file to the response stream. Handles Range requests.
   *
   * @param req         request
   * @param res         response
   * @param file        must exist and not be a directory
   * @param contentType must not be null
   * @throws IOException or error
   */
  public static void returnFile(HttpServletRequest req, HttpServletResponse res, File file, String contentType) throws IOException {
    res.setContentType(contentType);
    // res.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");

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

    // when compression is turned on, ContentLength has to be overridden
    // this is also true for HEAD, since this must be the same as GET without the body
    if (contentLength > Integer.MAX_VALUE)
      res.addHeader("Content-Length", Long.toString(contentLength));  // allow content length > MAX_INT
    else
      res.setContentLength((int) contentLength);

    String filename = file.getPath();
    // indicate we allow Range Requests
    res.addHeader("Accept-Ranges", "bytes");

    if (req.getMethod().equals("HEAD")) {
      return;
    }

    try {

      if (isRangeRequest) {
        // set before content is sent
        res.addHeader("Content-Range", "bytes " + startPos + "-" + (endPos - 1) + "/" + fileSize);
        res.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        try (RandomAccessFile craf = RandomAccessFile.acquire(filename)) {
          IO.copyRafB(craf, startPos, contentLength, res.getOutputStream(), new byte[60000]);
          return;
        }
      }

      // Return the file
      ServletOutputStream out = res.getOutputStream();
      IO.copyFileB(file, out, 60 * 1000);
      /* try (WritableByteChannel cOut = Channels.newChannel(out)) {
        IO.copyFileWithChannels(file, cOut);
        res.flushBuffer();
      } */
    }

    // @todo Split up this exception handling: those from file access vs those from dealing with response
    //       File access: catch and res.sendError()
    //       response: don't catch (let bubble up out of doGet() etc)
    catch (FileNotFoundException e) {
      log.error("returnFile(): FileNotFoundException= " + filename);
      if (!res.isCommitted()) res.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (java.net.SocketException e) {
      log.info("returnFile(): SocketException sending file: " + filename + " " + e.getMessage());
    } catch (IOException e) {
      String eName = e.getClass().getName(); // dont want compile time dependency on ClientAbortException
      if (eName.equals("org.apache.catalina.connector.ClientAbortException")) {
        log.debug("returnFile(): ClientAbortException while sending file: " + filename + " " + e.getMessage());
        return;
      }

      if (e.getMessage().startsWith("File transfer not complete")) { // coming from FileTransfer.transferTo()
        log.debug("returnFile() "+e.getMessage());
        return;
      }

      log.error("returnFile(): IOException (" + e.getClass().getName() + ") sending file ", e);
      if (!res.isCommitted()) res.sendError(HttpServletResponse.SC_NOT_FOUND, "Problem sending file: " + e.getMessage());
    }
  }

  /**
   * Send given content string as the HTTP response.
   *
   * @param contents the string to return as the HTTP response.
   * @param res      the HttpServletResponse
   * @throws IOException if an I/O error occurs while writing the response.
   */
  public static void returnString(String contents, HttpServletResponse res)
          throws IOException {

    try {
      ServletOutputStream out = res.getOutputStream();
      IO.copy(new ByteArrayInputStream(contents.getBytes(CDM.utf8Charset)), out);
    } catch (IOException e) {
      log.error(" IOException sending string: ", e);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, "Problem sending string: " + e.getMessage());
    }
  }

  /**
   * Set the proper content length for the string
   *
   * @param response the HttpServletResponse to act upon
   * @param s the string that will be returned
   * @return the number of bytes
   * @throws UnsupportedEncodingException on bad character encoding
   */
  public static int setResponseContentLength(HttpServletResponse response, String s) throws UnsupportedEncodingException {
    int length = s.getBytes(response.getCharacterEncoding()).length;
    response.setContentLength(length);
    return length;
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
   * @throws IOException      on IO error
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
    RequestForwardUtils.forwardRequestRelativeToCurrentContext("/catalog.html?" + reqs, req, res);
  }

  static public void showSystemProperties(PrintStream out) {

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

  /**
   * This is the server part, eg http://motherlode:8080
   *
   * @param req the HttpServletRequest
   * @return request server
   */
  public static String getRequestServer(HttpServletRequest req) {
    return req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();
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
   *
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
   *
   * @param req the HttpServletRequest
   * @return parsed request servletPath + pathInfo
   */
  public static String getRequestPath(HttpServletRequest req) {
    StringBuilder buff = new StringBuilder();
    if (req.getServletPath() != null)
      buff.append(req.getServletPath());
    if (req.getPathInfo() != null)
      buff.append(req.getPathInfo());
    return buff.toString();
  }

  /**
   * The entire request including query string
   *
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
   * @param req       the HttpServletRequest
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
   * Show details about the request
   *
   * @param req     the request
   * @return string showing the details of the request.
   */
  static public String showRequestDetail(HttpServletRequest req) {
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

    out.println(ConfigCatalogHtmlWriter.getHtmlDoctypeAndOpenTag());
    out.println("<HEAD><TITLE>SessionSnoop</TITLE></HEAD>");
    out.println("<BODY><H1>Session Snoop</H1>");

    // Display the hit count for this page
    out.println("You've visited this page " + count + ((count == 1) ? " time." : " times."));
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


  /* static public void showSession(HttpServletRequest req, PrintStream out) {

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

  } */

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

  /* static private String getServerInfoName(String serverInfo) {
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
  } */

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