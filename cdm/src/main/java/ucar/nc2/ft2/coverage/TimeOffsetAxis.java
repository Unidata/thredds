/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.Optional;

/**
 * A new way to handle 2D time, a runtime axis with orthogonal offset values, so time = (runtime x offset).
 * This class represents the offset values, which must be the same for each runtime.
 * A Time2DCoordSys has a runtime and a TimeOffsetAxis, and manages the 2D time.
 * @author John
 * @since 8/13/2015
 */
public class TimeOffsetAxis extends CoverageCoordAxis1D {

  public TimeOffsetAxis( CoverageCoordAxisBuilder builder) {
    super(builder);
  }

  @Override
  public CoverageCoordAxis copy() {
    return new TimeOffsetAxis(new CoverageCoordAxisBuilder(this));
  }

  // normal case already handled, this is the case where a time has been specified, and only one runtime
  public Optional<TimeOffsetAxis> subsetFromTime(SubsetParams params, CalendarDate runDate) {
    CoordAxisHelper helper = new CoordAxisHelper(this);
    CoverageCoordAxisBuilder builder = null;
    if (params.isTrue(SubsetParams.timePresent)) {
      double offset = getOffsetInTimeUnits(runDate, CalendarDate.present());
      builder = helper.subsetClosest(offset);
    }

    CalendarDate dateWanted = (CalendarDate) params.get(SubsetParams.time);
    if (dateWanted != null) {                           // convertFrom, convertTo
      double offset = getOffsetInTimeUnits(runDate, dateWanted);
      builder =  helper.subsetClosest(offset);
    }

    Integer stride = (Integer) params.get(SubsetParams.timeStride);
    if (stride == null || stride < 0) stride = 1;

    CalendarDateRange dateRange = (CalendarDateRange) params.get(SubsetParams.timeRange);
    if (dateRange != null) {
      double min = getOffsetInTimeUnits(runDate, dateRange.getStart());
      double max = getOffsetInTimeUnits(runDate, dateRange.getEnd());
      Optional<CoverageCoordAxisBuilder> buildero =  helper.subset(min, max, stride);
      if (buildero.isPresent()) builder = buildero.get();
      else return Optional.empty(buildero.getErrorMessage());
    }

    assert (builder != null);

    // all the offsets are reletive to rundate
    builder.setReferenceDate(runDate);
    return Optional.of(new TimeOffsetAxis(builder));
  }

  public CalendarDate makeDate(CalendarDate runDate, double val) {
    double offset = timeHelper.getOffsetInTimeUnits(timeHelper.getRefDate(), runDate);
    return timeHelper.makeDate(offset + val);
  }

  @Override
  public Optional<CoverageCoordAxis> subset(SubsetParams params) {
    Optional<CoverageCoordAxisBuilder> buildero = subsetBuilder(params);
    return !buildero.isPresent() ? Optional.empty(buildero.getErrorMessage()) : Optional.of(new TimeOffsetAxis(buildero.get()));
  }

  @Override
  public Optional<CoverageCoordAxis> subset(double minValue, double maxValue, int stride) {
    CoordAxisHelper helper = new CoordAxisHelper(this);
    Optional<CoverageCoordAxisBuilder> buildero = helper.subset(minValue, maxValue, stride);
    return !buildero.isPresent() ? Optional.empty(buildero.getErrorMessage()) : Optional.of(new TimeOffsetAxis(buildero.get()));
  }

}
