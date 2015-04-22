/* Copyright */
package thredds.server.cdmremote;

import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 4/4/2015
 */
public class CdmrFeatureQueryBean {
  public enum RequestType {
    capabilities, cdl, data, dataForm, form, header, ncml, stations
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

  // comma delimited list of variable names
  private String variables; // (forms) all some
  private String var;

  //// spatial selection
  private String spatial; // (forms) all, bb, point, stns
  private TemporalSelection temporalSelection;

  // comma delimited list of station ids
  private String stn;

  // spatial extent - one or none
  private String bbox;
  private String west, east, south, north;
  private String latitude, longitude;

  //// time selection
  private String temporal; // (forms) all, range, point
  private SpatialSelection spatialSelection;

  // time range
  private String time_start, time_end, time_duration;
  private String time;

  // parsed quantities
  private DateRange dateRange;
  private DateType timePoint;
  private LatLonRect llbb;
  private LatLonPoint latlonPoint;
  private RequestType reqType = null;
  private ResponseType resType = null;

  private boolean fatal = false;
  private Formatter errs = new Formatter();


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

  SpatialSelection getSpatialSelection() {
    return spatialSelection;
  }


  TemporalSelection getTemporalSelection() {
    return temporalSelection;
  }

  boolean validate() {
    RequestType reqType = getRequestType();
    if (reqType == null) {
      errs.format("form must req=[%s]%n", RequestType.values());
      fatal = true;
      return !fatal;
    }

    switch (reqType) {
      case dataForm:
        parseVariablesForm();
        parseSpatialExtentForm();
        parseTemporalExtentForm();
        break;

      case data:
        parseVariablesForm();
        // fall through
      default:
        parseSpatialExtent();
        parseTimeExtent();
        if ((spatialSelection == null) && (stn != null))
          spatialSelection = SpatialSelection.stns;
    }

    return !fatal;
  }

  private void parseVariablesForm() {  // from the form
    if (variables == null) {
      errs.format("data request must have var=(all|some)%n");
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

  // variable names
  public void setVariables(String variables) {
    this.variables = variables;
  }

  public void setVar(String var) {
    this.var = var;
  }

  public String getVar() {
    return var;
  }

  public String[] getVarNames() {
    return (var == null) ? null : var.split(",");
  }

  //////// spatial

  public void setSpatial(String spatial) {
    this.spatial = spatial;
  }

  public void setStn(String stn) {
    this.stn = stn;
  }

  public String getStn() {
    return stn;
  }

  public String[] getStnNames() {
    if (!(spatialSelection == SpatialSelection.stns)) return null;
    return (stn == null) ? null : stn.split(",");
  }

  public void setBbox(String bbox) {
    this.bbox = bbox;
  }

  public void setWest(String west) {
    this.west = west;
  }

  public void setEast(String east) {
    this.east = east;
  }

  public void setSouth(String south) {
    this.south = south;
  }

  public void setNorth(String north) {
    this.north = north;
  }

  public void setLatitude(String latitude) {
    this.latitude = latitude;
  }

  public void setLongitude(String longitude) {
    this.longitude = longitude;
  }

  //////// temporal

  public void setTemporal(String temporal) {
    this.temporal = temporal;
  }

  public void setTime_start(String timeStart) {
    this.time_start = timeStart;
  }

  public void setTime_end(String timeEnd) {
    this.time_end = timeEnd;
  }

  public void setTime_duration(String timeDuration) {
    this.time_duration = timeDuration;
  }

  public void setTime(String time) {
    this.time = time;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    f.format("QueryBean: reqType=%s resType=%s", getRequestType(), getResponseType());

    if (spatialSelection == SpatialSelection.all)
      f.format(" spatialSelection=all;");
    else if (spatialSelection == SpatialSelection.bb)
      f.format(" bb=%s;", getLatLonRect());
    else if (spatialSelection == SpatialSelection.stns)
      f.format(" stns=%s;", getStn());

    if (temporalSelection == TemporalSelection.all)
      f.format(" temporalSelection=all;");
    else if (temporalSelection == TemporalSelection.range)
      f.format(" range=%s;", getDateRange());

    if (var != null)
      f.format(" vars=%s", var);

    return f.toString();
  }
}
