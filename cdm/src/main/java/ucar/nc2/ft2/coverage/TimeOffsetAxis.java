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
package ucar.nc2.ft2.coverage;

import ucar.ma2.DataType;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

import javax.annotation.Nonnull;

/**
 * A new way to handle 2D time, an orthogonal axis with offset values. The time can be calculated with both the runtime with the offset.
 *
 * @author John
 * @since 8/13/2015
 */
public class TimeOffsetAxis extends CoverageCoordAxis1D {

  public TimeOffsetAxis( CoverageCoordAxisBuilder builder) {
    super(builder);
  }

  /* public TimeOffsetAxis(String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer attributes,
                        CoverageCoordAxis.DependenceType dependenceType, String dependsOn, CoverageCoordAxis.Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                        double[] values, CoordAxisReader reader, boolean isSubset) {

    super(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader, isSubset);
  } */

  public boolean isTime2D() {
    return true;
  }

  // normal case already handled, this is the case where a time has been specified, and only one runtime
  @Nonnull
  public TimeOffsetAxis subsetFromTime(SubsetParams params, CalendarDate runDate) {
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

    CalendarDateRange dateRange = (CalendarDateRange) params.get(SubsetParams.timeRange);
    if (dateRange != null) {
      double min = getOffsetInTimeUnits(runDate, dateRange.getStart());
      double max = getOffsetInTimeUnits(runDate, dateRange.getEnd());
      builder =  helper.subset(min, max); // LOOK no stride
    }

    if (builder == null)
      throw new IllegalStateException();

    // all the offsets are reletive to rundate
    builder.setReferenceDate(runDate);
    return new TimeOffsetAxis(builder);
  }

  public CalendarDate makeDate(CalendarDate runDate, double val) {
    double offset = timeHelper.getOffsetInTimeUnits(timeHelper.getRefDate(), runDate);
    return timeHelper.makeDate(offset + val);
  }

  @Override
  public CoverageCoordAxis subset(SubsetParams params) {
    return new TimeOffsetAxis( subsetBuilder(params));
  }

  @Override
  public TimeOffsetAxis subset(double minValue, double maxValue) {
    CoordAxisHelper helper = new CoordAxisHelper(this);
    return new TimeOffsetAxis(helper.subset(minValue, maxValue));
  }

  /*
  TimeOffsetAxis subset(int ncoords, double start, double end, double[] values) {
    return new TimeOffsetAxis(this.getName(), this.getUnits(), this.getDescription(), this.getDataType(), this.getAxisType(),
            this.getAttributeContainer(), this.getDependenceType(), this.getDependsOn(), this.getSpacing(),
            ncoords, start, end, this.getResolution(), values, this.reader, true);
  }

  TimeOffsetAxis subset(String dependsOn, Spacing spacing, int npoints, double[] values) {
    assert values != null;
    return new TimeOffsetAxis(this.getName(), this.getUnits(), this.getDescription(), this.getDataType(), this.getAxisType(), this.getAttributeContainer(),
            dependsOn == null ? this.getDependenceType() : DependenceType.dependent, dependsOn,
            spacing, npoints, 0, 0, this.getResolution(), values, null, true);
  }
   */

}
