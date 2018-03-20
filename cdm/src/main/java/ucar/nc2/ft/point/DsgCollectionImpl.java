/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import ucar.nc2.Variable;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Common methods for DsgFeatureCollection.
 *
 * @author caron
 * @since 9/25/2015.
 */
public abstract class DsgCollectionImpl implements DsgFeatureCollection {

  protected String name;
  protected CalendarDateUnit timeUnit;
  protected String altUnits;
  protected CollectionInfo info;
  protected List<Variable> extras; // variables needed to make CF/DSG writing work

  protected DsgCollectionImpl(String name, CalendarDateUnit timeUnit, String altUnits) {
    this.name = name;
    this.timeUnit = timeUnit;
    this.altUnits = altUnits;
  }

  @Nonnull
  @Override
  public String getName() {
    return name;
  }

  @Nonnull
  @Override
  public CalendarDateUnit getTimeUnit() {
    return timeUnit;
  }

  @Nullable
  @Override
  public String getAltUnits() {
    return altUnits;
  }

  @Nonnull
  public List<Variable> getExtraVariables() { return (extras == null) ? new ArrayList<>() : extras; }

  @Override
  public int size() {
    return (info == null) ? -1 : info.nfeatures;
  }

  public int getNobs() {
    return (info == null) ? -1 : info.nobs;
  }

  @Nullable
  @Override
  public CalendarDateRange getCalendarDateRange() {
    return (info == null) ? null : info.getCalendarDateRange(timeUnit);
  }

  @Nullable
  @Override
  public ucar.unidata.geoloc.LatLonRect getBoundingBox() {
    return (info == null) ? null : info.bbox;
  }

  @Nonnull
  public CollectionInfo getInfo() { // LOOK exposes mutable fields
    if (info == null)
      info = new CollectionInfo();
    return info;
  }
}
