/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.cdmremote;

import com.google.common.base.MoreObjects;

import ucar.nc2.stream.NcStreamCompression;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Parses the query parameters for cdmRemote datasets.
 * This is the Model in Spring MVC
 *
 * @author caron
 * @since May 11, 2009
 */
public class CdmRemoteQueryBean {

  public enum RequestType {
    capabilities, cdl, data, header, ncml
  }

  public enum ResponseType {
    csv, netcdf, ncstream, html, xml
  }

  public enum SpatialSelection {
    all, bb, point, stns
  }

  public enum TemporalSelection {
    all, range, point
  }

  // type of request
  private String req = "";

  // type of response
  private String accept = "";

  // type of compression
  private NcStreamCompression compress = NcStreamCompression.none();

  // comma delimited list of variable names
  private String variables; // (forms) all some
  private String var;

  // parsed
  private RequestType reqType = null;

  boolean hasFatalError() {
    return fatal;
  }

  String getErrorMessage() {
    return errs.toString();
  }

  public LatLonRect getLatLonRect() {
    return (spatialSelection == SpatialSelection.bb) ? llbb : null;
  }

  DateRange getDateRange() {
    return dateRange;
  }

  public LatLonPoint getLatlonPoint() {
    return latlonPoint;
  }

  public DateType getTimePoint() {
    return timePoint;
  }

  public RequestType getRequestType() {
    if (reqType == null) {
      if (req.equalsIgnoreCase("capabilities")) reqType = RequestType.capabilities;
      else if (req.equalsIgnoreCase("cdl")) reqType = RequestType.cdl;
      else if (req.equalsIgnoreCase("data")) reqType = RequestType.data;
      else if (req.equalsIgnoreCase("dataForm")) reqType = RequestType.dataForm;
      else if (req.equalsIgnoreCase("form")) reqType = RequestType.form;
      else if (req.equalsIgnoreCase("header")) reqType = RequestType.header;
      else if (req.equalsIgnoreCase("ncml")) reqType = RequestType.ncml;
      else if (req.equalsIgnoreCase("stations")) reqType = RequestType.stations;
      else reqType = RequestType.data; // default
    }
    return reqType;
  }

  ResponseType getResponseType() {
    if (resType == null) {
      RequestType req = getRequestType();
      if (req == RequestType.capabilities) resType = ResponseType.xml;
      else if (req == RequestType.form) resType = ResponseType.html;
    }

    if (resType == null) {
      if (accept.equalsIgnoreCase("csv")) resType = ResponseType.csv;
      else if (accept.equalsIgnoreCase("ncstream")) resType = ResponseType.ncstream;
      else if (accept.equalsIgnoreCase("netcdf")) resType = ResponseType.netcdf;
      else if (accept.equalsIgnoreCase("xml")) resType = ResponseType.xml;
      else resType = ResponseType.ncstream; // default
    }

    return resType;
  }

  NcStreamCompression getCompression() {
    return compress;
  }

  public void setDeflate(String level) {
    compress = NcStreamCompression.deflate(Integer.parseInt(level));
  }

  SpatialSelection getSpatialSelection() {
    return spatialSelection;
  }


  TemporalSelection getTemporalSelection() {
    return temporalSelection;
  }

   boolean validate() {
    RequestType reqType = getRequestType();
    if (reqType == RequestType.dataForm) {
      parseVariablesForm();
      parseSpatialExtentForm();
      parseTemporalExtentForm();

    } else {
      parseSpatialExtent();
      parseTimeExtent();

      if ((spatialSelection == null) && (stn != null))
        spatialSelection = SpatialSelection.stns;
    }
    return !fatal;
  }

  private void parseVariablesForm() {  // from the form
    if (variables == null) {
      errs.format("form must have variables=(all|some)%n");
      fatal = true;
      return;
    }

    if (variables.equalsIgnoreCase("all")) {
      setVar(null);
    }
  }

  private void parseSpatialExtentForm() { // from the form
    if (spatial == null) {
      errs.format("form must have spatial=(all|bb|point|stns)%n");
      fatal = true;
      return;
    }

    if (spatial.equalsIgnoreCase("all")) spatialSelection = SpatialSelection.all;
    else if (spatial.equalsIgnoreCase("bb")) spatialSelection = SpatialSelection.bb;
    else if (spatial.equalsIgnoreCase("point")) spatialSelection = SpatialSelection.point;
    else if (spatial.equalsIgnoreCase("stns")) spatialSelection = SpatialSelection.stns;

    if (spatialSelection == SpatialSelection.bb) {
      parseSpatialExtent();

    } else if (spatialSelection == SpatialSelection.point) {
      double lat = parseLat("latitude", latitude);
      double lon = parseLon("longitude", longitude);
      latlonPoint = new LatLonPointImpl(lat, lon);
    }

  }

