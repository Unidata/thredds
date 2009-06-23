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
    {
      doDispatch( req, res, false );
    }
    else if ( reqPath.startsWith( testRedirectPath ) && enableTestRedirect )
    {
      this.handleGetRequestForRedirectTest( res, req );
    }
    else if ( reqPath.startsWith( testRedirectStopPath ) && enableTestRedirect )
    {
      this.handleGetRequestForRedirectStopTest( res, req );
    }
    else
    {
      doDispatch( req, res, false );
    }

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

//    // Determine the target URI path without the context.
//    String targetURIPathNoContext;
//    if ( useTestContext )
//    {
//      if ( req.getPathInfo().length() > this.testRedirectPath.length())
//      {
//        targetURIPathNoContext = new StringBuffer()
//                .append( this.testTargetServletPath )
//                .append( this.testRedirectStopPath )
//                .append( req.getPathInfo().substring( this.testRedirectPath.length() ) )
//                .toString();
//      }
//      else
//      {
//        targetURIPathNoContext = new StringBuffer()
//                .append( this.testTargetServletPath )
//                .append( this.testRedirectStopPath )
//                .toString();
//      }
//    }
//    else
//    {
//      targetURIPathNoContext = new StringBuffer()
//              .append( this.targetServletPath )
//              .append( req.getPathInfo() )
//              .toString();
//    }
//
//    // Determine the target URI path with the context.
//    String targetURIPath = new StringBuffer()
//            .append( useTestContext ? this.testTargetContextPath : this.targetContextPath )
//            .append( targetURIPathNoContext )
//            .toString();

    String queryString = req.getQueryString();
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
                                                              targetContext,
                                                              req, res );
  }

  private void handleGetRequestForRedirectTest( HttpServletResponse res, HttpServletRequest req )
          throws IOException, ServletException
  {
    String reqPath = req.getPathInfo();
    String queryString = req.getQueryString();

    log.debug( "handleGetRequestForRedirectTest(): handle GET path \"" + reqPath + "\") with query \"" + queryString + "\">." );
    if ( reqPath.equals( testRedirectPath ) )
    {
      if ( queryString == null )
      {
        log.warn( "handleGetRequestForRedirectTest(): request not understood <" + reqPath + ">." );
        log.info( "handleGetRequestForRedirectTest(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, 0 ) );
        res.setStatus( HttpServletResponse.SC_BAD_REQUEST );
      }
      else if ( queryString.equals( "301" ) )
        this.doRedirect301( req, res, true );
      else if ( queryString.equals( "302" ) )
        this.doRedirect302( req, res, true );
      else if ( queryString.equals( "305" ) )
        this.doUseProxy305( req, res, true );
      else if ( queryString.equals( "dispatch" ) )
        this.doDispatch( req, res, true );
      else
      {
        log.warn( "handleGetRequestForRedirectTest(): request not understood <" + reqPath + " -- " + queryString + ">." );
        log.info( "handleGetRequestForRedirectTest(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, 0 ) );
        res.setStatus( HttpServletResponse.SC_BAD_REQUEST );
      }
    }
    else if ( reqPath.equals( testRedirectPath + "/" ) )
    {
      log.warn( "handleGetRequestForRedirectTest(): request not understood <" + reqPath + " -- " + queryString + ">." );
      log.info( "handleGetRequestForRedirectTest(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, 0 ) );
      res.setStatus( HttpServletResponse.SC_BAD_REQUEST );
    }
    else if ( reqPath.equals( testRedirectPath + "/301" ) && queryString == null )
      this.doRedirect301( req, res, true );
    else if ( reqPath.equals( testRedirectPath + "/302" ) && queryString == null )
      this.doRedirect302( req, res, true );
    else if ( reqPath.equals( testRedirectPath + "/305" ) && queryString == null )
      this.doUseProxy305( req, res, true );
    else if ( reqPath.equals( testRedirectPath + "/dispatch" ) && queryString == null )
      this.doDispatch( req, res, true );
    else
    {
      log.warn( "handleGetRequestForRedirectTest(): request not understood <" + reqPath + " -- " + queryString + ">." );
      log.info( "handleGetRequestForRedirectTest(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, 0 ) );
      res.setStatus( HttpServletResponse.SC_BAD_REQUEST );
    }
    return;
  }

  private void handleGetRequestForRedirectStopTest( HttpServletResponse res, HttpServletRequest req )
          throws IOException
  {
    String reqPath = req.getPathInfo();
    String queryString = req.getQueryString();

    log.debug( "handleGetRequestForRedirectStopTest(): handle GET path \"" + reqPath + "\") with query \"" + queryString + "\">." );

    String title = "The Resource";
    String htmlResp = new StringBuffer()
            .append( getHtmlDoctypeAndOpenTag() )
            .append( "<head><title>" )
            .append( title )
            .append( "</title></head><body>" )
            .append( "<h1>" ).append( title ).append( "</h1>" )
            .append( "<ul>" )
            .append( "<li>" ).append( "Path : " ).append( reqPath ).append( "</li>" )
            .append( "<li>" ).append( "Query: " ).append( queryString ).append( "</li>" )
            .append( "</ul>" )
            .append( "</body></html>" )
            .toString();
    // Write the catalog out.
    PrintWriter out = res.getWriter();
    res.setContentType( "text/html" );
    res.setStatus( HttpServletResponse.SC_OK );
    out.print( htmlResp );
    log.info( "handleGetRequestForRedirectStopTest(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, htmlResp.length() ));

    return;
  }

  private void doRedirect301( HttpServletRequest req, HttpServletResponse res, boolean useTestContext )
          throws IOException
  {
    String requestURIPath = new StringBuffer()
            .append( req.getContextPath() )
            .append( req.getServletPath() )
            .append( req.getPathInfo() )
            .toString();
    String targetURIPath = convertRequestURLToResponseURL( requestURIPath, req, useTestContext );
//    String targetURIPathNoContext = targetURIPath.substring( useTestContext
//                                                             ? this.testTargetContextPath.length()
//                                                             : this.targetContextPath.length() );
//    String targetURIPathNoContext = new StringBuffer()
//            .append( useTestContext ? this.testTargetServletPath : this.targetServletPath )
//            .append( this.testRedirectStopPath )
//            .toString();
//    String targetURIPath = new StringBuffer()
//            .append( useTestContext ? this.testTargetContextPath : this.targetContextPath )
//            .append( targetURIPathNoContext )
//            .toString();

    String queryString = req.getQueryString();
    if ( queryString != null ) targetURIPath = targetURIPath + "?" + queryString;

    targetURIPath = res.encodeRedirectURL( targetURIPath );
    log.info( "doRedirect301(): " + req.getRemoteHost() + " - requested URL \"" + requestURIPath
               + "\" permanently moved, redirect to \"" + targetURIPath + "\"." );
    res.setStatus( HttpServletResponse.SC_MOVED_PERMANENTLY );
    res.addHeader( "Location", targetURIPath );

    String title = "Permanently Moved - 301";
    String body = new StringBuffer()
            .append( "<p>" )
            .append( "The requested URL <" ).append( req.getRequestURL() )
            .append( "> has been permanently moved (HTTP status code 301)." )
            .append( " Instead, please use the following URL: <a href=\"" ).append( targetURIPath ).append( "\">" ).append( targetURIPath ).append( "</a>." )
            .append( "</p>" )
            .toString();
    String htmlResp = new StringBuffer()
            .append( getHtmlDoctypeAndOpenTag() )
            .append( "<head><title>" )
            .append( title )
            .append( "</title></head><body>" )
            .append( "<h1>" ).append( title ).append( "</h1>" )
            .append( body )
            .append( "</body></html>" )
            .toString();
    // Write the catalog out.
    PrintWriter out = res.getWriter();
    res.setContentType( "text/html" );
    out.print( htmlResp );

    log.info( "doRedirect301(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_MOVED_PERMANENTLY, 0 ));
    return;
  }

  private void doRedirect302( HttpServletRequest req, HttpServletResponse res, boolean useTestContext )
          throws IOException
  {
    String requestURIPath = new StringBuffer()
            .append( req.getContextPath() )
            .append( req.getServletPath() )
            .append( req.getPathInfo() )
            .toString();
    String targetURIPath = convertRequestURLToResponseURL( requestURIPath, req, useTestContext );
//    String targetURIPathNoContext = targetURIPath.substring( useTestContext
//                                                             ? this.testTargetContextPath.length()
//                                                             : this.targetContextPath.length() );

//    String targetURIPathNoContext = new StringBuffer()
//            .append( useTestContext ? this.testTargetServletPath : this.targetServletPath )
//            .append( this.testRedirectStopPath )
//            .toString();
//    String targetURIPath = new StringBuffer()
//            .append( useTestContext ? this.testTargetContextPath : this.targetContextPath )
//            .append( targetURIPathNoContext )
//            .toString();

    String queryString = req.getQueryString();
    if ( queryString != null ) targetURIPath = targetURIPath + "?" + queryString;

    targetURIPath = res.encodeRedirectURL( targetURIPath );

    log.info( "doRedirect302(): " + req.getRemoteHost() + " - requested URL \"" + requestURIPath
               + "\" temporarily moved, redirect to \"" + targetURIPath + "\"." );

    String title = "Temporarily Moved - 302";
    String body = new StringBuffer()
            .append( "<p>" )
            .append( "The requested URL <" ).append( req.getRequestURL() )
            .append( "> has been temporarily moved (HTTP status code 302)." )
            .append( " Instead, please use the following URL: <a href=\"" ).append( targetURIPath ).append( "\">" ).append( targetURIPath ).append( "</a>." )
            .append( "</p>" )
            .toString();
    String htmlResp = new StringBuffer()
            .append( getHtmlDoctypeAndOpenTag() )
            .append( "<head><title>" )
            .append( title )
            .append( "</title></head><body>" )
            .append( "<h1>" ).append( title ).append( "</h1>" )
            .append( body )
            .append( "</body></html>" )
            .toString();
    // Write the catalog out.
    PrintWriter out = res.getWriter();
    res.setContentType( "text/html" );
    out.print( htmlResp );

    res.sendRedirect( targetURIPath );
    //res.setStatus( HttpServletResponse.SC_MOVED_TEMPORARILY );
    //res.addHeader( "Location", targetURIPath );

    log.info( "doRedirect302(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_MOVED_TEMPORARILY, 0 ));
    return;
  }

  private void doUseProxy305( HttpServletRequest req, HttpServletResponse res, boolean useTestContext )
          throws IOException
  {
    String reqURL = req.getRequestURL().toString();

    String targetURL = res.encodeRedirectURL(
            convertRequestURLToResponseURL( reqURL, req, useTestContext ) );

    String queryString = req.getQueryString();
    if ( queryString != null ) reqURL = reqURL + "?" + queryString;
    if ( queryString != null ) targetURL = targetURL + "?" + queryString;
    log.info( "doUseProxy305(): " + req.getRemoteHost() + " - proxy requested URI \"" + reqURL
               + "\" to \"" + targetURL + "\"." );
    res.addHeader( "Location", targetURL );

    String title = "Use Proxy - 305";
    String body = new StringBuffer()
            .append( "<ul>" )
            .append( "<li>" ).append( "request URL : " ).append( req.getRequestURL() ).append( "</li>" )
            .append( "<li>" ).append( "proxy URL   : <a href=\"" ).append( targetURL ).append( "\">" ).append( targetURL ).append( "</a></li>" )
            .append( "</ul>" )
            .toString();
    String htmlResp = new StringBuffer()
            .append( getHtmlDoctypeAndOpenTag() )
            .append( "<head><title>" )
            .append( title )
            .append( "</title></head><body>" )
            .append( "<h1>" ).append( title ).append( "</h1>" )
            .append( body )
            .append( "</body></html>" )
            .toString();
    // Write the catalog out.
    PrintWriter out = res.getWriter();
    res.setContentType( "text/html" );
    res.setStatus( HttpServletResponse.SC_USE_PROXY );
    out.print( htmlResp );
    log.info( "doRedirect305(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_USE_PROXY, 0 ));
    return;
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

  private String getHtmlDoctypeAndOpenTag()
  {
    return new StringBuffer()
            .append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" )
            .append( "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" )
            .append( "<html>\n" )
            .toString();
  }

//  private String getXHtmlDoctypeAndOpenTag()
//  {
//    return new StringBuffer()
//            // .append( "<?xml version=\"1.0\" encoding=\"utf-8\"?>")
//            .append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n" )
//            .append( "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" )
//            .append( "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">" )
//            .toString();
//  }

}