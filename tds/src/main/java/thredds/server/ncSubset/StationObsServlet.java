// $Id: NetcdfServlet.java 51 2006-07-12 17:13:13Z caron $
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
package thredds.server.ncSubset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import thredds.servlet.AbstractServlet;
import thredds.servlet.ServletUtil;

/**
 * Netcdf Grid subsetting.
 *
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public class StationObsServlet extends AbstractServlet {
  static final String RAW = "text/plain";
  static final String XML = "application/xml";
  static final String CSV = "text/csv";

  // the first in the list is the canonical name, the others are aliases
  private static String[][] validAccept = new String[][]{
          {XML, "text/xml", "xml"},
          {RAW, "raw", "ascii"},
          {CSV, "csv"},
          {"text/html", "html"},
          {"application/x-netcdf", "netcdf"},
  };

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
    soc = new StationObsCollection("C:/data/metars/");
    // soc = new StationObsCollection("C:/data/metar/");
    // soc = new StationObsCollection("/data/ldm/pub/decoded/netcdf/surface/metar/");
  }

  public void destroy() {
    super.destroy();
  }


  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    ServletUtil.logServerAccessSetup(req);
    ServletUtil.showRequestDetail(this, req);

    String pathInfo = req.getPathInfo();

    // parse the input
    QueryParams qp = new QueryParams();
    qp.accept = qp.parseList(req, "accept", validAccept, RAW);

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

    boolean spatialAll = false;
    if (!hasStns && !hasBB) {
      // gotta have a lat/lon point
      qp.lat = qp.parseLat(req, "lat");
      qp.lon = qp.parseLon(req, "lon");

      if (qp.hasValidPoint()) {
        qp.stns.add( soc.findClosestStation(qp.lat, qp.lon));

      } else {
        spatialAll = true;
      }
    }

    // time range
    qp.time_start = qp.parseDate(req, "time_start");
    qp.time_end = qp.parseDate(req, "time_end");
    qp.time_duration = qp.parseW3CDuration(req, "time_duration");

    // time point
    qp.time = qp.parseDate(req, "time");
    if ((qp.time != null) && (soc.filterDataset( qp.time) == null)) {
        qp.errs.append("ERROR: This dataset does not contain the time point= "+qp.time+" \n");
        writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

    // last n
    // qp.time_latest = qp.parseInt(req, "time_latest");

    // choose a type
    String type;
    if (qp.accept.contains(RAW)) {
      res.setContentType(RAW);
      type = RAW;

    } else if (qp.accept.contains(XML)) {
      res.setContentType(XML);
      type = XML;

    } else if (qp.accept.contains(CSV)) {
      res.setContentType("text/plain");
      type = CSV;

    } else {
      writeErr(res, qp.errs.toString(), HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }

    if (qp.stns.size() > 0) {
      if (qp.time != null)
        soc.write(qp.vars, qp.stns, qp.time, type, res.getWriter());
      else
      soc.write(qp.vars, qp.stns, qp.getDateRange(), type, res.getWriter());

    } else if (spatialAll) {

    } else {
      writeErr(res, qp.errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  private void writeErr(HttpServletResponse res, String s, int code) throws IOException {
    res.setStatus(code);
    if (s.length() > 0) {
      PrintWriter pw = res.getWriter();
      pw.print(s);
      pw.close();
    }
  }

}
