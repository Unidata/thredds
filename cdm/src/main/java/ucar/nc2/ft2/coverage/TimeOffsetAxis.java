package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NCdumpW;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Description
 *
 * @author John
 * @since 8/13/2015
 */
public class TimeOffsetAxis extends CoverageCoordAxis1D {

  private final TimeHelper timeHelper;

  public TimeOffsetAxis(String name, String units, String description, DataType dataType, AxisType axisType, List<Attribute> attributes,
                           CoverageCoordAxis.DependenceType dependenceType, List<String> dependsOn, CoverageCoordAxis.Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                           double[] values, CoordAxisReader reader, boolean isSubset, TimeHelper timeHelper ) {

    super(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader, isSubset);
    this.timeHelper = timeHelper;
  }

  /*
  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
    f.format("%s runtime= %s%n", indent, runCoord.getName());
  } */

  // for now just (runtime, offset) or (runtime=1, time)
  @Override
  public CoverageCoordAxis subset(SubsetParams params) {
    CoordAxisHelper helper = new CoordAxisHelper(this);
    Double dval = params.getDouble(SubsetParams.timeOffset);
    if (dval != null) {
      return helper.subsetClosest(dval);
    }

    // LOOK could do offset min, max

    // for the moment, just deal with the case of (runtime=1, time)

    /* which runtime ?
    CoverageCoordAxis1D runtimeSubset = null;
    if (params.isTrue(SubsetParams.latestRuntime)) {
      runtimeSubset = (CoverageCoordAxis1D) new CoordAxisHelper(runCoord).subsetLatest();
    } else {
      CalendarDate rundate = (CalendarDate) params.get(SubsetParams.runtime);
      if (rundate != null)
        runtimeSubset = (CoverageCoordAxis1D) new CoordAxisHelper(runCoord).subsetLatest();
    }
    if (runtimeSubset == null)
      throw new IllegalArgumentException("TimeOffsetAxis: only single runtime is allowed"); */

    // which time ?
    if (params.isTrue(SubsetParams.allTimes))
      return this;

    if (params.isTrue(SubsetParams.latestTime))
      return helper.subsetLatest();

    CalendarDate date = (CalendarDate) params.get(SubsetParams.time);
    if (date != null) {
      double offset = timeHelper.convert(date);
      return helper.subsetClosest(offset);
    }

    CalendarDateRange dateRange = (CalendarDateRange) params.get(SubsetParams.timeRange);
    if (dateRange != null) {
      double min = timeHelper.convert( dateRange.getStart());
      double max = timeHelper.convert( dateRange.getEnd());
      return helper.subset(min, max);
    }

    // if no time parameter, use the first offset in the latest run
    return helper.subsetValues(0, 0);
  }

}
