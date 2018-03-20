/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A 2-dimensional numeric Coordinate Axis. Must be invertible meaning, roughly, that
 *   if you draw lines connecting the points, none would cross.
 *
 * @see CoordinateAxis#factory
 * @author john caron
 */

public class CoordinateAxis2D extends CoordinateAxis {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinateAxis2D.class);
  static private final boolean debug = false;

  private ArrayDouble.D2 coords = null;  // LOOK maybe optional for large arrays, or maybe eliminate all together, and read each time ??

  /**
   * Create a 2D coordinate axis from an existing VariableDS
   *
   * @param ncd the containing dataset
   * @param vds create it from here
   */
  public CoordinateAxis2D(NetcdfDataset ncd, VariableDS vds) {
    super(ncd, vds);
    isContiguous = false;
  }

  // for section and slice
  @Override
  protected Variable copy() {
    return new CoordinateAxis2D(this.ncd, this);
  }

  /**
   * Get the coordinate value at the i, j index.
   *
   * @param i index 0 (fastest varying, right-most)
   * @param j index 1
   * @return midpoint.get(j, i).
   */
  public double getCoordValue(int j, int i) {
    if (coords == null) doRead();
    return coords.get(j, i);
  }

  private void doRead() {
    Array data;
    try {
      data = read();
      // if (!hasCachedData()) setCachedData(data, false); //cache data for subsequent reading
    } catch (IOException ioe) {
      log.error("Error reading coordinate values " + ioe);
      throw new IllegalStateException(ioe);
    }

    if (data.getRank() != 2)
      throw new IllegalArgumentException("must be 2D");
    if (debug)
      System.out.printf("Coordinate2D read%n");

    coords = (ArrayDouble.D2) Array.factory(DataType.DOUBLE, data.getShape(), data.get1DJavaArray(DataType.DOUBLE));

    if (this.axisType == AxisType.Lon)
      makeConnectedLon(coords);
  }

  private boolean isInterval;
  private boolean intervalWasComputed;

  public boolean isInterval() {
    if (!intervalWasComputed)
      isInterval = computeIsInterval();
    return isInterval;
  }

  private void makeConnectedLon(ArrayDouble.D2 mid) {

    int[] shape = mid.getShape();
    int ny = shape[0];
    int nx = shape[1];

    // first row
    double connect = mid.get(0, 0);
    for (int i = 1; i < nx; i++) {
      connect = connectLon(connect, mid.get(0, i));
      mid.set(0, i, connect);
    }

    // other rows
    for (int j = 1; j < ny; j++) {
      connect = mid.get(j - 1, 0);
      for (int i = 0; i < nx; i++) {
        connect = connectLon(connect, mid.get(j, i));
        mid.set(j, i, connect);
      }
    }

  }

  private static final double MAX_JUMP = 100.0; // larger than you would ever expect

  static private double connectLon(double connect, double val) {
    if (Double.isNaN(connect)) return val;
    if (Double.isNaN(val)) return val;

    double diff = val - connect;
    if (Math.abs(diff) < MAX_JUMP) return val; // common case fast
    // we have to add or subtract 360
    double result = diff > 0 ? val - 360 : val + 360;
    double diff2 = connect - result;
    if ((Math.abs(diff2)) < Math.abs(diff))
      val = result;
    return val;
  }


  /**
   * Get the coordinate values as a 1D double array, in canonical order.
   *
   * @return coordinate values
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double[] getCoordValues() {
    if (coords == null) doRead();
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis2D.getCoordValues() on non-numeric");
    return (double[]) coords.get1DJavaArray(DataType.DOUBLE);
  }

  /**
   * Create a new CoordinateAxis2D as a section of this CoordinateAxis2D.
   *
   * @param r1 the section on the first index
   * @param r2 the section on the second index
   * @return a section of this CoordinateAxis2D
   * @throws InvalidRangeException if specified Ranges are invalid
   */
  public CoordinateAxis2D section(Range r1, Range r2) throws InvalidRangeException {
    List<Range> section = new ArrayList<>();
    section.add(r1);
    section.add(r2);
    return (CoordinateAxis2D) section(section);
  }

  public ArrayDouble.D2 getCoordValuesArray() {
    if (coords == null) doRead();
    return coords;
  }

  /**
   * Only call if isInterval()
   *
   * @return bounds array pr null if not an interval
   */
  public ArrayDouble.D3 getCoordBoundsArray() {
    if (coords == null) doRead();
    return makeBoundsFromAux();
  }

  public ArrayDouble.D2 getEdges() {
    ArrayDouble.D2 mids = getCoordValuesArray();
    return makeEdges(mids);
  }

  /**
   * @deprecated use getEdges()
   */
  public ArrayDouble.D2 getXEdges() {
    ArrayDouble.D2 mids = getCoordValuesArray();
    return makeEdges(mids);
  }

  /**
   * @deprecated use getEdges()
   */
  public ArrayDouble.D2 getYEdges() {
    ArrayDouble.D2 mids = getCoordValuesArray();
    return makeEdges(mids);
  }

  /**
   * Normal case: do something reasonable in deciding on the edges when we have the midpoints of a 2D coordinate.
   *
   * @param midpoints values of midpoints with shape (ny, nx)
   * @return values of edges with shape (ny+1, nx+1)
   */
  static public ArrayDouble.D2 makeEdges(ArrayDouble.D2 midpoints) {
    int[] shape = midpoints.getShape();
    int ny = shape[0];
    int nx = shape[1];
    ArrayDouble.D2 edge = new ArrayDouble.D2(ny + 1, nx + 1);

    for (int y = 0; y < ny - 1; y++) {
      for (int x = 0; x < nx - 1; x++) {
        // the interior edges are the average of the 4 surrounding midpoints
        double xval = (midpoints.get(y, x) + midpoints.get(y, x + 1) + midpoints.get(y + 1, x) + midpoints.get(y + 1, x + 1)) / 4;
        edge.set(y + 1, x + 1, xval);
      }
      // extrapolate to exterior points
      edge.set(y + 1, 0, edge.get(y + 1, 1) - (edge.get(y + 1, 2) - edge.get(y + 1, 1)));
      edge.set(y + 1, nx, edge.get(y + 1, nx - 1) + (edge.get(y + 1, nx - 1) - edge.get(y + 1, nx - 2)));
    }

    // extrapolate to the first and last row
    for (int x = 0; x < nx + 1; x++) {
      edge.set(0, x, edge.get(1, x) - (edge.get(2, x) - edge.get(1, x)));
      edge.set(ny, x, edge.get(ny - 1, x) + (edge.get(ny - 1, x) - edge.get(ny - 2, x)));
    }

    return edge;
  }


  /**
   * Experimental: for WRF rotated (NMM "E") Grids
   *
   * @param midx x coordinates of midpoints
   * @return x coordinates of edges with shape (ny+2, nx+1)
   */
  static public ArrayDouble.D2 makeXEdgesRotated(ArrayDouble.D2 midx) {
    int[] shape = midx.getShape();
    int ny = shape[0];
    int nx = shape[1];
    ArrayDouble.D2 edgex = new ArrayDouble.D2(ny + 2, nx + 1);

    // compute the interior rows
    for (int y = 0; y < ny; y++) {
      for (int x = 1; x < nx; x++) {
        double xval = (midx.get(y, x - 1) + midx.get(y, x)) / 2;
        edgex.set(y + 1, x, xval);
      }
      edgex.set(y + 1, 0, midx.get(y, 0) - (edgex.get(y + 1, 1) - midx.get(y, 0)));
      edgex.set(y + 1, nx, midx.get(y, nx - 1) - (edgex.get(y + 1, nx - 1) - midx.get(y, nx - 1)));
    }

    // compute the first row
    for (int x = 0; x < nx; x++) {
      edgex.set(0, x, midx.get(0, x));
    }

    // compute the last row
    for (int x = 0; x < nx - 1; x++) {
      edgex.set(ny + 1, x, midx.get(ny - 1, x));
    }

    return edgex;
  }

  /**
   * Experimental: for WRF rotated (NMM "E") Grids
   *
   * @param midy y coordinates of midpoints
   * @return y coordinates of edges with shape (ny+2, nx+1)
   */
  static public ArrayDouble.D2 makeYEdgesRotated(ArrayDouble.D2 midy) {
    int[] shape = midy.getShape();
    int ny = shape[0];
    int nx = shape[1];
    ArrayDouble.D2 edgey = new ArrayDouble.D2(ny + 2, nx + 1);

    // compute the interior rows
    for (int y = 0; y < ny; y++) {
      for (int x = 1; x < nx; x++) {
        double yval = (midy.get(y, x - 1) + midy.get(y, x)) / 2;
        edgey.set(y + 1, x, yval);
      }
      edgey.set(y + 1, 0, midy.get(y, 0) - (edgey.get(y + 1, 1) - midy.get(y, 0)));
      edgey.set(y + 1, nx, midy.get(y, nx - 1) - (edgey.get(y + 1, nx - 1) - midy.get(y, nx - 1)));
    }

    // compute the first row
    for (int x = 0; x < nx; x++) {
      double pt0 = midy.get(0, x);
      double pt = edgey.get(2, x);

      double diff = pt0 - pt;
      edgey.set(0, x, pt0 + diff);
    }

    // compute the last row
    for (int x = 0; x < nx - 1; x++) {
      double pt0 = midy.get(ny - 1, x);
      double pt = edgey.get(ny - 1, x);

      double diff = pt0 - pt;
      edgey.set(ny + 1, x, pt0 + diff);
    }

    return edgey;
  }

  ///////////////////////////////////////////////////////////////////////////////
  // bounds calculations


  private ArrayDouble.D3 makeBoundsFromAux() {
    if (!computeIsInterval()) return null;

    Attribute boundsAtt = findAttributeIgnoreCase(CF.BOUNDS);
    if (boundsAtt == null) return null;

    String boundsVarName = boundsAtt.getStringValue();
    VariableDS boundsVar = (VariableDS) ncd.findVariable(getParentGroup(), boundsVarName);

    Array data;
    try {
      //boundsVar.setUseNaNs(false); // missing values not allowed
      data = boundsVar.read();
    } catch (IOException e) {
      log.warn("CoordinateAxis2D.makeBoundsFromAux read failed ", e);
      return null;
    }

    ArrayDouble.D3 bounds;
    assert (data.getRank() == 3) && (data.getShape()[2] == 2) : "incorrect shape data for variable " + boundsVar;
    if (data instanceof ArrayDouble.D3) {
      bounds = (ArrayDouble.D3) data;
    } else {
      bounds = (ArrayDouble.D3) Array.factory(DataType.DOUBLE, data.getShape());
      MAMath.copy(data, bounds);
    }

    return bounds;
  }

  private boolean computeIsInterval() {
    intervalWasComputed = true;

    Attribute boundsAtt = findAttributeIgnoreCase(CF.BOUNDS);
    if ((null == boundsAtt) || !boundsAtt.isString()) return false;
    String boundsVarName = boundsAtt.getStringValue();
    VariableDS boundsVar = (VariableDS) ncd.findVariable(getParentGroup(), boundsVarName);
    if (null == boundsVar) return false;
    if (3 != boundsVar.getRank()) return false;
    if (getDimension(0) != boundsVar.getDimension(0)) return false;
    if (getDimension(1) != boundsVar.getDimension(1)) return false;
    return 2 == boundsVar.getDimension(2).getLength();
  }

  ///////////////////////////////////////
  // time

  public CoordinateAxisTimeHelper getCoordinateAxisTimeHelper() {
    return new CoordinateAxisTimeHelper(getCalendarFromAttribute(), getUnitsString());
  }

  public int findTimeIndexFromCalendarDate(int run_idx, CalendarDate want) throws IOException, InvalidRangeException {
    CoordinateAxisTimeHelper helper = getCoordinateAxisTimeHelper();
    double wantOffset = helper.offsetFromRefDate(want);

    if (isInterval()) {
      ArrayDouble.D3 bounds = getCoordBoundsArray();
      if (bounds == null)
        throw new IllegalStateException("getCoordBoundsArray returned null for coordinate "+getFullName());
      ArrayDouble.D2 boundsForRun = (ArrayDouble.D2) bounds.slice(0,run_idx );

      int idx = findSingleHit(boundsForRun, wantOffset);
      if (idx >= 0) return idx;
      if (idx == -1) return -1;
      // multiple hits = choose closest to the midpoint
      return findClosest(boundsForRun, wantOffset);

    } else {
      ArrayDouble.D2 values = getCoordValuesArray();
      ArrayDouble.D1 valuesForRun = (ArrayDouble.D1) values.slice(0,run_idx );
      for (int i=0; i<valuesForRun.getSize(); i++) {
        if (Misc.nearlyEquals(valuesForRun.get(i), wantOffset))
          return i;
      }
      return -1;
    }
  }

  // return index if only one match, if no matches return -1, if > 1 match return -nhits
  private int findSingleHit(ArrayDouble.D2 boundsForRun, double target) {
    int hits = 0;
    int idxFound = -1;
    int n = boundsForRun.getShape()[0];
    for (int i = 0; i < n; i++) {
      if (contains(target, boundsForRun.get(i,0), boundsForRun.get(i,1))) {
        hits++;
        idxFound = i;
      }
    }
    if (hits == 1) return idxFound;
    if (hits == 0) return -1;
    return -hits;
  }

  // return index of closest value to target
  private int findClosest(ArrayDouble.D2 boundsForRun , double target) {
    double minDiff =  Double.MAX_VALUE;
    int idxFound = -1;
    int n = boundsForRun.getShape()[0];
    for (int i = 0; i < n; i++) {
      double midpoint = (boundsForRun.get(i,0) + boundsForRun.get(i,1))/2.0;
      double diff =  Math.abs(midpoint - target);
      if (diff < minDiff) {
        minDiff = diff;
        idxFound = i;
      }
    }
    return idxFound;
  }

  private boolean contains(double target, double b1, double b2) {
    if (b1 <= target && target <= b2) return true;
    return b1 >= target && target >= b2;
  }


}
