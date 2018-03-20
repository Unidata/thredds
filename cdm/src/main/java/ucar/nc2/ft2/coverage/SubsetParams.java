/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarPeriod;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.*;

/**
 * Describes a subset of a Coverage.
 * Coordinate values only, no indices.
 *
 * @see "docs/website/tds/reference/services/CdmrfParams.adoc"
 *
 * @author caron
 * @since 5/6/2015
 */
public class SubsetParams {
  public static final String variables = "var";           // value = List<String>

  public static final String latlonBB = "latlonBB";     // value = LatLonRect
  public static final String projBB = "projBB";         // value = ProjectionRect
  public static final String horizStride = "horizStride";  // value = Integer
  public static final String latlonPoint = "latlonPoint";  // value = LatLonPointImpl
  public static final String stations = "stn";           // value = List<String>

  public static final String time = "time";             // value = CalendarDate
  public static final String timeRange = "timeRange";   // value = CalendarDateRange
  public static final String timeStride = "timeStride"; // value = Integer
  public static final String timePresent = "timePresent"; // value = Boolean
  public static final String timeAll = "timeAll";         // value = Boolean
  public static final String timeWindow = "timeWindow";   // value = CalendarPeriod

  public static final String runtime = "runtime";       // value = CalendarDate
  // public static final String runtimeRange = "runtimeRange";       // value = CalendarDateRange
  public static final String runtimeLatest = "runtimeLatest"; // value = Boolean
  public static final String runtimeAll = "runtimeAll";       // value = Boolean

  public static final String timeOffset = "timeOffset"; // value = Double
  public static final String timeOffsetFirst = "timeOffsetFirst"; // value = Boolean
  public static final String timeOffsetAll = "timeOffsetAll"; // value = Boolean
  public static final String timeOffsetIntv = "timeOffsetIntv"; // value = double[2]

  public static final String vertCoord = "vertCoord";   // value = Double
  public static final String vertIntv = "vertIntv"; // value = double[2]
  public static final String vertRange = "vertRange";   // value = double[2] used WCS local, not remote

  public static final String ensCoord = "ensCoord";     // value = Double

  // cant use these for selecting, used for validation
  public static final String timeOffsetDate = "timeOffsetDate"; // value = CalendarDate
  public static final String timeOffsetUnit = "timeOffsetUnit"; // value = CalendarDateUnit

  /////////////////////////////////

  public void encodeForCdmrfDataRequest(Formatter f, String varname) {
    Escaper urlParamEscaper = UrlEscapers.urlFormParameterEscaper();
    f.format("req=data&var=%s", urlParamEscaper.escape(varname));

    for (Map.Entry<String,Object> entry : getEntries()) {
      switch (entry.getKey()) {
        case SubsetParams.latlonBB:
          LatLonRect llbb = (LatLonRect) entry.getValue();
          f.format("&north=%s&south=%s&west=%s&east=%s", llbb.getLatMax(), llbb.getLatMin(), llbb.getLonMin(), llbb.getLonMax());
          break;
        case SubsetParams.projBB:
          ProjectionRect projRect = (ProjectionRect) entry.getValue();
          f.format("&minx=%s&miny=%s&maxx=%s&maxy=%s", projRect.getMinX(), projRect.getMinY(), projRect.getMaxX(), projRect.getMaxY());
          break;
        case SubsetParams.latlonPoint:
          LatLonPointImpl llPoint = (LatLonPointImpl) entry.getValue();
          f.format("&lat=%s&lon=%s", llPoint.getLatitude(), llPoint.getLongitude());
          break;
        case SubsetParams.stations:
          List<String> stns = (List<String>) entry.getValue();
          int count = 0;
          for (String stn : stns) {
            if (count++ == 0)
              f.format("&stn=%s",stn);
            else
              f.format(",%s",stn);
          }
          break;
        case SubsetParams.timeRange:
          CalendarDateRange timeRange = (CalendarDateRange) entry.getValue();
          f.format("&time_start=%s&time_end=%s", timeRange.getStart(), timeRange.getEnd());
          break;
        case SubsetParams.timePresent:
          f.format("&time=present");
          break;
        case SubsetParams.timeAll:
          f.format("&time=all");
          break;
        case SubsetParams.runtimeLatest:
          f.format("&runtime=latest");
          break;
        case SubsetParams.runtimeAll:
          f.format("&runtime=all");
          break;
        case SubsetParams.timeOffsetAll:
          f.format("&timeOffset=all");
          break;
        case SubsetParams.timeOffsetFirst:
          f.format("&timeOffset=first");
          break;
        default:
          f.format("&%s=%s", entry.getKey(), entry.getValue());
          break;
      }
    }

  }

  ///////////////////////////////////////////////////

  private final Map<String, Object> req = new HashMap<>();

  public Set<Map.Entry<String, Object>> getEntries() {
    return req.entrySet();
  }
  public Set<String> getKeys() {
    return req.keySet();
  }

