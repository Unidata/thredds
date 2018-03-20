/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.ncss.params;

import thredds.server.ncss.validation.NcssGridRequestConstraint;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionRect;

/**
 * Parameters specific to ncss grid
 *
 * @author caron
 * @since 4/29/2015
 */
@NcssGridRequestConstraint
public class NcssGridParamsBean extends NcssParamsBean {

  //// projection rectangle
  private Double minx;
  private Double maxx;
  private Double miny;
  private Double maxy;

  private boolean addLatLon;
  private Integer horizStride = 1;

  private Integer timeStride = 1;
  private Double vertCoord;
  private Double ensCoord;

  private String runtime;
  private String timeOffset;

  public Double getMinx() {
    return minx;
  }

  public void setMinx(Double minx) {
    this.minx = minx;
  }

  public Double getMaxx() {
    return maxx;
  }

  public void setMaxx(Double maxx) {
    this.maxx = maxx;
  }

  public Double getMiny() {
    return miny;
  }

  public void setMiny(Double miny) {
    this.miny = miny;
  }

  public Double getMaxy() {
    return maxy;
  }

  public void setMaxy(Double maxy) {
    this.maxy = maxy;
  }

  public boolean isAddLatLon() {
    return addLatLon;
  }

  public void setAddLatLon(boolean addLatLon) {
    this.addLatLon = addLatLon;
  }

  public Integer getHorizStride() {
    return horizStride;
  }

  public void setHorizStride(Integer horizStride) {
    this.horizStride = horizStride;
  }

  public Integer getTimeStride() {
    return timeStride;
  }

  public void setTimeStride(Integer timeStride) {
    this.timeStride = timeStride;
  }

  public boolean hasProjectionBB() { // need to validate
    return minx != null && miny != null && maxx != null && maxy != null;
  }

  public ProjectionRect getProjectionBB() {
    return new ProjectionRect(minx, miny, maxx, maxy);
  }

  public Double getVertCoord() {
    return vertCoord;
  }

  public void setVertCoord(Double vertCoord) {
    this.vertCoord = vertCoord;
  }

  public Double getEnsCoord() {
    return ensCoord;
  }

  public void setEnsCoord(Double ensCoord) {
    this.ensCoord = ensCoord;
  }

  public String getRuntime() {
    return runtime;
  }

  public void setRuntime(String runtime) {
    this.runtime = runtime;
  }

  public String getTimeOffset() {
    return timeOffset;
  }

  public void setTimeOffset(String timeOffset) {
    this.timeOffset = timeOffset;
  }

  ////////////////////////////
  protected Double timeOffsetVal;
  protected boolean firstTimeOffset;

  public void setTimeOffsetVal(Double timeOffsetVal) {
    this.timeOffsetVal = timeOffsetVal;
  }

  public void setFirstTimeOffset(boolean firstTimeOffset) {
    this.firstTimeOffset = firstTimeOffset;
  }

  ////////////////////////////////////////////

  protected CalendarDate runtimeDate;
  protected boolean latestRuntime;
  protected boolean allRuntime;

  public void setLatestRuntime(boolean b) {
    this.latestRuntime = b;
  }

  public void setAllRuntime(boolean b) {
    this.allRuntime = b;
  }

  public void setRuntimeDate(CalendarDate runtimeDate) {
    this.runtimeDate = runtimeDate;
  }

  public CalendarDate getRuntimeDate(Calendar cal) {
    if (runtimeDate == null) return null;
    if (cal.equals(Calendar.getDefault())) return runtimeDate;

    // otherwise must reparse
    if (getTime().equalsIgnoreCase("present")) {
      return CalendarDate.present(cal);
    }

    return CalendarDateFormatter.isoStringToCalendarDate(cal, getRuntime());
  }

  ///////////////////////////////

  public SubsetParams makeSubset(CoverageCollection gcd) {
    Calendar cal = gcd.getCalendar();
    boolean isFmrc = gcd.getCoverageType() == FeatureType.FMRC;

    SubsetParams subset = new SubsetParams();

    // vars
    subset.set(SubsetParams.variables, var);

    // other coords
    if (vertCoord != null)
      subset.set(SubsetParams.vertCoord, vertCoord);

    if (ensCoord != null)
      subset.set(SubsetParams.ensCoord, ensCoord);

    // horiz subset
    if (hasProjectionBB())
      subset.set(SubsetParams.projBB, getProjectionBB());
    else if (hasLatLonBB())
      subset.set(SubsetParams.latlonBB, getLatLonBoundingBox());
    if (horizStride != null && horizStride != 1)
      subset.set(SubsetParams.horizStride, horizStride);
    if (hasLatLonPoint())
      subset.set(SubsetParams.latlonPoint, new LatLonPointImpl(getLatitude(), getLongitude()));

    if (isFmrc) { // 2D Time subsetting

      // runtime
      CalendarDate rundate = getRuntimeDate(cal);
      if (rundate != null)
        subset.set(SubsetParams.runtime, rundate);
      else if (allRuntime)
        subset.set(SubsetParams.runtimeAll, true);
      else
        subset.set(SubsetParams.runtimeLatest, true); // default

      // timeOffset
      if (timeOffsetVal != null)
        subset.set(SubsetParams.timeOffset, timeOffsetVal);
      else if (firstTimeOffset)
        subset.set(SubsetParams.timeOffsetFirst, true);

      else { // if no timeOffset, will allow some time values
        CalendarDate date = getRequestedDate(cal);
        CalendarDateRange dateRange = getCalendarDateRange(cal);

        if (isPresentTime())
          subset.setTimePresent();

        else if (isAllTimes() && !allRuntime) {
          subset.set(SubsetParams.timeAll, true);
          if (timeStride != null && timeStride != 1)
            subset.set(SubsetParams.timeStride, timeStride);

        } else if (date != null) {              // for allRuntimes, only a date is allowed
          subset.set(SubsetParams.time, date);

        } else if (dateRange != null  && !allRuntime) {
          subset.set(SubsetParams.timeRange, dateRange);
          if (timeStride != null && timeStride != 1)
            subset.set(SubsetParams.timeStride, timeStride);
        }
      }

    } else { // not an FMRC

      // time
      CalendarDate date = getRequestedDate(cal);
      CalendarDateRange dateRange = getCalendarDateRange(cal);
      if (isPresentTime())
        subset.setTimePresent();
      else if (isAllTimes()) {
        subset.set(SubsetParams.timeAll, true);
        if (timeStride != null && timeStride != 1)
          subset.set(SubsetParams.timeStride, timeStride);

      } else if (date != null) {
        subset.set(SubsetParams.time, date);

      } else if (dateRange != null) {
        subset.set(SubsetParams.timeRange, dateRange);
        if (timeStride != null && timeStride != 1)
          subset.set(SubsetParams.timeStride, timeStride);

      } else {
        subset.set(SubsetParams.timePresent, true);
      }
    }

    return subset;
  }

}
