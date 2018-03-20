/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NCdumpW;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;
import ucar.nc2.util.Optional;

import javax.annotation.Nonnull;
import java.util.Formatter;

/**
 * Swath TimeAxis: time(scan, xscan)
 * values will contain scan * xscan values
 *
 * @author caron
 * @since 3/19/2016.
 */
public class TimeAxis2DSwath extends CoverageCoordAxis {

  // can only be set once, needed for subsetting
  private int[] shape;
  public TimeAxis2DSwath(CoverageCoordAxisBuilder builder) {
    super(builder);
  }

  @Override
  protected void setDataset(CoordSysContainer dataset) {
    if (shape != null) throw new RuntimeException("Cant change axis, once dataset is set");
    shape = new int[2];
    assert dependsOn.size() == 2;
    CoverageCoordAxis axis1 = dataset.findCoordAxis(dependsOn.get(0));
    if (axis1 == null)
      throw new IllegalStateException("TimeAxis2DSwath cant find axis with name "+dependsOn.get(0));

    CoverageCoordAxis axis2 = dataset.findCoordAxis(dependsOn.get(1));
    if (axis2 == null)
      throw new IllegalStateException("TimeAxis2DSwath cant find axis with name "+dependsOn.get(1));

    shape[0] = axis1.getNcoords();
    shape[1] = axis2.getNcoords();
    assert shape[0] * shape[1] == this.getNcoords();
  }

  @Override
  public CoverageCoordAxis copy() {
    return new TimeAxis2DSwath(new CoverageCoordAxisBuilder(this));
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
      return Optional.of(new TimeAxis2DSwath(new CoverageCoordAxisBuilder(this)));

    /* CalendarDate rundate = (CalendarDate) params.get(SubsetParams.runtime);
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
    } */

    // no subsetting needed
    return Optional.of(new TimeAxis2DSwath(new CoverageCoordAxisBuilder(this)));
  }

  @Override
  public Optional<CoverageCoordAxis> subset(double minValue, double maxValue, int stride) { // LOOK not implemented, maybe illegal ??
    return Optional.of(new TimeAxis2DSwath(new CoverageCoordAxisBuilder(this)));
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
}
