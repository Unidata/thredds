package ucar.nc2.ft2.coverage;

import ucar.ma2.DataType;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import java.util.List;

/**
 * Description
 *
 * @author John
 * @since 8/13/2015
 */
public class TimeOffsetAxis extends CoverageCoordAxis1D {
  private CoverageCoordAxis1D runAxis;
  private final String reftimeName;

  public TimeOffsetAxis(String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer attributes,
                           CoverageCoordAxis.DependenceType dependenceType, List<String> dependsOn, CoverageCoordAxis.Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                           double[] values, CoordAxisReader reader, boolean isSubset, String reftimeName ) {

    super(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader, isSubset);
    this.reftimeName = reftimeName;
  }

  @Override
  protected void setDataset(CoordSysContainer dataset) {
    CoverageCoordAxis axis = dataset.findCoordAxis(reftimeName);
    assert axis != null;
    assert axis instanceof CoverageCoordAxis1D;
    assert axis.getAxisType() == AxisType.RunTime;
    runAxis = (CoverageCoordAxis1D) axis;
  }

  public CoverageCoordAxis1D getRunAxis() {
    return runAxis;
  }

  public void setRunAxis(CoverageCoordAxis1D runAxis) {
    if (this.runAxis != null) throw new RuntimeException("Cant change runAxis once set");
    this.runAxis = runAxis;
    this.runAxis.setIsTime2D();
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

  TimeOffsetAxis subset(int count, double start, double end, double[] values) {
    return new TimeOffsetAxis(this.getName(), this.getUnits(), this.getDescription(), this.getDataType(), this.getAxisType(),
            this.getAttributeContainer(), this.getDependenceType(), this.getDependsOnList(), this.getSpacing(),
            count, start, end, this.getResolution(), values, this.reader, true, reftimeName);
  }

}
