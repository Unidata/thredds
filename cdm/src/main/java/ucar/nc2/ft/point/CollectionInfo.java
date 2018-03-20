/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Value class to hold bounds info for a collection
 *
 * @author caron
 * @since 9/25/2015.
 */
public class CollectionInfo {
  public LatLonRect bbox;              // can be null if count == 0
  private CalendarDateRange dateRange;// can be null if count == 0
  public double minTime = Double.MAX_VALUE; // in units of dsg.timeUnit
  public double maxTime = -Double.MAX_VALUE;
  public int nobs;
  public int nfeatures;
  private boolean complete;

  public CollectionInfo() {}

  public CollectionInfo(LatLonRect bbox, CalendarDateRange dateRange, int nfeatures, int nobs) {
    this.bbox = bbox;
    this.dateRange = dateRange;
    this.nfeatures = nfeatures;
    this.nobs = nobs;
  }

  public void extend(CollectionInfo info) {
    if (info.nobs == 0) return;
    nobs += info.nobs;
    nfeatures++;

    if (bbox == null) bbox = info.bbox;
    else if (info.bbox != null) bbox.extend(info.bbox);

    minTime = Math.min(minTime, info.minTime);
    maxTime = Math.max(maxTime, info.maxTime);
  }

  public CalendarDateRange getCalendarDateRange(CalendarDateUnit timeUnit) {
    if (nobs == 0) return null;
    if (dateRange != null) return dateRange;
    if (timeUnit != null && minTime <= maxTime) {
      dateRange = CalendarDateRange.of(timeUnit.makeCalendarDate(minTime), timeUnit.makeCalendarDate(maxTime));
    }
    return dateRange;
  }

  public void setCalendarDateRange(CalendarDateRange dateRange) {
    this.dateRange = dateRange;
  }

  public boolean isComplete() {
    return complete;
  }

  public void setComplete() {
    if (nobs > 0)
      this.complete = true;
  }

  @Override
  public String toString() {
    return "CollectionInfo{" +
            "bbox=" + bbox +
            ", dateRange=" + getCalendarDateRange(null) +
            ", nfeatures=" + nfeatures +
            ", nobs=" + nobs +
            ", complete=" + complete +
            '}';
  }
}
