/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.NCdumpW;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;

import java.util.Formatter;
import java.util.List;

/**
 * FmrcTimeAxis2D: time(runtime, time)
 *
 * @author caron
 * @since 7/15/2015
 */
public class FmrcTimeAxis2D extends CoverageCoordAxis {
  private int[] shape;
  private CoverageCoordAxis1D runCoord;

  public FmrcTimeAxis2D(String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer atts,
                           DependenceType dependenceType, List<String> dependsOn, Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                           double[] values, CoordAxisReader reader, boolean isSubset) {

    super(name, units, description, dataType, axisType, atts, dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader, isSubset);
  }

  @Override
  protected void setDataset(CoordSysContainer dataset) {
    if (shape != null) throw new RuntimeException("Cant change axis once set");
    shape = new int[2];
    String axisName = dependsOn.get(0);
    CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
    shape[0] = axis.getNcoords();
    shape[1] = ncoords / shape[0];

    assert axis instanceof CoverageCoordAxis1D;
    assert axis.getAxisType() == AxisType.RunTime;
    runCoord = (CoverageCoordAxis1D) axis;
  }

  @Override
  public int[] getShape() {
    return shape;
  }

  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
    f.format("%s  %s%n", indent, Misc.showInts(shape));
    Array data = getCoordsAsArray();
    f.format("%s%n", NCdumpW.toString(data, getName()+" values", null));
  }

  //@Override
  //public FmrcTimeAxis2D copy() {
  //  return new FmrcTimeAxis2D(name, units, description, dataType, axisType, attributes.getAttributes(), dependenceType,
  //                        dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader);
  //}

  @Override
  public CoverageCoordAxis subset(SubsetParams params) {
    CoordAxisHelper helper = new CoordAxisHelper(runCoord);
    int run_index = -1;

    if (params.isTrue(SubsetParams.latestRuntime))
      run_index = runCoord.getNcoords()-1;
    else {
      CalendarDate rundate = (CalendarDate) params.get(SubsetParams.runtime);
      if (rundate != null) {
        double rundateTarget = runCoord.convert(rundate);
        run_index = helper.findCoordElementBounded(rundateTarget, CoordAxisHelper.Mode.closest);  // LOOK Bounded
      }
    }

    if (run_index >= 0) {
      CoverageCoordAxis1D time1D = getTimeAxisForRun(run_index);
      return time1D.subset(params);
    }

    return this;
  }

  @Override
  public FmrcTimeAxis2D subsetDependent(CoverageCoordAxis1D from) {
    return null; // LOOK
  }


  @Override
  public FmrcTimeAxis2D subset(double minValue, double maxValue) {
    return null; // LOOK
  }

  @Override
  public Array getCoordsAsArray() {
    double[] values = getValues();
    return Array.factory(DataType.DOUBLE, shape, values);
  }

  @Override
  public Array getCoordBoundsAsArray() {
    double[] values = getValues();
    int[] shapeB = new int[3];
    System.arraycopy(shape, 0, shapeB, 0, 2);
    shapeB[2] = 2;
    return Array.factory(DataType.DOUBLE, shapeB, values);
  }

  public CoverageCoordAxis1D getTimeAxisForRun(CalendarDate rundate) {
    double rundateTarget = runCoord.convert(rundate);
    int run_index = new CoordAxisHelper(runCoord).findCoordElementBounded(rundateTarget, CoordAxisHelper.Mode.closest);  // LOOK Bounded
    return (run_index < 0) ? null : getTimeAxisForRun(run_index);
  }

  public CoverageCoordAxis1D getTimeAxisForRun(int run_index) {
    if (spacing == Spacing.irregularPoint) {
      Array data = getCoordsAsArray();
      Array subset = data.slice(0, run_index);

      int count = 0;
      int n = (int) subset.getSize();
      double[] values = new double[n];
      while (subset.hasNext())
        values[count++] = subset.nextDouble();

      return new CoverageCoordAxis1D(name, units, description, dataType, axisType,
              AttributeContainerHelper.filter(attributes, "_Coordinate"),
              dependenceType, dependsOn, spacing, n, values[0], values[n - 1],
              0.0, values, reader, true);
    }

    if (spacing == Spacing.discontiguousInterval) {
      Array data = getCoordBoundsAsArray();
      Array subset = data.slice(0, run_index);

      int count = 0;
      int n = (int) subset.getSize();
      double[] values = new double[n];
      while (subset.hasNext())
        values[count++] = subset.nextDouble();

      return new CoverageCoordAxis1D(name, units, description, dataType, axisType,
              AttributeContainerHelper.filter(attributes, "_Coordinate"),
              dependenceType, dependsOn, spacing, n/2, values[0], values[n - 1],
              0.0, values, reader, true);
    }

    return null;
  }


}
