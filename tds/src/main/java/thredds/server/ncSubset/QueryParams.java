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

package thredds.server.ncSubset;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.IOException;
import java.io.PrintWriter;

import thredds.servlet.ServletUtil;
import thredds.servlet.UsageLog;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.units.DateType;

/**
 * Query parameter parsing for Netcdf Subset Service
 *
 * @author caron
 */
public class QueryParams {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( QueryParams.class );

  static public final String RAW = "text/plain";
  static public final String XML = "application/xml";
  static public final String HTML = "text/html";
  static public final String CSV = "text/csv";
  static public final String NETCDF = "application/x-netcdf";
  static public final String NETCDFS = "application/x-netcdfs";
  static public final String CdmRemote = "application/x-cdmremote";

  // the first in the list is the canonical name, the others are aliases
  static String[][] validAccept = new String[][]{
          {XML, "text/xml", "xml"},
          {RAW, "raw", "ascii"},
          {CSV, "csv"},
          {HTML, "html"},
          {NETCDF, "netcdf"},
          {NETCDFS, "netcdfStream"},
          {CdmRemote, "cdmRemote"},
  };

  public String queryString;
  public List<String> accept;
  public String acceptType; // choose one of the accept

  public boolean wantAllVariables;
  public List<String> vars;

  // spatial subsetting
  public boolean hasBB = false, hasStns = false, hasLatlonPoint = false; // only one is true
  public double north, south, east, west;
  public double lat, lon;
  public List<String> stns; // for stationObs, empty list means all

  public int horizStride, vertStride, timeStride; // 0 = none

  public boolean hasVerticalCoord = false;
  public double vertCoord;

  // temporal subsetting
  public boolean hasDateRange = false, hasTimePoint = false; // only one is true
  public DateType time_start, time_end, time;
  public TimeDuration time_duration;
  public int time_latest;

  // track errors
  public StringBuilder errs = new StringBuilder();
  public boolean fatal;

  public String toString() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("queryString= " + queryString + "\n\n");
    sbuff.append("parsed=\n ");
    if (hasBB)
      sbuff.append("bb=" + getBB().toString2() + ";");
    else if (hasLatlonPoint)
      sbuff.append("lat/lon=" + getPoint() + ";");
    else if (hasStns) {
      boolean first = true;
      sbuff.append("stns=");
      for (String stnName : stns) {
        if (!first) sbuff.append(",");
        sbuff.append(stnName);
        first = false;
      }
      sbuff.append(";");
    } else {
      sbuff.append("spatial=all;");
    }
    sbuff.append("\n ");

    if (hasTimePoint)
      sbuff.append("time=" + time + ";");
    else if (hasDateRange) {
      sbuff.append("timeRange=" + getDateRange() + ";");
    } else {
      sbuff.append("temporal=all;");
    }
    sbuff.append("\n ");

    if (wantAllVariables)
      sbuff.append("vars=all;");
    else {
      boolean first = true;
      sbuff.append("vars=");
      for (String varName : vars) {
        if (!first) sbuff.append(",");
        sbuff.append(varName);
        first = false;
      }
      sbuff.append(";");
    }
    sbuff.append("\n ");

