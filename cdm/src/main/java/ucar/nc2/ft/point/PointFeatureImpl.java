/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.EarthLocation;

/**
 * Abstract superclass for implementations of PointFeature.
 * Concrete subclass must implement getFeatureData() and getDataAll();
 *
 * @author caron
 * @since Feb 29, 2008
 */
public abstract class PointFeatureImpl implements PointFeature, Comparable<PointFeature> {
  protected DsgFeatureCollection dsg;
  protected EarthLocation location;
  protected double obsTime, nomTime;
  protected CalendarDateUnit timeUnit;

  protected PointFeatureImpl(DsgFeatureCollection dsg, CalendarDateUnit timeUnit) {
    this.dsg = Preconditions.checkNotNull(dsg, "dgs == null");
    this.timeUnit = timeUnit;
  }

  public PointFeatureImpl(DsgFeatureCollection dsg, EarthLocation location, double obsTime, double nomTime, CalendarDateUnit timeUnit) {
    this.dsg = Preconditions.checkNotNull(dsg, "dgs == null");
    this.location = Preconditions.checkNotNull(location, "location == null");
    this.obsTime = obsTime;
    this.nomTime = (nomTime == 0) ? obsTime : nomTime; // LOOK temp kludge until protobuf accepts NaN as defaults
    this.timeUnit = Preconditions.checkNotNull(timeUnit, "timeUnit == null");
  }

  @Nonnull
  @Override
  public DsgFeatureCollection getFeatureCollection() {
    return dsg;
  }

  @Nonnull
  @Override
  public EarthLocation getLocation() { return location; }

  @Override
  public double getNominalTime() { return nomTime; }

  @Override
  public double getObservationTime() { return obsTime; }

  public String getDescription() {
    return location.toString(); // ??
  }

  @Nonnull
  @Override
  public CalendarDate getObservationTimeAsCalendarDate() {
    return timeUnit.makeCalendarDate(getObservationTime());
  }

  @Nonnull
  @Override
  public CalendarDate getNominalTimeAsCalendarDate() {
    return timeUnit.makeCalendarDate(getNominalTime());
  }

  @Override
  public int compareTo(@Nonnull PointFeature other) {
    if (obsTime < other.getObservationTime()) return -1;
    if (obsTime > other.getObservationTime()) return 1;
    return 0;
  }

  @Override
  public String toString() {
    return "PointFeatureImpl{" +
        "location=" + location +
        ", obsTime=" + obsTime +
        ", nomTime=" + nomTime +
        ", timeUnit=" + timeUnit +
        '}';
  }
}
