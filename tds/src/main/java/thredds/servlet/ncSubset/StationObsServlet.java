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
package thredds.servlet.ncSubset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import thredds.servlet.AbstractServlet;
import thredds.servlet.ServletUtil;

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
    soc = new StationObsCollection("C:/data/metars/");
  }

  public void destroy() {
    super.destroy();
  }

  // the first in the list is the canonical name, the others are aliases
  private static String[][] validAccept = new String[][]{
      {"application/xml", "text/xml", "xml"},
      {"text/html", "html"},
      {"application/x-netcdf", "netcdf"},
      {"text/plain", "raw", "ascii"},
  };

  public class QP {
    public List<String> accept;

    public double north, south, east, west;
    public double lat, lon;
    public List<String> stns;

    public Date time_start, time_end, time;
    public long time_duration;
    public int time_latest;

    public StringBuffer errs = new StringBuffer();
    public boolean fatal;

    private DateFormatter format;

    public Date parseDate(HttpServletRequest req, String key) {
      String s = ServletUtil.getParameterIgnoreCase(req, key);
      if (s != null) {
        try {
          if (format == null) format = new DateFormatter();
          return format.isoDateTimeFormat(s);
        } catch (java.text.ParseException e) {
          errs.append("Illegal param= '" + key + "=" + s + "' must be valid ISO Date\n");
        }
      }
      return null;
    }

    public double parseDouble(HttpServletRequest req, String key) {
      String s = ServletUtil.getParameterIgnoreCase(req, key);
      if (s != null) {
        try {
          return Double.parseDouble(s);
        } catch (NumberFormatException e) {
          errs.append("Illegal param= '" + key + "=" + s + "' must be valid floating point number\n");
        }
      }
      return Double.NaN;
    }

    public int parseInt(HttpServletRequest req, String key) {
      String s = ServletUtil.getParameterIgnoreCase(req, key);
      if (s != null) {
        try {
          return Integer.parseInt(s);
        } catch (NumberFormatException e) {
          errs.append("Illegal param= '" + key + "=" + s + "' must be valid integer number\n");
        }
      }
      return 0;
    }

    public double parseLat(HttpServletRequest req, String key) {
      double lat = parseDouble(req, key);
      if (!Double.isNaN(lat)) {
        if ((lat > 90.0) || (lat < -90.0)) {
          errs.append("Illegal param= '" + key + "=" + lat + "' must be between +/- 90.0\n");
          lat = Double.NaN;
        }
      }
      return lat;
    }

    public double parseLon(HttpServletRequest req, String key) {
      double lon = parseDouble(req, key);
      if (!Double.isNaN(lon)) {
        lon = LatLonPointImpl.lonNormal(lon);
      }
      return lon;
    }

    public List<String> parseList(HttpServletRequest req, String key) {
      ArrayList<String> result = new ArrayList<String>();

      // may have multiple key=value
      String[] vals = ServletUtil.getParameterValuesIgnoreCase(req, key);
      for (String userVal : vals) {

        if (userVal.contains(",")) { // comma separated values
          StringTokenizer stoke = new StringTokenizer(userVal, ",");
          while (stoke.hasMoreTokens()) {
            String token = stoke.nextToken();
            result.add(token);
          }

        } else { // single value
          result.add(userVal);
        }
      }

      return result;
    }

    public List<String> parseList(HttpServletRequest req, String key, String[][] valids, String defValue) {
      ArrayList<String> result = new ArrayList<String>();

      // may have multiple key=value
      String[] vals = ServletUtil.getParameterValuesIgnoreCase(req, key);
      for (String userVal : vals) {

        if (userVal.contains(",")) { // comma separated values
          StringTokenizer stoke = new StringTokenizer(userVal, ",");
          while (stoke.hasMoreTokens()) {
            String token = stoke.nextToken();
            if (!findValid(token, valids, result))
              errs.append("Illegal param '" + key + "=" + token + "'\n");
          }

        } else { // single value
          if (!findValid(userVal, valids, result))
            errs.append("Illegal param= '" + key + "=" + userVal + "'\n");
        }
      }

      if (result.size() == 0) {
        if (defValue == null) fatal = true;
        else result.add(defValue);
      }

      return result;
    }

    // look for userVal in list of valids; add to result if found
    // return true if found
    private boolean findValid(String userVal, String[][] valids, ArrayList<String> result) {
      for (String[] list : valids) {
        String canon = list[0];
        for (String valid : list) {
          if (userVal.equalsIgnoreCase(valid)) {
            result.add(canon);
            return true;
          }
        }
      }
      return false;
    }

    boolean hasValidBB() {
      // no bb
      if (Double.isNaN(north) && Double.isNaN(south) && Double.isNaN(east) && Double.isNaN(west))
        return false;

      // misformed bb
      if (Double.isNaN(north) || Double.isNaN(south) || Double.isNaN(east) || Double.isNaN(west)) {
        errs.append("Bounding Box must have all 4 parameters: north,south,east,west\n");
        return false;
      }


      return true;
    }

    LatLonRect getBB() {
      return new LatLonRect(new LatLonPointImpl(south, west), new LatLonPointImpl(north, east));
    }


    boolean hasValidPoint() {
      // no point
      if (Double.isNaN(lat) && Double.isNaN(lon))
        return false;

      // misformed point
      if (Double.isNaN(lat) || Double.isNaN(lon)) {
        errs.append("Missing lat or lon parameter\n");
        return false;
      }
      return true;
    }
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
    QP qp = new QP();
    qp.accept = qp.parseList(req, "accept", validAccept, "text/plain");

    // bounding box
    qp.north = qp.parseLat(req, "north");
    qp.south = qp.parseLat(req, "south");
    qp.east = qp.parseDouble(req, "east");
    qp.west = qp.parseDouble(req, "west");
    boolean hasBB = qp.hasValidBB();

    // lat/lon point
    qp.lat = qp.parseLat(req, "lat");
    qp.lon = qp.parseLon(req, "lon");
    boolean hasPoint = qp.hasValidPoint();

    // stations
    qp.stns = qp.parseList(req, "stn");
    boolean hasStns = qp.stns.size() > 0;

    // time range
    qp.time_start = qp.parseDate(req, "time_start");
    qp.time_end = qp.parseDate(req, "time_end");
    //qp.time_duration = qp.parseDuration(req, "time_duration");

    // time point
    qp.time = qp.parseDate(req, "time");

    // last n
    qp.time_latest = qp.parseInt(req, "time_latest");

    // kludge just a bit!
    res.setContentType("text/plain");
    soc.writeRaw( qp.stns, res.getWriter());
  }

}
