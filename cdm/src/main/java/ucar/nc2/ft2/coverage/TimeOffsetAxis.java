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
import java.util.List;

/**
 * A new way to handle 2D time, an orthogonal axis with offset values. The time can be calculated with both the runtime with the offset.
 *
 * @author John
 * @since 8/13/2015
 */
public class TimeOffsetAxis extends CoverageCoordAxis1D {
  // private CoverageCoordAxis1D runAxis;
  // private final String reftimeName;

  public TimeOffsetAxis(String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer attributes,
                           CoverageCoordAxis.DependenceType dependenceType, String dependsOn, CoverageCoordAxis.Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                           double[] values, CoordAxisReader reader, boolean isSubset ) {

    super(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader, isSubset);
  }

  public boolean isTime2D() {
     return true;
   }

  /*
  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
    f.format("%s runtime= %s%n", indent, runCoord.getName());
  } */

  // for now just (runtime, offset) or (runtime=1, time)
  // note helper is returning a CoverageCoordAxis1D< not a TimeOffsetAxis
  public CoverageCoordAxis subset(SubsetParams params, CoverageCoordAxis1D runtimeSubset) {
    CoordAxisHelper helper = new CoordAxisHelper(this);
    Double dval = params.getDouble(SubsetParams.timeOffset);
    if (dval != null) {
      return helper.subsetClosest(dval);
    }

    // LOOK could do offset min, max

    // for the moment, just deal with the case of (runtime=1, time)
    assert runtimeSubset.getNcoords() == 1;
    //int runIdx = runtimeSubset.getMinIndex();
    //double val = runAxis.getCoord(runIdx);   // not sure runAxis is needed. maybe use runtimeSubset
    //CalendarDate runDate = runAxis.makeDate(val);

    double val2 = runtimeSubset.getCoord(0);   // not sure runAxis is needed. maybe use runtimeSubset
    CalendarDate runDate2 = runtimeSubset.makeDate(val2);

    // which time ?
    if (params.isTrue(SubsetParams.allTimes))
      return this;

    if (params.isTrue(SubsetParams.latestTime))
      return helper.subsetLatest();

    CalendarDate dateWanted = (CalendarDate) params.get(SubsetParams.time);
    if (dateWanted != null) {       // convertFrom, convertTo
      double offset = runtimeSubset.getOffsetInTimeUnits(runDate2, dateWanted);
      return helper.subsetClosest(offset);
    }

    CalendarDateRange dateRange = (CalendarDateRange) params.get(SubsetParams.timeRange);
    if (dateRange != null) {
      double min = runtimeSubset.getOffsetInTimeUnits(runDate2, dateRange.getStart());
      double max = runtimeSubset.getOffsetInTimeUnits(runDate2, dateRange.getEnd());
      return helper.subset(min, max);
    }

    // if no time parameter, use the first offset in the latest run
    return helper.subsetValues(0, 0);
  }

  public CalendarDate makeDate(CalendarDate runDate, double val) {
    double offset = timeHelper.getOffsetInTimeUnits(timeHelper.getRefDate(), runDate);
    return timeHelper.makeDate(offset+val);
  }


  TimeOffsetAxis subset(int ncoords, double start, double end, double[] values) {
    return new TimeOffsetAxis(this.getName(), this.getUnits(), this.getDescription(), this.getDataType(), this.getAxisType(),
            this.getAttributeContainer(), this.getDependenceType(), this.getDependsOn(), this.getSpacing(),
            ncoords, start, end, this.getResolution(), values, this.reader, true);
  }

}