  public SubsetParams set(String key, Object value) {
    req.put(key, value);
    return this;
  }

  public Object get(String key) {
     return req.get(key);
   }

  public boolean isTrue(String key) {
     Boolean val = (Boolean) req.get(key);
    return (val != null) && val;
   }

  public Double getDouble(String key) {
    Object val = req.get(key);
    if (val == null) return null;
    Double dval;
    if (val instanceof Number) dval = ((Number) val).doubleValue();
    else dval = Double.parseDouble((String) val);
    return dval;
  }

  public Integer getInteger(String key) {
    Object val = req.get(key);
    if (val == null) return null;
    Integer dval;
    if (val instanceof Number) dval = ((Number) val).intValue();
    else dval = Integer.parseInt((String) val);
    return dval;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    for (Map.Entry<String,Object> entry : req.entrySet()) {
      f.format(" %s == ", entry.getKey());
      Object val = entry.getValue();
      if (val instanceof CalendarDate[]) {
        CalendarDate[] cd = ((CalendarDate[]) val);
        f.format("[%s,%s]", cd[0], cd[1]);
      } else if (val instanceof double[]) {
        double[] d = ((double[]) val);
        f.format("[%f,%f]", d[0], d[1]);
      } else {
        f.format("%s,", entry.getValue());
      }
    }
    return f.toString();
  }

  public boolean hasTimeParam() {
    return get(timeRange) != null || get(time) != null || get(timeStride) != null || get(timePresent) != null;
  }

  public boolean hasTimeOffsetParam() {
    return get(timeOffset) != null  || get(timeOffsetFirst) != null || get(timeOffsetIntv) != null;
  }

  public boolean hasTimeOffsetIntvParam() {
    return get(timeOffsetIntv) != null || get(timeOffsetFirst) != null;
  }

  public SubsetParams setHorizStride(int stride) {  set(horizStride, stride); return this; }

  public LatLonRect getLatLonBoundingBox() { return (LatLonRect) get(latlonBB);}
  public SubsetParams setLatLonBoundingBox(LatLonRect llbb) {  set(latlonBB, llbb); return this; }

  public ProjectionRect getProjectionRect() { return (ProjectionRect) get(projBB);}
  public SubsetParams setProjectionRect(ProjectionRect projRect) {  set(projBB, projRect); return this; }

  public LatLonPointImpl getLatLonPoint() { return (LatLonPointImpl) get(latlonPoint);}
  public SubsetParams setLatLonPoint(LatLonPointImpl pt) { set(latlonPoint, pt); return this; }

  public List<String> getStations() { return (List<String>) get(stations);}
  public SubsetParams setStations(List<String> stns) { set(stations, stns); return this; }

  public List<String> getVariables() { return (List<String>) get(variables);}
  public SubsetParams setVariables(List<String> vars) { set(variables, vars); return this; }

  public SubsetParams setTime(CalendarDate date) {  set(time, date); return this; }
  public CalendarDate getTime() { return (CalendarDate) get(time);}
  public SubsetParams setTimePresent() {  set(timePresent, true); return this; }

  public CalendarDateRange getTimeRange() { return (CalendarDateRange) get(timeRange);}
  public SubsetParams setTimeRange(CalendarDateRange dateRange) {  set(timeRange, dateRange); return this; }

  public CalendarDate getRunTime() { return (CalendarDate) get(runtime);}
  public SubsetParams setRunTime(CalendarDate date) {  set(runtime, date); return this; }

  public CalendarPeriod getTimeWindow() { return (CalendarPeriod) get(timeWindow);}

  public double[] getVertRange() { return (double []) get(vertRange);}
  public Double getVertCoord() { return (Double) get(vertCoord);}
  public SubsetParams setVertCoord(double coord) { set(vertCoord, coord); return this; }

  public Double getEnsCoord() { return (Double) get(ensCoord);}
  public SubsetParams setEnsCoord(double coord) { set(ensCoord, coord); return this; }

  public SubsetParams setVertCoordIntv(double [] vertCoordIntv) {  set(vertIntv, vertCoordIntv); return this; }
  public double[] getVertCoordIntv() { return (double[]) get(vertIntv);}

  // A time offset or time offset interval starts from the rundate of that point, in the units of the coordinate
  // eg "calendar Month since 2004-12-30T00:00:00Z" or "Hours since 2004-12-30T00:00:00Z"
  public SubsetParams setTimeOffset(double offset) {  set(timeOffset, offset); return this; }
  public Double getTimeOffset() {  return (Double) get(timeOffset); }

  public SubsetParams setTimeOffsetIntv(double [] timeCoordIntv) {  set(timeOffsetIntv, timeCoordIntv); return this; }
  public double [] getTimeOffsetIntv() { return (double []) get(timeOffsetIntv); }

}