  private void parseSpatialExtent() {
    if (bbox != null) {
      String[] s = bbox.split(",");
      if (s.length != 4) {
        errs.format("bbox must have form 'bbox=west,east,south,north'; found 'bbox=%s'%n", bbox);
        fatal = true;
        return;
      }
      west = s[0];
      east = s[1];
      south = s[2];
      north = s[3];
    }

    if ((west != null) || (east != null) || (south != null) || (north != null)) {
      if ((west == null) || (east == null) || (south == null) || (north == null)) {
        errs.format("All edges (west,east,south,north) must be specified; found west=%s east=%s south=%s north=%s %n", west, east, south, north);
        fatal = true;
        return;
      }
      double westd = parseLon("west", west);
      double eastd = parseLon("east", east);
      double southd = parseLat("south", south);
      double northd = parseLat("north", north);

      if (!fatal) {
        llbb = new LatLonRect(new LatLonPointImpl(southd, westd), new LatLonPointImpl(northd, eastd));
        spatialSelection = SpatialSelection.bb;
      }
    }
  }

  private double parseLat(String key, String value) {
    double lat = parseDouble(key, value);
    if (!Double.isNaN(lat)) {
      if ((lat > 90.0) || (lat < -90.0)) {
        errs.format("Illegal param= param='%s=%s' must be between +/- 90.0 %n", key, value);
        lat = Double.NaN;
        fatal = true;
      }
    }
    return lat;
  }

  private double parseLon(String key, String value) {
    return parseDouble(key, value);
  }

  private double parseDouble(String key, String value) {
    value = value.trim();
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      errs.format("Illegal param='%s=%s' must be valid floating point number%n", key, value);
      fatal = true;
    }
    return Double.NaN;
  }

  ////////////////////////////////////////

  private void parseTemporalExtentForm() { // from the form
    if (temporal == null) {
      errs.format("form must have temporal=(all|range|point)%n");
      fatal = true;
      return;
    }

    if (temporal.equalsIgnoreCase("all")) temporalSelection = TemporalSelection.all;
    else if (temporal.equalsIgnoreCase("range")) temporalSelection = TemporalSelection.range;
    else if (temporal.equalsIgnoreCase("point")) temporalSelection = TemporalSelection.point;

    if (temporal.equalsIgnoreCase("range")) {
      try {
        parseTimeExtent();
      } catch (Throwable t) {
        errs.format("badly specified time range");
        fatal = true;
      }
    } else if (temporal.equalsIgnoreCase("point")) {
      timePoint = parseDate("time", time);
    }
  }

  private void parseTimeExtent() {
    DateType startDate = parseDate("time_start", time_start);
    DateType endDate = parseDate("time_end", time_end);
    TimeDuration duration = parseW3CDuration("time_duration", time_duration);

    // no range
    if ((startDate != null) && (endDate != null))
      dateRange = new DateRange(startDate, endDate, null, null);
    else if ((startDate != null) && (duration != null))
      dateRange = new DateRange(startDate, null, duration, null);
    else if ((endDate != null) && (duration != null))
      dateRange = new DateRange(null, endDate, duration, null);

    if (dateRange != null)
      temporalSelection = TemporalSelection.range;
  }


  public DateType parseDate(String key, String value) {
    if (value != null) {
      try {
        return new DateType(value, null, null);
      } catch (java.text.ParseException e) {
        errs.format("Illegal param='%s=%s'  must be valid ISO Date%n", key, value);
        fatal = true;
      }
    }
    return null;
  }

  public TimeDuration parseW3CDuration(String key, String value) {
    if (value != null) {
      try {
        return new TimeDuration(value);
      } catch (java.text.ParseException e) {
        errs.format("Illegal param='%s=%s'  must be valid ISO Duration%n", key, value);
        fatal = true;
      }
    }
    return null;
  }

  /////////////////////////////////////

  public void setAccept(String accept) {
    this.accept = accept;
  }

  public void setReq(String req) {
    this.req = req;
  }

  public String getVar() {
    return var;
  }

  public void setVar(String var) {
    this.var = var;
  }

  public RequestType getReqType() {
    return reqType;
  }

  public void setReqType(RequestType reqType) {
    this.reqType = reqType;
  }

  private List<String> errs;
  public void addError(String mess) {
    if (errs == null) errs = new ArrayList<>();
    errs.add(mess);
  }
  public boolean hasErrors() {
    return errs != null;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("errs", errs)
            .toString();
  }
}
