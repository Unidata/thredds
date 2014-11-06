/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.dataset;

import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;

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

    coords = (ArrayDouble.D2) Array.factory(double.class, data.getShape(), data.get1DJavaArray(double.class));

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
    return (double[]) coords.get1DJavaArray(double.class);
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

  /**
   * @deprecated use getCoordValuesArray
   */
  public ArrayDouble.D2 getMidpoints() {
    return getCoordValuesArray();
  }

  public ArrayDouble.D2 getXEdges() {
    ArrayDouble.D2 mids = getCoordValuesArray();
    return makeXEdges(mids);
  }

  public ArrayDouble.D2 getYEdges() {
    ArrayDouble.D2 mids = getCoordValuesArray();
    return makeYEdges(mids);
  }

  /**
   * Normal case: do something reasonable in deciding on the edges when we have the midpoints of a 2D coordinate.
   *
   * @param midx x coordinates of midpoints
   * @return x coordinates of edges with shape (ny+1, nx+1)
   */
  static public ArrayDouble.D2 makeXEdges(ArrayDouble.D2 midx) {
    int[] shape = midx.getShape();
    int ny = shape[0];
    int nx = shape[1];
    ArrayDouble.D2 edgex = new ArrayDouble.D2(ny + 1, nx + 1);

    for (int y = 0; y < ny - 1; y++) {
      for (int x = 0; x < nx - 1; x++) {
        // the interior edges are the average of the 4 surrounding midpoints
        double xval = (midx.get(y, x) + midx.get(y, x + 1) + midx.get(y + 1, x) + midx.get(y + 1, x + 1)) / 4;
        edgex.set(y + 1, x + 1, xval);
      }
      // extrapolate to exterior points
      edgex.set(y + 1, 0, edgex.get(y + 1, 1) - (edgex.get(y + 1, 2) - edgex.get(y + 1, 1)));
      edgex.set(y + 1, nx, edgex.get(y + 1, nx - 1) + (edgex.get(y + 1, nx - 1) - edgex.get(y + 1, nx - 2)));
    }

    // extrapolate to the first and last row
    for (int x = 0; x < nx + 1; x++) {
      edgex.set(0, x, edgex.get(1, x) - (edgex.get(2, x) - edgex.get(1, x)));
      edgex.set(ny, x, edgex.get(ny - 1, x) + (edgex.get(ny - 1, x) - edgex.get(ny - 2, x)));
    }

    return edgex;
  }

  /**
   * Normal case: do something reasonable in deciding on the edges when we have the midpoints of a 2D coordinate.
   *
   * @param midy y coordinates of midpoints
   * @return y coordinates of edges with shape (ny+1, nx+1)
   */
  static public ArrayDouble.D2 makeYEdges(ArrayDouble.D2 midy) {
    int[] shape = midy.getShape();
    int ny = shape[0];
    int nx = shape[1];
    ArrayDouble.D2 edgey = new ArrayDouble.D2(ny + 1, nx + 1);

    for (int y = 0; y < ny - 1; y++) {
      for (int x = 0; x < nx - 1; x++) {
        // the interior edges are the average of the 4 surrounding midpoints
        double xval = (midy.get(y, x) + midy.get(y, x + 1) + midy.get(y + 1, x) + midy.get(y + 1, x + 1)) / 4;
        edgey.set(y + 1, x + 1, xval);
      }
      // extrapolate to exterior points
      edgey.set(y + 1, 0, edgey.get(y + 1, 1) - (edgey.get(y + 1, 2) - edgey.get(y + 1, 1)));
      edgey.set(y + 1, nx, edgey.get(y + 1, nx - 1) + (edgey.get(y + 1, nx - 1) - edgey.get(y + 1, nx - 2)));
    }

    // extrapolate to the first and last row
    for (int x = 0; x < nx + 1; x++) {
      edgey.set(0, x, edgey.get(1, x) - (edgey.get(2, x) - edgey.get(1, x)));
      edgey.set(ny, x, edgey.get(ny - 1, x) + (edgey.get(ny - 1, x) - edgey.get(ny - 2, x)));
    }

    return edgey;
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
    return 2 == boundsVar.getDimension(2).getLength();
  }

  ///////////////////////////////////////
  // time

  public CoordinateAxisTimeHelper getCoordinateAxisTimeHelper() {
    return new CoordinateAxisTimeHelper(getCalendarFromAttribute(), getUnitsString());
  }


}