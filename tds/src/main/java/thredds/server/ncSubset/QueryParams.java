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

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import thredds.servlet.ServletUtil;
import thredds.datatype.DateRange;
import thredds.datatype.TimeDuration;
import thredds.datatype.DateType;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class QueryParams {
  static final String RAW = "text/plain";
  static final String XML = "application/xml";
  static final String CSV = "text/csv";
  static final String NETCDF = "application/x-netcdf";

  // the first in the list is the canonical name, the others are aliases
  static String[][] validAccept = new String[][]{
      {XML, "text/xml", "xml"},
      {RAW, "raw", "ascii"},
      {CSV, "csv"},
      {"text/html", "html"},
      {"application/x-netcdf", "netcdf"},
  };

  public List<String> accept;
  public List<String> vars;

  public double north, south, east, west;
  public double lat, lon;
  public List<String> stns;

  public DateType time_start, time_end, time;
  public TimeDuration time_duration;
  public int time_latest;

  public StringBuffer errs = new StringBuffer();
  public boolean fatal;

  public DateType parseDate(HttpServletRequest req, String key) {
    String s = ServletUtil.getParameterIgnoreCase(req, key);
    if (s != null) {
      try {
        return new DateType(s, null, null);
      } catch (java.text.ParseException e) {
        errs.append("Illegal param= '" + key + "=" + s + "' must be valid ISO Date\n");
      }
    }
    return null;
  }

  public TimeDuration parseW3CDuration(HttpServletRequest req, String key) {
    String s = ServletUtil.getParameterIgnoreCase(req, key);
    if (s != null) {
      try {
        return TimeDuration.parseW3CDuration(s);
      } catch (java.text.ParseException e) {
        errs.append("Illegal param= '" + key + "=" + s + "' must be valid ISO Duration\n");
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
    if (vals != null) {
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
    }

    return result;
  }

  public List<String> parseList(HttpServletRequest req, String key, String[][] valids, String defValue) {
    ArrayList<String> result = new ArrayList<String>();

    // may have multiple key=value
    String[] vals = ServletUtil.getParameterValuesIgnoreCase(req, key);
    if (vals != null) {
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
      fatal = true;
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
      fatal = true;
      return false;
    }
    return true;
  }

  boolean hasValidDateRange() {
    // no range
    if ((null == time_start) && (null == time_end) && (null == time_duration))
      return false;

    if ((null != time_start) && (null != time_end))
      return true;

    if ((null != time_start) && (null != time_duration))
      return true;

    if ((null != time_end) && (null != time_duration))
      return true;

    // misformed range
    errs.append("Must have 2 of 3 parameters: time_start, time_end, time_duration\n");
    return false;

  }

  DateRange getDateRange() {
    return hasValidDateRange() ? new DateRange(time_start, time_end, time_duration, null) : null;
  }
}

