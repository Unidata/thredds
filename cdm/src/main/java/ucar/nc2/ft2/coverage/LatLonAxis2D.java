/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import ucar.ma2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;
import ucar.nc2.util.Optional;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * LatLon axes : used by lat(y,x) and lon(y,x)
 * An instance represents just one of lat or lon.
 * HorizCoordSys manages the two, usually you want to use that.
 *
 * @author caron
 * @since 7/15/2015
 */
public class LatLonAxis2D extends CoverageCoordAxis {

  // can only be set once
  private int[] shape;        // y, x
  private Object userObject;

  public LatLonAxis2D(CoverageCoordAxisBuilder builder) {
    super(builder);
    this.shape = builder.shape;
    this.userObject = builder.userObject;
  }

  @Override
  protected void setDataset(CoordSysContainer dataset) {
    if (this.shape != null) return; // set in constructor

    List<CoverageCoordAxis> dependentAxes = new ArrayList<>();
    int[] shape = new int[2];
    int count = 0;
    for (String axisName : dependsOn) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
      if (axis == null) {
        if (this.shape == null) // ok is hape has already been set
          throw new IllegalStateException("LatLonAxis2D cant find axis " + axisName);
      } else {
        shape[count] = axis.getNcoords();
        dependentAxes.add(axis);
      }
    }
    this.shape = shape;
  }

  @Override
  public CoverageCoordAxis copy() {
    return new LatLonAxis2D(new CoverageCoordAxisBuilder(this));
  }

  @Override
  // y, x
  public int[] getShape() {
    return shape;
  }

  public Object getUserObject() {
    return userObject;
  }

  public List<RangeIterator> getRanges() {
    List<RangeIterator> result = new ArrayList<>();
    result.add(Range.make(AxisType.Lat.toString(), shape[0]));  // LOOK wrong, need subset Range
    result.add(Range.make(AxisType.Lon.toString(), shape[1]));
    return result;
  }

  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
    f.format("%s  shape=[%s]%n", indent, Misc.showInts(shape));
  }

  public double getCoord(int yindex, int xindex) {
    // assume values hold 2D coord
    loadValuesIfNeeded();
    int idx = yindex * shape[1] + xindex;
    return values[idx];
  }

  @Override
  public Optional<CoverageCoordAxis> subset(SubsetParams params) {  // Handled in HorzCoordSys2D
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<CoverageCoordAxis> subset(double minValue, double maxValue, int stride) { // Handled in HorzCoordSys2D
    throw new UnsupportedOperationException();
  }

  @Override
  @Nonnull
  public Optional<CoverageCoordAxis> subsetDependent(CoverageCoordAxis1D from) { // LOOK not implemented
    throw new UnsupportedOperationException();
  }

  @Override
  public Array getCoordsAsArray() {
    double[] values = getValues();
    return Array.factory(DataType.DOUBLE, shape, values);
  }

  @Override
  public Array getCoordBoundsAsArray() { // LOOK do we want to cache this ?
    return CoordinateAxis2D.makeEdges((ArrayDouble.D2) getCoordsAsArray()); // makeXEdges same as makeYEdges
  }

  public LatLonAxis2D subset(RangeIterator rangex, RangeIterator rangey) {
    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(this);

    // subset the values
    double[] values = getValues();
    int nx = rangex.length();
    int ny = rangey.length();
    double[] svalues = new double[nx * ny];
    int count = 0;
    for (int y : rangey)
      for (int x : rangex)
        svalues[count++] = values[y * nx + x];

    builder.values = svalues;
    builder.isSubset = true;
    builder.ncoords = nx * ny;
    builder.shape = new int[]{ny, nx};

    return new LatLonAxis2D(builder);
  }

}
