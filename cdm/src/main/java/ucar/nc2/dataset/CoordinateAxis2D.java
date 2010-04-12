/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.dataset;

import ucar.ma2.*;
import ucar.nc2.Variable;

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
  
  /**
   * Create a 2D coordinate axis from an existing VariableDS
   * @param ncd the containing dataset
   * @param vds create it from here
   */
  public CoordinateAxis2D( NetcdfDataset ncd, VariableDS vds) {
    super( ncd, vds);
  }

  // for section and slice
  @Override
  protected Variable copy() {
    return new CoordinateAxis2D(this.ncd, this);
  }

  /** Get the coordinate value at the i, j index.
   *  @param i index 0
   *  @param j index 1
   *  @return coordinate value.
   */
  public double getCoordValue(int i, int j) {
    if (midpoint == null) doRead();
    return midpoint.get( i, j);
  }

  private ArrayDouble.D2 midpoint = null;
  private void doRead() {
    Array data;
    try {
      data = read();
      // if (!hasCachedData()) setCachedData(data, false); //cache data for subsequent reading
    } catch (IOException ioe) {
      log.error("Error reading coordinate values " + ioe);
      throw new IllegalStateException(ioe);
    }

    data = data.reduce();
    if (data.getRank() != 2)
      throw new IllegalArgumentException("must be 2D");
    if (debug)
      System.out.printf("Coordinate2D read%n");

    midpoint = (ArrayDouble.D2) Array.factory(double.class, data.getShape(), data.get1DJavaArray( double.class) );
  }

  /** Get the coordinate values as a 1D double array, in canonical order.
   *  @return coordinate values
   *  @exception UnsupportedOperationException if !isNumeric()
   */
  public double[] getCoordValues() {
    if (midpoint == null) doRead();
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis2D.getCoordValues() on non-numeric");
    return (double[]) midpoint.get1DJavaArray( double.class);
  }

  /**
   * Create a new CoordinateAxis2D as a section of this CoordinateAxis2D.
   * @param r1 the section on the first index
   * @param r2 the section on the second index
   * @return a section of this CoordinateAxis2D
   * @throws InvalidRangeException if specified Ranges are invalid
   */
  public CoordinateAxis2D section(Range r1, Range r2) throws InvalidRangeException {
    List<Range> section = new ArrayList<Range>();
    section.add(r1);
    section.add(r2);
    return (CoordinateAxis2D) section( section);
  }

  public ArrayDouble.D2 getMidpoints() {
    if (midpoint == null) doRead();
    return midpoint;
  }

  /**
   * Normal case: do something reasonable in deciding on the edges when we have the midpoints of a 2D coordinate.
   * @param midx x coordinates of midpoints
   * @return x coordinates of edges with shape (ny+1, nx+1)
   */
  static public ArrayDouble.D2 makeXEdges(ArrayDouble.D2 midx) {
    int[] shape = midx.getShape();
    int ny = shape[0];
    int nx = shape[1];
    ArrayDouble.D2 edgex = new ArrayDouble.D2(ny+1, nx+1);

    for (int y=0; y<ny-1; y++) {
      for (int x=0; x<nx-1; x++) {
        // the interior edges are the average of the 4 surrounding midpoints
        double xval = (midx.get(y,x) + midx.get(y,x+1) + midx.get(y+1,x) + midx.get(y+1,x+1))/4;
        edgex.set(y+1, x+1, xval);
      }
      // extrapolate to exterior points
      edgex.set(y+1, 0, edgex.get(y+1,1) - (edgex.get(y+1,2) - edgex.get(y+1,1)));
      edgex.set(y+1, nx, edgex.get(y+1,nx-1) + (edgex.get(y+1,nx-1) - edgex.get(y+1,nx-2)));
    }

    // extrapolate to the first and last row
    for (int x=0; x<nx+1; x++) {
      edgex.set(0, x, edgex.get(1,x) - (edgex.get(2,x) - edgex.get(1,x)));
      edgex.set(ny, x, edgex.get(ny-1,x) + (edgex.get(ny-1,x) - edgex.get(ny-2,x)));
    }


   /* for (int y=0; y<ny; y++) {
      for (int x=1; x<nx; x++) {
        double xmid = (midx.get(y,x-1) + midx.get(y,x))/2;
        edgex.set(y, x, xmid);
      }
      edgex.set(y, 0, midx.get(y,0) - (edgex.get(y,1) - midx.get(y,0)));
      edgex.set(y, nx, midx.get(y, nx-1) - (edgex.get(y,nx-1) - midx.get(y,nx-1)));
    } */

    return edgex;
  }

  /**
   * Normal case: do something reasonable in deciding on the edges when we have the midpoints of a 2D coordinate.
   * @param midy y coordinates of midpoints
   * @return y coordinates of edges with shape (ny+1, nx+1)
   */
  static public ArrayDouble.D2  makeYEdges(ArrayDouble.D2 midy) {
    int[] shape = midy.getShape();
    int ny = shape[0];
    int nx = shape[1];
    ArrayDouble.D2 edgey = new ArrayDouble.D2(ny+1, nx+1);

    for (int y=0; y<ny-1; y++) {
      for (int x=0; x<nx-1; x++) {
        // the interior edges are the average of the 4 surrounding midpoints
        double xval = (midy.get(y,x) + midy.get(y,x+1) + midy.get(y+1,x) + midy.get(y+1,x+1))/4;
        edgey.set(y+1, x+1, xval);
      }
      // extrapolate to exterior points
      edgey.set(y+1, 0, edgey.get(y+1,1) - (edgey.get(y+1,2) - edgey.get(y+1,1)));
      edgey.set(y+1, nx, edgey.get(y+1,nx-1) + (edgey.get(y+1,nx-1) - edgey.get(y+1,nx-2)));
    }

    // extrapolate to the first and last row
    for (int x=0; x<nx+1; x++) {
      edgey.set(0, x, edgey.get(1,x) - (edgey.get(2,x) - edgey.get(1,x)));
      edgey.set(ny, x, edgey.get(ny-1,x) + (edgey.get(ny-1,x) - edgey.get(ny-2,x)));
    }


    /* compute the interior rows
    for (int x=0; x<nx; x++) {
      for (int y=1; y<ny; y++) {
        double yval = (midy.get(y-1,x) + midy.get(y,x))/2;
        edgey.set(y, x, yval);
      }
      edgey.set(0, x, midy.get(0,x) - (edgey.get(1,x) - midy.get(0,x)));
      edgey.set(ny, x,  midy.get(ny-1, x) - (edgey.get(ny-1,x) - midy.get(ny-1,x)));
    } */

    return edgey;
  }


  /**
   * Experimental: for WRF rotated (staggered) Grids
   * @param midx x coordinates of midpoints
   * @return x coordinates of edges with shape (ny+2, nx+1)
   */
  static public ArrayDouble.D2 makeXEdgesRotated(ArrayDouble.D2 midx) {
    int[] shape = midx.getShape();
    int ny = shape[0];
    int nx = shape[1];
    ArrayDouble.D2 edgex = new ArrayDouble.D2(ny+2, nx+1);

    // compute the interior rows
    for (int y=0; y<ny; y++) {
      for (int x=1; x<nx; x++) {
        double xval = (midx.get(y,x-1) + midx.get(y,x))/2;
        edgex.set(y+1, x, xval);
      }
      edgex.set(y+1, 0, midx.get(y,0) - (edgex.get(y+1,1) - midx.get(y,0)));
      edgex.set(y+1, nx,  midx.get(y, nx-1) - (edgex.get(y+1,nx-1) - midx.get(y,nx-1)));
    }

    // compute the first row
      for (int x=0; x<nx; x++) {
        edgex.set(0, x, midx.get(0,x));
      }

    // compute the last row
      for (int x=0; x<nx-1; x++) {
        edgex.set(ny+1, x, midx.get(ny-1,x));
      }

    return edgex;
  }

  /**
   * Experimental: for WRF rotated (staggered) Grids
   * @param midy y coordinates of midpoints
   * @return y coordinates of edges with shape (ny+2, nx+1)
   */
  static public ArrayDouble.D2  makeYEdgesRotated(ArrayDouble.D2 midy) {
    int[] shape = midy.getShape();
    int ny = shape[0];
    int nx = shape[1];
    ArrayDouble.D2 edgey = new ArrayDouble.D2(ny+2, nx+1);

    // compute the interior rows
    for (int y=0; y<ny; y++) {
      for (int x=1; x<nx; x++) {
        double yval = (midy.get(y,x-1) + midy.get(y,x))/2;
        edgey.set(y+1, x, yval);
      }
      edgey.set(y+1, 0, midy.get(y,0) - (edgey.get(y+1,1) - midy.get(y,0)));
      edgey.set(y+1, nx,  midy.get(y, nx-1) - (edgey.get(y+1,nx-1) - midy.get(y,nx-1)));
    }

    // compute the first row
      for (int x=0; x<nx; x++) {
         double pt0 = midy.get(0,x);
         double pt = edgey.get(2,x);

        double diff = pt0-pt;
        edgey.set(0, x, pt0 + diff);
      }

    // compute the last row
      for (int x=0; x<nx-1; x++) {
        double pt0 = midy.get(ny-1,x);
        double pt = edgey.get(ny-1,x);

        double diff = pt0-pt;
        edgey.set(ny+1, x, pt0 + diff);
      }

    return edgey;
  }

  /* Given a coordinate position, find what grid element contains it.
    This means that
    <pre>
    edge[i] <= pos < edge[i+1] (if values are ascending)
    edge[i] > pos >= edge[i+1] (if values are descending)
    </pre>

    @param pos position in this coordinate system
    @param lastIndex last position we looked for, or -1 if none
    @return index of grid point containing it, or -1 if outside grid area
  *
  public int findCoordElement(double pos, int lastIndex) {
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis.findCoordElement() on non-numeric");

    if (axisType == AxisType.Lon) {
      for (int x=0; x < getSize(); x++) {
        if (LatLonPointImpl.betweenLon( pos, getCoordEdge(x), getCoordEdge(x+1)))
          return x;
      }
      return -1;
    }

    if (lastIndex < 0) lastIndex = (int) getSize()/2;

    if (isAscending) {

      if ((pos < getCoordEdge(0)) || (pos > getCoordEdge((int)getSize())))
        return -1;
      while (pos < getCoordEdge(lastIndex))
        lastIndex--;
      while (pos > getCoordEdge(lastIndex+1))
        lastIndex++;
      return lastIndex;

    } else {

      if ((pos > getCoordEdge(0)) || (pos < getCoordEdge((int)getSize())))
        return -1;
      while (pos > getCoordEdge(lastIndex))
        lastIndex--;
      while (pos < getCoordEdge(lastIndex+1))
        lastIndex++;
      return lastIndex;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  private boolean isAscending;
  private boolean wasRead = false;
  private void doRead() {
    if (isNumeric()) {
      readValues();
      wasRead = true;
      //calcIsRegular();
    } else {
      readStringValues();
      wasRead = true;
    }

    isAscending = getCoordEdge(0) < getCoordEdge(1);
  }

  private String[] names = null;
  private void readStringValues() {
    int count = 0;
    ArrayChar data;
    try {
      data = (ArrayChar) read();
    } catch (IOException ioe) { return; }
    ArrayChar.StringIterator iter = data.getStringIterator();
    names = new String[ iter.getNumElems()];
    while (iter.hasNext())
      names[count++] = iter.next();
  }


  private double[] midpoint, edge;
  private void readValues() {
    midpoint = new double[ (int) getSize()];
    int count = 0;
    Array data;
    try {
      data = read();
    } catch (IOException ioe) { return; }

    IndexIterator iter = data.getIndexIterator();
    while (iter.hasNext())
      midpoint[count++] = iter.getDoubleNext();

    makeEdges();
  }

  private void makeEdges() {
    int size = (int) getSize();
    edge = new double[size+1];
    for(int i=1; i<size; i++)
      edge[i] = (midpoint[i-1] + midpoint[i])/2;
    edge[0] = midpoint[0] - (edge[1] - midpoint[0]);
    edge[size] = midpoint[size-1] + (midpoint[size-1] - edge[size-1]);
  }

  private void makeMidpoints() {
    int size = (int) getSize();
    midpoint = new double[size];
    for(int i=0; i<size; i++)
      midpoint[i] = (edge[i] + edge[i+1])/2;
  }
  */


}