    return sbuff.toString();
  }

  /**
   * Parse request
   *
   * @param req      HTTP request
   * @param res      HTTP response
   * @param acceptOK array of acceptable accept types, in order. First one is default
   * @return true if params are ok
   * @throws java.io.IOException if I/O error
   */
  public boolean parseQuery(HttpServletRequest req, HttpServletResponse res, String[] acceptOK) throws IOException {
    queryString = URLDecoder.decode(req.getQueryString(), "UTF-8");

    accept = parseList(req, "accept", QueryParams.validAccept, acceptOK[0]);
    for (String ok : acceptOK) {
      if (accept.contains(ok)) {
        acceptType = ok;
      }
    }
    if (acceptType == null) {
      fatal = true;
      errs.append("Accept parameter not supported ="+accept);
    }

    // list of variable names
    String variables = ServletUtil.getParameterIgnoreCase(req, "variables");
    wantAllVariables = (variables != null) && (variables.equals("all"));
    if (!wantAllVariables) {
      vars = parseList(req, "var");
      if (vars.isEmpty()) {
        vars = null;
        wantAllVariables = true;
      }
    }

    // spatial subsetting
    String spatial = ServletUtil.getParameterIgnoreCase(req, "spatial");
    boolean spatialNotSpecified = (spatial == null);

    // bounding box
    if (spatialNotSpecified || spatial.equalsIgnoreCase("bb")) {
      north = parseLat(req, "north");
      south = parseLat(req, "south");
      east = parseDouble(req, "east");
      west = parseDouble(req, "west");
      hasBB = hasValidBB();
    }

    // stations
    if (!hasBB && (spatialNotSpecified || spatial.equalsIgnoreCase("stns"))) {
      stns = parseList(req, "stn");
      hasStns = stns.size() > 0;
    }

    // lat/lon point
    if (!hasBB && !hasStns && (spatialNotSpecified || spatial.equalsIgnoreCase("point"))) {
      lat = parseLat(req, "latitude");
      lon = parseLon(req, "longitude");
      hasLatlonPoint = hasValidPoint();
    }

    // strides
    horizStride = parseInt(req, "horizStride");
    vertStride = parseInt(req, "vertStride");
    timeStride = parseInt(req, "timeStride");

    // time range
    String temporal = ServletUtil.getParameterIgnoreCase(req, "temporal");
    boolean timeNotSpecified = (temporal == null);

    // time range
    if (timeNotSpecified || temporal.equalsIgnoreCase("range")) {
      time_start = parseDate(req, "time_start");
      time_end = parseDate(req, "time_end");
      time_duration = parseW3CDuration(req, "time_duration");
      hasDateRange = hasValidDateRange();
    }

    // time point
    if (timeNotSpecified || temporal.equalsIgnoreCase("point")) {
      time = parseDate(req, "time");
      hasTimePoint = (time != null);
    }

    // vertical coordinate
    vertCoord = parseDouble(req, "vertCoord");
    hasVerticalCoord = !Double.isNaN(vertCoord);

    if (fatal) {
      writeErr(req, res, errs.toString(), HttpServletResponse.SC_BAD_REQUEST);
      return false;
    }

    return true;
  }


  public DateType parseDate(HttpServletRequest req, String key) {
    String s = ServletUtil.getParameterIgnoreCase(req, key);
    if (s != null) {
      try {
        return new DateType(s, null, null);
      } catch (java.text.ParseException e) {
        errs.append("Illegal param= '" + key + "=" + s + "' must be valid ISO Date\n");
        fatal = true;
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
        fatal = true;
      }
    }
    return null;
  }

  public double parseDouble(HttpServletRequest req, String key) {
    String s = ServletUtil.getParameterIgnoreCase(req, key);
    if ((s != null) && (s.trim().length() > 0)) {
      try {
        return Double.parseDouble(s);
      } catch (NumberFormatException e) {
        errs.append("Illegal param= '" + key + "=" + s + "' must be valid floating point number\n");
        fatal = true;
      }
    }
    return Double.NaN;
  }

  public int parseInt(HttpServletRequest req, String key) {
    String s = ServletUtil.getParameterIgnoreCase(req, key);
    if ((s != null) && (s.trim().length() > 0)) {
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException e) {
        errs.append("Illegal param= '" + key + "=" + s + "' must be valid integer number\n");
        fatal = true;
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
        fatal = true;
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


  /**
   * parse KVP for key=value or key=value,value,...
   *
   * @param req HTTP request
   * @param key key to look for
   * @return list of values, may be empty
   */
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

  /**
   * Used for accept
   * parse KVP for key=value or key=value,value,...
   *
   * @param req      HTTP request
   * @param key      key to look for
   * @param valids   list of valid keywords
   * @param defValue default value
   * @return list of values, use default if not otherwise specified
   */
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

    if ((result.size() == 0) && (defValue != null)) {
      result.add(defValue);
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

  /**
   * Determine if a valid lat/lon bounding box was specified
   *
   * @return true if there is a valid BB, false if not. If an invalid BB, set fatal=true, with error message in errs.
   */
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

    if (north < south) {
      errs.append("Bounding Box must have north > south\n");
      fatal = true;
      return false;
    }

    if (east < west) {
      errs.append("Bounding Box must have east > west; if crossing 180 meridion, use east boundary > 180\n");
      fatal = true;
      return false;
    }

    return true;
  }

  public LatLonRect getBB() {
    return new LatLonRect(new LatLonPointImpl(south, west), new LatLonPointImpl(north, east));
  }

  LatLonPoint getPoint() {
    return new LatLonPointImpl(lat, lon);
  }

  /**
   * Determine if a valid lat/lon point was specified
   *
   * @return true if there is a valid point, false if not. If an invalid point, set fatal=true, with error message in errs.
   */
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

  /**
   * Determine if a valid date range was specified
   *
   * @return true if there is a valid date range, false if not. If an invalid date range, append error message in errs.
   */
  private boolean hasValidDateRange() {
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

  public DateRange getDateRange() {
      return hasDateRange ? new DateRange(time_start, time_end, time_duration, null) : null;
  }

  public void writeErr(HttpServletRequest req, HttpServletResponse res, String s, int code) throws IOException {
    log.error( "QueryParams bad request = {}", s);   // LOOK debug only
    log.info( "writeErr(): " + UsageLog.closingMessageForRequestContext(code, 0));
    res.setStatus(code);
    if (s.length() > 0) {
      PrintWriter pw = res.getWriter();
      pw.print(s);
      pw.print("Request= "+req.getRequestURI()+"?"+req.getQueryString());
      pw.close();
    }
  }

}

