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

package thredds.dqc.server;

import thredds.servlet.ServletUtil;
import thredds.servlet.UsageLog;
import thredds.util.RequestForwardUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet for redirecting /dqcServlet/dqc/* and /dqcServlet/dqcServlet/*
 * requests to /thredds/dqc/*
 *
 */

public class DqcServletRedirect extends HttpServlet
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( DqcServletRedirect.class);

//  private String servletName = "dqcServlet";

  private String targetContextPath = "/thredds";
  private String targetServletPath = "/dqc";

  private String testTargetContextPath = "/dqcServlet";
//  private String testTargetServletPath = "/dqc";

  private String testRedirectPath = "/redirect-test";
  private String testRedirectStopPath = "/redirect-stop-test";

  @Override
  public void init() throws ServletException
  {
    this.getServletConfig();
    ServletUtil.setContextPath( testTargetContextPath );
    ServletUtil.initContext( this.getServletContext() );
  }

  /**
   * Redirect all GET requests.
   *
   *
   * @param req - the HttpServletRequest
   * @param res - the HttpServletResponse
   * @throws javax.servlet.ServletException if the request could not be handled for some reason.
   * @throws java.io.IOException if an I/O error is detected (when communicating with client not for servlet internal IO problems?).
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    log.info( "doGet(): " + UsageLog.setupRequestContext( req ) );
    boolean enableTestRedirect = false;

    // Get the request path and query information.
    String reqPath = req.getPathInfo();

    if ( reqPath == null )
      doDispatch( req, res, false );

    else if ( reqPath.equals( testRedirectPath + "/index.html"))
      ServletUtil.handleRequestForRawFile( "index.html", this, req, res );
    else if ( reqPath.equals( testRedirectPath + "/301.html"))
      handleGetRequestForRedirectTest( res, req, StatusCode.SC_301, BodyType.HTML);
    else if ( reqPath.equals( testRedirectPath + "/302.html"))
      handleGetRequestForRedirectTest( res, req, StatusCode.SC_302, BodyType.HTML);
    else if ( reqPath.equals( testRedirectPath + "/305.html"))
      handleGetRequestForRedirectTest( res, req, StatusCode.SC_305, BodyType.HTML);
    else if ( reqPath.equals( testRedirectPath + "/307.html"))
      handleGetRequestForRedirectTest( res, req, StatusCode.SC_307, BodyType.HTML);

    else if ( reqPath.equals( testRedirectPath + "/301.xml"))
      handleGetRequestForRedirectTest( res, req, StatusCode.SC_301, BodyType.XML);
    else if ( reqPath.equals( testRedirectPath + "/302.xml"))
      handleGetRequestForRedirectTest( res, req, StatusCode.SC_302, BodyType.XML);
    else if ( reqPath.equals( testRedirectPath + "/305.xml"))
      handleGetRequestForRedirectTest( res, req, StatusCode.SC_305, BodyType.XML);
    else if ( reqPath.equals( testRedirectPath + "/307.xml"))
      handleGetRequestForRedirectTest( res, req, StatusCode.SC_307, BodyType.XML);

    else if ( reqPath.startsWith( testRedirectPath + "/301.dods.nc" ) )
      handleGetRequestForRedirectTest( res, req, StatusCode.SC_301, BodyType.DAP );
    else if ( reqPath.startsWith( testRedirectPath + "/302.dods.nc" ) )
      handleGetRequestForRedirectTest( res, req, StatusCode.SC_302, BodyType.DAP );
    else if ( reqPath.startsWith( testRedirectPath + "/305.dods.nc" ) )
      handleGetRequestForRedirectTest( res, req, StatusCode.SC_305, BodyType.DAP );
    else if ( reqPath.startsWith( testRedirectPath + "/307.dods.nc" ) )
      handleGetRequestForRedirectTest( res, req, StatusCode.SC_307, BodyType.DAP );

    else if ( reqPath.startsWith( testRedirectStopPath ) && enableTestRedirect )
      this.handleGetRequestForRedirectStopTest( res, req );
    else
      doDispatch( req, res, false );

    return;
  }
  enum BodyType{ HTML, XML, DAP }
  enum StatusCode {
    SC_200( "200", "OK"),
    SC_301( "301", "Moved Permanently"),
    SC_302( "302", "Found"),
    SC_305( "305", "Use Proxy"),
    SC_307( "307", "Temporary Redirect");

    private String scId;
    private String scMsg;

    StatusCode( String id, String msg) { scId = id; scMsg = msg; }

    public String toString() { return scId; }
    public String getMessage() { return scMsg; }
  }

  private void handleGetRequestForRedirectTest( HttpServletResponse res, HttpServletRequest req,
                                                StatusCode sc, BodyType desiredBodyType )
          throws IOException
  {
    String urlSuffix = null;
    BodyType respBodyType = null;
    if ( desiredBodyType.equals( BodyType.HTML )) {
      urlSuffix = ".html";
      respBodyType = BodyType.HTML;
    }
    else if ( desiredBodyType.equals( BodyType.XML )) {
      urlSuffix = ".xml";
      respBodyType = BodyType.XML;
    }
    else if ( desiredBodyType.equals( BodyType.DAP )) {
      urlSuffix = ".dods.nc";
      respBodyType = BodyType.HTML;
    }

    String requestUrlString = req.getRequestURL().toString();
    String reqPath = req.getPathInfo();
    String expectedPath = testRedirectPath + "/" + sc.toString() + urlSuffix;
    if ( ! reqPath.startsWith( expectedPath ))
      throw new IllegalStateException( "Request [" + reqPath + "] not as expected [" + expectedPath + "]." );
    if ( ( desiredBodyType.equals( BodyType.HTML ) || desiredBodyType.equals( BodyType.XML ) ) &&  ! reqPath.equals( expectedPath ))
      throw new IllegalStateException( "Request [" + reqPath + "] not as expected [" + expectedPath + "].");

    String queryString = req.getQueryString();
    String targetUrlString = null;
    if ( desiredBodyType.equals( BodyType.DAP )) {
      String remainingPath = reqPath.substring( expectedPath.length() );
      targetUrlString = "http://motherlode.ucar.edu:8080/thredds/dodsC/public/dataset/testData.nc" + remainingPath + (queryString != null ? ("?" + queryString) : "");
//      targetUrlString = "/thredds/dodsC/public/dataset/testData.nc" + remainingPath + "?" + queryString;
//      URI targetUrl = null;
//      try {
//        targetUrl = new URI( requestUrlString ).resolve( targetUrlString);
//      }
//      catch ( URISyntaxException e ) {
//        throw new IllegalStateException( "Bad URL [" + requestUrlString + "].");
//      }
    } else {
      StringBuffer targetUrlStringBuffer = new StringBuffer( requestUrlString );
      int start = targetUrlStringBuffer.indexOf( testRedirectPath );
      targetUrlStringBuffer.replace( start, start + testRedirectPath.length(), testRedirectStopPath );
      if ( queryString != null )
        targetUrlStringBuffer.append( "?" ).append( queryString );
      targetUrlString = targetUrlStringBuffer.toString();
    }

    String title = "Redirection: " + sc.toString() + "(" + sc.getMessage() + ")" + " to " + targetUrlString;
    StringBuilder response = null;
    if ( desiredBodyType.equals( BodyType.HTML) || desiredBodyType.equals( BodyType.DAP) )
    {
      StringBuilder htmlBody = new StringBuilder()
              .append( "<p>" )
              .append( "The requested URL [" ).append( requestUrlString )
              .append( "] has been redirected [").append( sc.toString()).append( " (").append( sc.getMessage()).append( ")]." )
              .append( " Instead, please use the following URL: <a href='" ).append( targetUrlString ).append( "'>" )
              .append( targetUrlString ).append( "</a>." )
              .append( "</p>" );
      response = generateHtmlResponse( title, htmlBody.toString() );
    } else if ( desiredBodyType.equals( BodyType.XML))
      response = generateCatalogResponse( title, targetUrlString, title );

    sendResponse( res, targetUrlString, sc, response.toString(), respBodyType );
    return;
  }

  private void handleGetRequestForRedirectStopTest( HttpServletResponse res, HttpServletRequest req )
          throws IOException
  {
    String requestUrlString = req.getRequestURL().toString();

    String reqPath = req.getPathInfo();
    String queryString = req.getQueryString();

    log.debug( "handleGetRequestForRedirectStopTest(): handle GET path \"" + reqPath + "\") with query \"" + queryString + "\">." );
    String title = "Redirected: " + requestUrlString;
    StringBuilder response = null;
    BodyType responseBodyType = null;
    if ( reqPath.endsWith( ".xml") ) {
      responseBodyType = BodyType.XML;
      response = generateCatalogResponse( title, "/thredds/catalog.xml", "main catalog" );
    }
    else // if ( reqPath.endsWith( ".html"))
    {
      responseBodyType = BodyType.HTML;
      StringBuilder body = new StringBuilder()
              .append( "<p>" )
              .append( requestUrlString )
              .append( "</p>" );
      response = generateHtmlResponse( title, body.toString() );
    }

    sendResponse( res, null, StatusCode.SC_200, response.toString(), responseBodyType );
    log.info( "handleGetRequestForRedirectStopTest(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, response.length() ) );
    return;
  }

  private void sendResponse( HttpServletResponse response,
                                        String targetUrlString, StatusCode sc,
                                        String body, BodyType bodyType )
          throws IOException
  {
    if ( sc.equals( StatusCode.SC_200)) {
      response.setStatus( HttpServletResponse.SC_OK );
    }
    if ( sc.equals( StatusCode.SC_301)) {
      response.setStatus( HttpServletResponse.SC_MOVED_PERMANENTLY );
      response.addHeader( "Location", targetUrlString );
    } else if ( sc.equals( StatusCode.SC_305 ) ) {
      response.setStatus( HttpServletResponse.SC_USE_PROXY );
      response.addHeader( "Location", targetUrlString );
    }
    else if ( sc.equals( StatusCode.SC_307 ) ) {
      response.setStatus( HttpServletResponse.SC_TEMPORARY_REDIRECT );
      response.addHeader( "Location", targetUrlString );
    }

    PrintWriter out = response.getWriter();
    if ( bodyType.equals( BodyType.XML ))
      response.setContentType( "application/xml" );
    else if ( bodyType.equals( BodyType.HTML ) )
      response.setContentType( "text/html" );

    out.print( body );

    if ( sc.equals( StatusCode.SC_302 ) )
      response.sendRedirect( targetUrlString );

    return;
  }

  public void doPut( HttpServletRequest req, HttpServletResponse res )
          throws IOException, ServletException
  {
    log.info( "doPut(): " + UsageLog.setupRequestContext( req ) );

    doDispatch( req, res, false );

    return;
  }

  /**
   * Dispatch the request to the target context and servlet.
   *
   * @param req the HttpServletRequest
   * @param res the HttpServletResponse
   * @param useTestContext j
   * @throws IOException if IO problems
   * @throws ServletException if any internal errors
   */
  private void doDispatch( HttpServletRequest req, HttpServletResponse res, boolean useTestContext )
          throws IOException, ServletException
  {
    // Determine the request URI path.
    String requestURIPath = new StringBuffer()
            .append( req.getContextPath() )
            .append( req.getServletPath() )
            .append( req.getPathInfo() )
            .toString();

    String targetURIPath = convertRequestURLToResponseURL( requestURIPath, req, useTestContext );
    String targetURIPathNoContext = targetURIPath.substring( useTestContext
                                                             ? this.testTargetContextPath.length()
                                                             : this.targetContextPath.length() );

    String queryString = req. getQueryString();
    String reqURL = requestURIPath;
    String targetURL = targetURIPath;
    if ( queryString != null ) reqURL = reqURL + "?" + queryString;
    if ( queryString != null ) targetURL = targetURL + "?" + queryString;

    log.info( "doDispatch(): " + req.getRemoteHost()
              + " - dispatching request for URL \"" + reqURL
              + "\" to \"" + targetURL + "\"." );

    // Dispatch to the target URL.
    ServletContext context = this.getServletContext();
    ServletContext targetContext = context.getContext( useTestContext ? this.testTargetContextPath : this.targetContextPath );
    if ( targetContext == null )
    {
      String tmpMsg = "Null ServletContext for \"" + ( useTestContext ? this.testTargetContextPath : this.targetContextPath ) + "\"";
      log.warn( "doDispatch(): " + tmpMsg + ": "
                + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, tmpMsg.length() ) );
      res.sendError( HttpServletResponse.SC_NOT_FOUND, tmpMsg );
      return;
    }

    RequestForwardUtils.forwardRequestRelativeToGivenContext( targetURIPathNoContext,
                                                              targetContext, req, res );
  }

  private String convertRequestURLToResponseURL( String reqURL, HttpServletRequest req, boolean useTestContext )
  {
    StringBuffer reqURLBuffer = new StringBuffer( reqURL );
    String strToReplace = useTestContext
                          ? ( req.getPathInfo().length() > this.testRedirectPath.length()
                              ? this.testRedirectPath
                              : req.getPathInfo() )
                          : req.getContextPath() + req.getServletPath();

    int strToReplaceStart = reqURL.indexOf( strToReplace );
    int strToReplaceEnd = strToReplaceStart + strToReplace.length();
    reqURLBuffer.replace( strToReplaceStart, strToReplaceEnd,
                          useTestContext
                          ? this.testRedirectStopPath
                          : targetContextPath + targetServletPath );
    return reqURLBuffer.toString();
  }

  private StringBuilder generateCatalogResponse( String title, String catLink, String catName )
  {
    StringBuilder response = new StringBuilder()
            .append( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( "<catalog xmlns='http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0'\n" )
            .append( "         xmlns:xlink='http://www.w3.org/1999/xlink' name='" ).append( title ).append( "' version='1.0.3'>\n" )
            .append( "  <dataset name='" ).append( title ).append( "'>\n" )
            .append( "    <catalogRef xlink:href='" ).append( catLink )
            .append( "' xlink:title='" ).append( catName ).append( "' name='' />\n" )
            .append( "  </dataset>\n" )
            .append( "</catalog>\n" );
    return response;
  }

  private StringBuilder generateHtmlResponse( String title, String body )
  {
    StringBuilder response = new StringBuilder()
            .append( getHtmlDoctypeAndOpenTag() )
            .append( "<head><title>" ).append( title ).append( "</title></head>" )
            .append( "<body>" )
            .append( "<h1>" ).append( title ).append( "</h1>" )
            .append( body )
            .append( "</body></html>" );
    return response;
  }

  private String getHtmlDoctypeAndOpenTag()
  {
    return new StringBuilder()
            .append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" )
            .append( "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" )
            .append( "<html>\n" )
            .toString();
  }
}