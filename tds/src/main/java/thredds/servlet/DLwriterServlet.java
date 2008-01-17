// $Id: DLwriterServlet.java 51 2006-07-12 17:13:13Z caron $
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

    ServletUtil.logServerAccessSetup( req );

    try {
      // see if it has a catalog parameter
      String type = req.getParameter("type");
      String catURL = req.getParameter("catalog");
      if ((catURL == null) || (catURL.length() == 0))
        catURL = "/thredds/idd/models.xml";
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
    StringBuffer sb = new StringBuffer();
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
