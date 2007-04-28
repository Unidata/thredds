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

import thredds.servlet.AbstractServlet;
import thredds.servlet.ServletUtil;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Netcdf Grid subsetting.
 *
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public class StationObsServlet extends AbstractServlet {

  private boolean allow = true;
  private StationObsCollection soc;

  // must end with "/"
  protected String getPath() {
    return "ncSubsetService/";
  }

  protected void makeDebugActions() {
  }

  public void init() throws ServletException {
    super.init();
    //soc = new StationObsCollection("C:/data/metars/");
    soc = new StationObsCollection("/data/ldm/pub/decoded/netcdf/surface/metar/");
  }

  public void destroy() {
    super.destroy();
  }


  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }
    long start = System.currentTimeMillis();

    ServletUtil.logServerAccessSetup(req);
    System.out.println(req.getQueryString());

    String pathInfo = req.getPathInfo();

    boolean wantXML = pathInfo.endsWith("dataset.xml");
    boolean showForm = pathInfo.endsWith("dataset.html");
    if (wantXML || showForm) {
      showForm(res, wantXML);
      return;
    }

    // parse the input
    QueryParams qp = new QueryParams();
    qp.accept = qp.parseList(req, "accept", QueryParams.validAccept, QueryParams.RAW);

    // list of variable names
    qp.vars = qp.parseList(req, "var");
    if (qp.vars.isEmpty())
      qp.vars = null;

    // spatial subsetting

    // bounding box
    qp.north = qp.parseLat(req, "north");
    qp.south = qp.parseLat(req, "south");
    qp.east = qp.parseDouble(req, "east");
    qp.west = qp.parseDouble(req, "west");
    boolean hasBB = qp.hasValidBB();
    if (qp.fatal) {
      writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // stations
    qp.stns = qp.parseList(req, "stn");
    boolean hasStns = qp.stns.size() > 0;
    if (hasStns && soc.isStationListEmpty(qp.stns)) {
      qp.errs.append("ERROR: No valid stations specified\n");
      writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    if (!hasStns && hasBB) {
      qp.stns = soc.getStationNames(qp.getBB());
      if (qp.stns.size() == 0) {
        qp.errs.append("ERROR: Bounding Box contains no stations\n");
        writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
    }

    boolean useAll = false;
    if (!hasStns && !hasBB) {
      // does it have a lat/lon point
      qp.lat = qp.parseLat(req, "lat");
      qp.lon = qp.parseLon(req, "lon");

      if (qp.hasValidPoint()) {
        qp.stns.add(soc.findClosestStation(qp.lat, qp.lon));
      } else if (qp.fatal) {
        writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        return;
      } else {
        useAll = true;
      }
    }

    // time range
    qp.time_start = qp.parseDate(req, "time_start");
    qp.time_end = qp.parseDate(req, "time_end");
    qp.time_duration = qp.parseW3CDuration(req, "time_duration");

    // time point
    qp.time = qp.parseDate(req, "time");
    if ((qp.time != null) && (soc.filterDataset(qp.time) == null)) {
      qp.errs.append("ERROR: This dataset does not contain the time point= " + qp.time + " \n");
      writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // last n
    // qp.time_latest = qp.parseInt(req, "time_latest");

    if (useAll && (qp.getDateRange() == null) && (qp.time == null)) {
      qp.errs.append("ERROR: You must subset by space or time\n");
      writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // choose a type
    String type;
    if (qp.accept.contains(QueryParams.RAW)) {
      res.setContentType(QueryParams.RAW);
      type = QueryParams.RAW;

    } else if (qp.accept.contains(QueryParams.XML)) {
      res.setContentType(QueryParams.XML);
      type = QueryParams.XML;

    } else if (qp.accept.contains(QueryParams.CSV)) {
      res.setContentType("text/plain");
      type = QueryParams.CSV;

    } else if (qp.accept.contains(QueryParams.NETCDF)) {
      res.setContentType(QueryParams.NETCDF);
      type = QueryParams.NETCDF;
      res.setHeader("Content-Disposition", "attachment; filename=metarSubset.nc");
      File file = soc.writeNetcdf(qp.vars, qp.stns, qp.getDateRange(), qp.time);
      ServletUtil.returnFile(this, req, res, file, QueryParams.NETCDF);

      long took = System.currentTimeMillis() - start;
      System.out.println("\ntotal response took = " + took + " msecs");

      return;

    } else {
      writeErr(res, qp.errs.toString(), HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }

    soc.write(qp.vars, qp.stns, qp.getDateRange(), qp.time, type, res.getWriter());

    long took = System.currentTimeMillis() - start;
    System.out.println("\ntotal response took = " + took + " msecs");
  }

  private void writeErr(HttpServletResponse res, String s, int code) throws IOException {
    res.setStatus(code);
    if (s.length() > 0) {
      PrintWriter pw = res.getWriter();
      pw.print(s);
      pw.close();
    }
  }

  private void showForm(HttpServletResponse res, boolean wantXml) throws IOException {
    String infoString;

    if (wantXml) {
      Document doc = getDoc("sobsDataset.xml");
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      infoString = fmt.outputString(doc);

    } else {
      InputStream xslt = getXSLT("ncssSobs.xsl");
      Document doc = getDoc("sobsDataset.xml");

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
    if (wantXml)
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
