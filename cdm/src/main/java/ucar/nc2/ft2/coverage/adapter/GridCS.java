/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft2.coverage.adapter;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

import java.util.ArrayList;
import java.util.List;

/**
 * Grid Coordinate System Implementation
 * A Grid has all 1D coordinates.
 *
 * @author John
 * @since 12/25/12
 */
public class GridCS extends DtCoverageCS {

  GridCS(DtCoverageCSBuilder builder) {
    super(builder);
  }

  @Override
  public boolean isRegularSpatial() {
    return getXHorizAxis().isRegular() && getYHorizAxis().isRegular();
  }

  @Override
  public CoordinateAxis1D getXHorizAxis() {
    return (CoordinateAxis1D) super.getXHorizAxis();
  }

  @Override
  public CoordinateAxis1D getYHorizAxis() {
    return (CoordinateAxis1D) super.getYHorizAxis();
  }

  // LOOK another possibility is a scalar runtime and a 1D time offset

  @Override
  public CoordinateAxis1DTime getTimeAxis() {
    return (CoordinateAxis1DTime) super.getTimeAxis();
  }

  /*
   @Override
 public List<CalendarDate> getCalendarDates() {
    if (getTimeAxis() != null)
      return getTimeAxis().getCalendarDates();

    else if (getRunTimeAxis() != null)
      return getRunTimeAxis().getCalendarDates();

    else
      return new ArrayList<>();
  }
  */

  @Override
  public CalendarDateRange getCalendarDateRange() {
    if (getTimeAxis() != null)
      return getTimeAxis().getCalendarDateRange();

    else if (getRunTimeAxis() != null)
      return getRunTimeAxis().getCalendarDateRange();

    else
      return null;
  }


}
