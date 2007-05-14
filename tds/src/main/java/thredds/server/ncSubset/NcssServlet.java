/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package thredds.server.ncSubset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;

import thredds.servlet.AbstractServlet;
import thredds.servlet.ServletUtil;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Netcdf subsetting.
 *
 * @author caron
 */
public class NcssServlet extends AbstractServlet {

  private boolean allow = true, debug = true;
  private StationObsCollection soc;

  // must end with "/"
  protected String getPath() {
    return "ncss/";
  }

  protected void makeDebugActions() {
  }

  public void init() throws ServletException {
    super.init();
  }

  public void destroy() {
    super.destroy();
    soc.close();
  }


  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }
    long start = System.currentTimeMillis();

    ServletUtil.logServerAccessSetup(req);
    if (debug) System.out.println(req.getQueryString());

    String pathInfo = req.getPathInfo();

    boolean wantXML = pathInfo.endsWith("dataset.xml");
    boolean showForm = pathInfo.endsWith("dataset.html");
    boolean wantStationXML = pathInfo.endsWith("stations.xml");
    if (wantXML || showForm || wantStationXML) {
      showForm(res, wantXML, wantStationXML);
      return;
    }

    // parse the input
    QueryParams qp = new QueryParams();
    qp.parseQuery(req, res);

    soc.write(qp.vars, qp.stns, qp.getDateRange(), qp.time, qp.type, res.getWriter());

    long took = System.currentTimeMillis() - start;
    if (debug) System.out.println("\ntotal response took = " + took + " msecs");
  }

  private void showForm(HttpServletResponse res, boolean wantXml, boolean wantStationXml) throws IOException {
    String infoString;

    if (wantXml) {
      Document doc = soc.getDoc();
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      infoString = fmt.outputString(doc);

    } else if (wantStationXml) {
        Document doc = soc.getStationDoc();
        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        infoString = fmt.outputString(doc);

    } else {
      InputStream xslt = getXSLT("ncssSobs.xsl");
      Document doc = soc.getDoc();

      try {
        XSLTransformer transformer = new XSLTransformer(xslt);
        Document html = transformer.transform(doc);
        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        infoString = fmt.outputString(html);

      } catch (Exception e) {
        log.error("SobsServlet internal error", e);
        ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SobsServlet internal error");
        return;
      }
    }

    res.setContentLength(infoString.length());
    if (wantXml || wantStationXml)
      res.setContentType("text/xml; charset=iso-8859-1");
    else
      res.setContentType("text/html; charset=iso-8859-1");

    OutputStream out = res.getOutputStream();
    out.write(infoString.getBytes());
    out.flush();

    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, infoString.length());
  }

  private InputStream getXSLT(String xslName) {
    return getClass().getResourceAsStream("/resources/xsl/" + xslName);
  }

  private Document getDoc(String name) throws IOException {
    java.net.URL url = getClass().getResource("/resources/xsl/" + name);
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(url);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage() + " reading from XML " + url);
    }
    return doc;
  }


}
