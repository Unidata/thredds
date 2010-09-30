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

import thredds.catalog.*;
import thredds.catalog.dl.*;

import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;

import ucar.nc2.util.IO;
import ucar.unidata.util.StringUtil;

/**
 * Servlet handles creating DL records.
 *
 */
public class DLwriterServlet extends AbstractServlet {
  private String adnDir, difDir;
  private boolean allow, allowRemote;

  public void init() throws ServletException {
    super.init();
    allow = ThreddsConfig.getBoolean( "DLwriter.allow", false );
    if ( !allow )
    {
      logServerStartup.info( "DLwriterServlet.init(): DLwriter service not enabled in threddsConfig.xml: " + UsageLog.closingMessageNonRequestContext() );
      return;
    }
    allowRemote = ThreddsConfig.getBoolean( "DLwriter.allowRemote", false );

    adnDir = contentPath + "/adn/";
    difDir = contentPath + "/dif/";

    File file = new File(adnDir);
    file.mkdirs();
    file = new File(difDir);
    file.mkdirs();
    logServerStartup.info( "DLwriterServlet.init() - done: " + UsageLog.closingMessageNonRequestContext() );
  }

  protected String getPath() { return "DLwriter/"; }
  protected void makeDebugActions() {  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
                             throws ServletException, IOException {

    log.info( UsageLog.setupRequestContext(req));
    if ( ! allow )
    {
      res.sendError( HttpServletResponse.SC_FORBIDDEN, "DLwriter service not supported" );
      log.debug( "doGet(): DLwriter service not enabled in threddsConfig.xml.");
      log.info( "doGet(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, -1 ) );
      return;
    }

    try {
      // see if it has a catalog parameter
      String type = req.getParameter("type");
      String catURL = req.getParameter("catalog");
      if ((catURL == null) || (catURL.length() == 0))
        catURL = ServletUtil.getContextPath()+"/idd/models.xml";
      URI catUri = null;
      try
      {
        catUri = new URI( catURL);
      }
      catch ( URISyntaxException e )
      {
        res.sendError( HttpServletResponse.SC_FORBIDDEN, "Given catalog URL not a URL." );
        log.debug( "doGet(): Given catalog URL not a URL", e );
        log.info( "doGet(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, -1 ) );
        return;
      }
      if ( catUri.isAbsolute())
      {
        if ( ! allowRemote )
        {
          res.sendError( HttpServletResponse.SC_FORBIDDEN, "Given catalog URL not allowed (remote)." );
          log.debug( "doGet(): Given catalog URL was absolute, remote catalog handling not enabled.");
          log.info( "doGet(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, -1 ) );
          return;
        }
      }
      // Default "type" parameter to "DIF"
      boolean isDIF = type == null ? true : type.equals( "DIF" );
      doit(req, res, catURL, isDIF );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, -1 ) );

    } catch (Throwable t) {
      log.error("doGet(): req= "+ServletUtil.getRequest(req)+" got Exception", t);
      ServletUtil.handleException( t,  res);
    }
  }

  private void doit(HttpServletRequest req, HttpServletResponse res, String catURL, boolean isDIF)
          throws IOException {

    URI catURI;
    try
    {
      // Resolve against the request URL.
      URI reqURI = new URI( req.getRequestURL().toString() );
      catURI = reqURI.resolve( catURL );
    }
    catch ( URISyntaxException e )
    {
      res.sendError( HttpServletResponse.SC_FORBIDDEN, "Given catalog URL not a URL." );
      log.debug( "doGet(): Given catalog URL not a URL", e );
      log.info( "doGet(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, -1 ) );
      return;
    }

    // parse the catalog
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false);
    InvCatalogImpl catalog;
    try {
      catalog = catFactory.readXML(catURI);
    } catch (Exception e) {
      ServletUtil.handleException( e, res);
      return;
    }

    // validate the catalog
    StringBuilder sb = new StringBuilder();
    if (!catalog.check(sb, false)) {
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_BAD_REQUEST, -1));

      res.setContentType("text/html");
      res.setHeader("Validate", "FAIL");
      PrintWriter pw = new PrintWriter(res.getOutputStream());
      showValidationMesssage( catURI.toString(), sb.toString(), pw);
      pw.flush();
      return;
    }

    StringBuffer mess = new StringBuffer();
    mess.append("Catalog " + catURI.toString()+"\n\n");

    if (isDIF) {
      mess.append("DIF records:"+"\n");
      DIFWriter writer = new DIFWriter();
      writer.writeDatasetEntries(catalog, difDir, mess);

    } else { // ADN
      mess.append("ADN records:"+"\n");
      ADNWriter writer = new ADNWriter();
      mess.setLength(0);
      writer.writeDatasetEntries(catalog, adnDir, mess);
    }

    res.setContentType("text/plain");
    OutputStream out = res.getOutputStream();
    out.write(mess.toString().getBytes());
    out.flush();
    log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, mess.length()));
  }

  private void showValidationMesssage(String catURL, String mess, java.io.PrintWriter pw) {

    pw.println(HtmlWriter.getInstance().getHtmlDoctypeAndOpenTag());
    pw.println("<head>");
    pw.println("<title> Catalog Validation</title>");
    pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html\">");
    pw.println("</head>");
    pw.println("<body bgcolor=\"#FFF0FF\">");

    pw.println("<h2> Catalog " + catURL+" has validation errors:</h2>");

    pw.println("<b>");
    pw.println( StringUtil.quoteHtmlContent(mess));
    pw.println("</b>");

    // show catalog.xml
    pw.println("<hr><pre>");
    String catString = IO.readURLcontents( catURL);
    pw.println( StringUtil.quoteHtmlContent(catString));
    pw.println("</pre>");

    pw.println("</body>");
    pw.println("</html>");
  }

}
