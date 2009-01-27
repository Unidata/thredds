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

import ucar.unidata.util.StringUtil;
import ucar.nc2.util.IO;

/**
 * Servlet handles creating DL records.
 *
 */
public class DLwriterServlet extends AbstractServlet {
  private String adnDir, difDir;

  public void init() throws ServletException {
    super.init();

    adnDir = contentPath + "/adn/";
    difDir = contentPath + "/dif/";

    File file = new File(adnDir);
    file.mkdirs();
    file = new File(difDir);
    file.mkdirs();
  }

  protected String getPath() { return "DLwriter/"; }
  protected void makeDebugActions() {  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
                             throws ServletException, IOException {

    log.info( UsageLog.setupRequestContext(req));

    try {
      // see if it has a catalog parameter
      String type = req.getParameter("type");
      String catURL = req.getParameter("catalog");
      if ((catURL == null) || (catURL.length() == 0))
        catURL = ServletUtil.getContextPath()+"/idd/models.xml";
      doit(req, res, catURL, type.equals("DIF"));

    } catch (Throwable t) {
      log.error("doGet req= "+ServletUtil.getRequest(req)+" got Exception", t);
      ServletUtil.handleException( t,  res);
    }
  }

  private void doit(HttpServletRequest req, HttpServletResponse res, String catURL, boolean isDIF) throws IOException, URISyntaxException {

    // resolve if needed
    String reqBase = ServletUtil.getRequestBase( req); // this is the base of the request
    URI reqURI = new URI( reqBase);
    URI catURI = reqURI.resolve( catURL);
    catURL = catURI.toString();

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
      ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, -1);

      res.setContentType("text/html");
      res.setHeader("Validate", "FAIL");
      PrintWriter pw = new PrintWriter(res.getOutputStream());
      showValidationMesssage( catURL, sb.toString(), pw);
      pw.flush();
      return;
    }

    StringBuffer mess = new StringBuffer();
    mess.append("Catalog " + catURL+"\n\n");

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
    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, mess.length());
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
