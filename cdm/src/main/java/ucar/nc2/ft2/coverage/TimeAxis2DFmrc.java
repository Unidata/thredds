/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.NCdumpW;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;
import ucar.nc2.util.Optional;

import javax.annotation.Nonnull;
import java.util.Formatter;

/**
 * Fmrc TimeAxis: time(runtime, time)
 * values will contain nruns * ntimes values
 *
 * @author caron
 * @since 7/15/2015
 */
public class TimeAxis2DFmrc extends CoverageCoordAxis {

  // can only be set once, needed for subsetting
  private int[] shape;
  private CoverageCoordAxis1D runCoord;

  public TimeAxis2DFmrc(CoverageCoordAxisBuilder builder) {
    super(builder);
  }

  @Override
  protected void setDataset(CoordSysContainer dataset) {
    if (shape != null) throw new RuntimeException("Cant change axis once set");
    shape = new int[2];
    String runtimeName = dependsOn.get(0);
    CoverageCoordAxis runtime = dataset.findCoordAxis(runtimeName);
    if (runtime == null)
      throw new IllegalStateException("FmrcTimeAxis2D cant find runtime axis with name "+runtimeName);

    assert runtime instanceof CoverageCoordAxis1D;
    assert runtime.getAxisType() == AxisType.RunTime;
    runCoord = (CoverageCoordAxis1D) runtime;

    shape[0] = runtime.getNcoords();
    shape[1] = ncoords / shape[0];
  }

  @Override
  public CoverageCoordAxis copy() {
    return new TimeAxis2DFmrc(new CoverageCoordAxisBuilder(this));
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
    f.format("%s%n", NCdumpW.toString(data, getName() + " values", null));
  }

  @Override
  public Optional<CoverageCoordAxis> subset(SubsetParams params) {
    if (params == null)
      return Optional.of(new TimeAxis2DFmrc(new CoverageCoordAxisBuilder(this)));

    CalendarDate rundate = (CalendarDate) params.get(SubsetParams.runtime);
    boolean runtimeAll = (Boolean) params.get(SubsetParams.runtimeAll);
    boolean latest = (rundate == null) && !runtimeAll; // default is latest

    int run_index = -1;
    if (latest) {
      run_index = runCoord.getNcoords() - 1;

    } else if (rundate != null){
      double rundateTarget = runCoord.convert(rundate);
      CoordAxisHelper helper = new CoordAxisHelper(runCoord);
      run_index = helper.findCoordElement(rundateTarget, true);  // LOOK Bounded
    }
    if (run_index >= 0) {
      CoverageCoordAxis1D time1D = getTimeAxisForRun(run_index);
      return time1D.subset(params);
    }

    // no subsetting needed
    return Optional.of(new TimeAxis2DFmrc(new CoverageCoordAxisBuilder(this)));
  }

  @Override
  public Optional<CoverageCoordAxis> subset(double minValue, double maxValue, int stride) { // LOOK not implemented, maybe illegal ??
    return Optional.of(new TimeAxis2DFmrc(new CoverageCoordAxisBuilder(this)));
  }

  @Override
  @Nonnull
  public Optional<CoverageCoordAxis> subsetDependent(CoverageCoordAxis1D from) { // LOOK not implemented, maybe illegal ??
    throw new UnsupportedOperationException();
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
    int run_index = new CoordAxisHelper(runCoord).findCoordElement(rundateTarget, false);  // LOOK not Bounded
    return (run_index < 0 || run_index >= runCoord.getNcoords()) ? null : getTimeAxisForRun(run_index);
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

      CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(name, units, description, dataType, axisType,
              AttributeContainerHelper.filter(attributes, "_Coordinate"),
              dependenceType, getDependsOn(), spacing, n, values[0], values[n - 1],
              0.0, values, reader);
      builder.setIsSubset(true);
      return new CoverageCoordAxis1D(builder);
    }

    if (spacing == Spacing.discontiguousInterval) {
      Array data = getCoordBoundsAsArray();
      Array subset = data.slice(0, run_index);

      int count = 0;
      int n = (int) subset.getSize();
      double[] values = new double[n];
      while (subset.hasNext())
        values[count++] = subset.nextDouble();

      CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(name, units, description, dataType, axisType,
              AttributeContainerHelper.filter(attributes, "_Coordinate"),
              dependenceType, getDependsOn(), spacing, n / 2, values[0], values[n - 1],
              0.0, values, reader);

      builder.setIsSubset(true);
      return new CoverageCoordAxis1D(builder);
    }

    // LOOK what about the other cases ??
    return null;
  }


}
