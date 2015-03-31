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
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.util.Misc;
import ucar.nc2.util.NamedObject;
import ucar.unidata.util.Format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A 1-dimensional Coordinate Axis. Its values must be monotonic.
 * <p/>
 * If this is char valued, it will have rank 2, otherwise it will have rank 1.
 * <p/>
 * If string or char valued, only <i>getCoordName()</i> can be called.
 * <p/>
 * If the coordinates are regularly spaced, <i>isRegular()</i> is true, and the values are equal to
 * <i>getStart()</i> + i * <i>getIncrement()</i>.
 * <p/>
 * This will also set "cell bounds" for this axis. By default, the cell bounds are midway between the coordinates values,
 * and are therefore contiguous, and can be accessed though getCoordEdge(i).
 * The only way the bounds can be set is if the coordinate variable has an attribute "bounds" that points to another variable
 * bounds(ncoords,2). These contain the cell bounds, and must be ascending or descending as the coordinate values are. In
 * this case isContiguous() is true when bounds1(i+1) == bounds2(i) for all i.
 *
 * @author john caron
 * @see CoordinateAxis#factory
 */

public class CoordinateAxis1D extends CoordinateAxis {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinateAxis1D.class);

  private boolean wasRead = false; // have the data values been  read
  private boolean wasBoundsDone = false; // have we created the bounds arrays if exists ?
  private boolean isInterval = false; // is this an interval coordinates - then should use bounds
  private boolean isAscending;

  // read in on doRead()
  private double[] coords; // coordinate values, must be between edges
  private String[] names = null; // only set if String or char values

  // defer making until asked, use makeBounds()
  private double[] edge; // n+1 edges, edge[k] < midpoint[k] < edge[k+1]
  private double[] bound1, bound2; // may be contiguous or not

  private boolean wasCalcRegular = false; // have we checked if the data is regularly spaced ?
  private boolean isRegular = false;
  private double start, increment;

  /**
   * Create a 1D coordinate axis from an existing Variable
   *
   * @param ncd the containing dataset
   * @param vds wrap this VariableDS, which is not changed.
   */
  public CoordinateAxis1D(NetcdfDataset ncd, VariableDS vds) {
    super(ncd, vds);
    vds.setCaching(true);
  }

  /**
   * Copy constructor
   * @param ncd ok to reparent
   * @param org copy from here
   */
  CoordinateAxis1D(NetcdfDataset ncd, CoordinateAxis1D org) {
    super(ncd, org);
    this.orgName = org.orgName;
    this.cache = new Variable.Cache(); // decouple cache
    org.setCaching(true);

    // copy rest of state
    this.increment = org.getIncrement();
    this.isAscending = org.isAscending;
    this.isInterval = org.isInterval();
    this.isRegular = org.isRegular();

    this.coords = org.getCoordValues();
    this.edge = org.getCoordEdges();
    this.names = org.names;

    if (isInterval) {
      this.bound1 = org.getBound1();
      this.bound2 = org.getBound2();
    }

    this.wasBoundsDone = org.wasBoundsDone;
    this.wasCalcRegular = org.wasCalcRegular;
    this.wasRead = org.wasRead;
  }

  /**
   * Constructor when theres no underlying variable. You better set the values too!
   *
   * @param ds        the containing dataset.
   * @param group     the containing group; if null, use rootGroup
   * @param shortName axis name.
   * @param dataType  data type
   * @param dims      list of dimension names
   * @param units     units of coordinates, preferably udunit compatible.
   * @param desc      long name.
   */
  public CoordinateAxis1D(NetcdfDataset ds, Group group, String shortName,
                          DataType dataType, String dims, String units, String desc) {

    super(ds, group, shortName, dataType, dims, units, desc);
  }

  /**
   * Create a new CoordinateAxis1D as a section of this CoordinateAxis1D.
   *
   * @param r the section range
   * @return a new CoordinateAxis1D as a section of this CoordinateAxis1D
   * @throws InvalidRangeException if IllegalRange
   */
  public CoordinateAxis1D section(Range r) throws InvalidRangeException {
    Section section = new Section().appendRange(r);
    CoordinateAxis1D result = (CoordinateAxis1D) section(section);
    int len = r.length();

    // deal with the midpoints, bounds
    double[] new_mids = new double[len];
    for (int idx = 0; idx < len; idx++) {
      int old_idx = r.element(idx);
      new_mids[idx] = coords[old_idx];
    }
    result.coords = new_mids;

    if (isInterval) {
      double[] new_bound1 = new double[len];
      double[] new_bound2 = new double[len];
      double[] new_edge = new double[len+1];
      for (int idx = 0; idx < len; idx++) {
        int old_idx = r.element(idx);
        new_bound1[idx] = bound1[old_idx];
        new_bound2[idx] = bound2[old_idx];
        new_edge[idx] = bound1[old_idx];
        new_edge[idx+1] = bound2[old_idx]; // all but last are overwritten
      }
      result.bound1 = new_bound1;
      result.bound2 = new_bound2;
      result.edge = new_edge;

    } else {
      double[] new_edge = new double[len+1];
      for (int idx = 0; idx < len; idx++) {
        int old_idx = r.element(idx);
        new_edge[idx] = edge[old_idx];
        new_edge[idx+1] = edge[old_idx+1]; // all but last are overwritten
      }
      result.edge = new_edge;
    }

    if (names != null) {
      String[] new_names = new String[len];
      for (int idx = 0; idx < len; idx++) {
        int old_idx = r.element(idx);
        new_names[idx] = names[old_idx];
      }
      result.names = new_names;
    }

    result.wasCalcRegular = false;
    result.calcIsRegular();

    return result;
  }

  // for section and slice

  @Override
  protected Variable copy() {
    return new CoordinateAxis1D(this.ncd, this);
  }

  @Override
  public CoordinateAxis copyNoCache() {
    CoordinateAxis1D axis = new CoordinateAxis1D(ncd, getParentGroup(), getShortName(), getDataType(), getDimensionsString(),
        getUnitsString(), getDescription());

    // other state
    axis.axisType = this.axisType;
    axis.boundaryRef = this.boundaryRef;
    axis.isContiguous = this.isContiguous;
    axis.positive = this.positive;

    axis.cache = new Variable.Cache(); // decouple cache
    return axis;
  }

  /**
    * Get the list of names, to be used for user selection.
    * The ith one refers to the ith coordinate.
    *
    * @return List of ucar.nc2.util.NamedObject, or empty list.
    */
   public List<NamedObject> getNames() {
     int n = getDimension(0).getLength();
     List<NamedObject> names = new ArrayList<>(n);
     for (int i = 0; i < n; i++)
       names.add(new ucar.nc2.util.NamedAnything(getCoordName(i), getShortName() + " "+ getUnitsString()));
     return names;
   }

  /**
   * The "name" of the ith coordinate. If nominal, this is all there is to a coordinate.
   * If numeric, this will return a String representation of the coordinate.
   *
   * @param index which one ?
   * @return the ith coordinate value as a String
   */
  public String getCoordName(int index) {
    if (!wasRead) doRead();
    if (isNumeric())
      return Format.d(getCoordValue(index), 5, 8);
    else
      return names[index];
  }

  /**
   * Get the ith coordinate value. This is the value of the coordinate axis at which
   * the data value is associated. These must be strictly monotonic.
   *
   * @param index which coordinate. Between 0 and getNumElements()-1 inclusive.
   * @return coordinate value.
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double getCoordValue(int index) {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getCoordValue() on non-numeric");
    if (!wasRead) doRead();
    return coords[index];
  }

  @Override
  public double getMinValue() {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getCoordValue() on non-numeric");
    if (!wasRead) doRead();

    return Math.min(coords[0], coords[(int) getSize() - 1]);
  }

  @Override
  public double getMaxValue() {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getCoordValue() on non-numeric");
    if (!wasRead) doRead();

    return Math.max(coords[0], coords[(int) getSize() - 1]);
  }

  /**
   * Get the ith coordinate edge. Exact only if isContiguous() is true, otherwise use getBound1() and getBound2().
   * This is the value where the underlying grid element switches
   * from "belonging to" coordinate value i-1 to "belonging to" coordinate value i.
   * In some grids, this may not be well defined, and so should be considered an
   * approximation or a visualization hint.
   * <p><pre>
   *  Coordinate edges must be strictly monotonic:
   *    coordEdge(0) < coordValue(0) < coordEdge(1) < coordValue(1) ...
   *    ... coordEdge(i) < coordValue(i) < coordEdge(i+1) < coordValue(i+1) ...
   *    ... coordEdge(n-1) < coordValue(n-1) < coordEdge(n)
   *  </pre>
   *
   * @param index which coordinate. Between 0 and getNumElements() inclusive.
   * @return coordinate edge.
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double getCoordEdge(int index) {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getCoordEdge() on non-numeric");
    if (!wasBoundsDone) makeBounds();
    return edge[index];
  }

  /**
   * Get the coordinate values as a double array.
   *
   * @return coordinate value.
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double[] getCoordValues() {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getCoordValues() on non-numeric");
    if (!wasRead) doRead();
    return coords.clone();
  }

  /**
   * Get the coordinate edges as a double array.
   * Exact only if isContiguous() is true, otherwise use getBound1() and getBound2().
   *
   * @return coordinate edges.
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double[] getCoordEdges() {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getCoordEdges() on non-numeric");
    if (!wasBoundsDone) makeBounds();
    return edge.clone();
  }

  @Override
  public boolean isContiguous() {
    if (!wasBoundsDone) makeBounds();  // this sets isContiguous
    return isContiguous;
  }

  ///////////////////////////////////////////////

  /**
   * If this coordinate has interval values.
   * If so, then one should use getBound1, getBound2, and not getCoordEdges()
   * @return true if coordinate has interval values
   */
  public boolean isInterval() {
    if (!wasBoundsDone) makeBounds();      // this sets isInterval
    return isInterval;
  }

  /**
   * Get the coordinate bound1 as a double array.
   * bound1[i] # coordValue[i] # bound2[i], where # is < if increasing (bound1[i] < bound1[i+1])
   * else < if decreasing.
   *
   * @return coordinate bound1.
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double[] getBound1() {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getBound1() on non-numeric");
    if (!wasBoundsDone) makeBounds();
    if (bound1 == null) makeBoundsFromEdges();
    assert bound1 != null;
    return bound1.clone();
  }

  /**
   * Get the coordinate bound1 as a double array.
   * bound1[i] # coordValue[i] # bound2[i],  where # is < if increasing (bound1[i] < bound1[i+1])
   * else < if decreasing.
   *
   * @return coordinate bound2.
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double[] getBound2() {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getBound2() on non-numeric");
    if (!wasBoundsDone) makeBounds();
    if (bound2 == null) makeBoundsFromEdges();
    assert bound2 != null;
    return bound2.clone();
  }

  /**
   * Get the coordinate bounds for the ith coordinate.
   * Can use this for isContiguous() true or false.
   *
   * @param i coordinate index
   * @return double[2] edges for ith coordinate
   */
  public double[] getCoordBounds(int i) {
    if (!wasBoundsDone) makeBounds();

    double[] e = new double[2];
    if (isContiguous()) {
      e[0] = getCoordEdge(i);
      e[1] = getCoordEdge(i + 1);
    } else {
      e[0] = bound1[i];
      e[1] = bound2[i];
    }
    return e;
  }

  /**
   * Given a coordinate position, find what grid element contains it.
   * This means that
   * <pre>
   * edge[i] <= pos < edge[i+1] (if values are ascending)
   * edge[i] > pos >= edge[i+1] (if values are descending)
   * </pre>
   *
   * @param coordVal position in this coordinate system
   * @return index of grid point containing it, or -1 if outside grid area
   */
  public int findCoordElement(double coordVal) {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis.findCoordElement() on non-numeric");

    if (isRegular())
      return findCoordElementRegular(coordVal, false);
    if (isContiguous())
      return findCoordElementIrregular(coordVal, false);
    else
      return findCoordElementNonContiguous(coordVal, false);
  }

  /**
   * Given a coordinate position, find what grid element contains it, or is closest to it.
   *
   * @param coordVal position in this coordinate system
   * @return index of grid point containing it, or best estimate of closest grid interval.
   */
  public int findCoordElementBounded(double coordVal) {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis.findCoordElementBounded() on non-numeric");

    if (isRegular())
      return findCoordElementRegular(coordVal, true);
    if (isContiguous())
      return findCoordElementIrregular(coordVal, true);
    else
      return findCoordElementNonContiguous(coordVal, true);
  }

  /**
   * @deprecated use findCoordElement(coordVal)
   */
  public int findCoordElement(double coordVal, int lastIndex) {
    return findCoordElement(coordVal);
  }

  //////////////////////////////////////////////////////////////////
  // following is from Jon Blower's ncWMS
  // faster routines for coordValue -> index search
  // significantly modified

  /**
   * Optimize the regular case
   * Gets the index of the given point. Uses index = (value - start) / stride,
   * hence this is faster than an exhaustive search.
   * from jon blower's ncWMS.
   *
   * @param coordValue The value along this coordinate axis
   * @param bounded    if false and not in range, return -1, else nearest index
   * @return the index that is nearest to this point, or -1 if the point is
   *         out of range for the axis
   */
  private int findCoordElementRegular(double coordValue, boolean bounded) {
    int n = (int) this.getSize();

  /*  if (axisType == AxisType.Lon) {
      double maxValue = this.start + this.increment * n;
      if (betweenLon(coordValue, this.start, maxValue)) {
        double distance = LatLonPointImpl.getClockwiseDistanceTo(this.start, coordValue);
        double exactNumSteps = distance / this.increment;
        // This axis might wrap, so we make sure that the returned index is within range
        return ((int) Math.round(exactNumSteps)) % (int) this.getSize();

      } else if (coordValue < this.start) {
        return bounded ? 0 : -1;
      } else {
        return bounded ? n - 1 : -1;
      }
    } */

    double distance = coordValue - this.start;
    double exactNumSteps = distance / this.increment;
    int index = (int) Math.round(exactNumSteps);
    if (index < 0)
      return bounded ? 0 : -1;
    else if (index >= n)
      return bounded ? n - 1 : -1;
    return index;
  }

  private boolean betweenLon(double lon, double lonBeg, double lonEnd) {
    while (lon < lonBeg) lon += 360;
    return (lon >= lonBeg) && (lon <= lonEnd);
  }

  /**
   * Performs a binary search to find the index of the element of the array
   * whose value is contained in the interval, so must be contiguous.
   *
   * @param target  The value to search for
   * @param bounded if false, and not in range, return -1, else nearest index
   * @return the index of the element in values whose value is closest to target,
   *         or -1 if the target is out of range
   */
  private int findCoordElementIrregular(double target, boolean bounded) {
    int n = (int) this.getSize();
    int low = 0;
    int high = n;

    /* special case for longitude
    if (axisType == AxisType.Lon) {
      if (target < this.edge[low]) {
        target += 360.0;
        if (target > this.edge[high])
          return bounded ? 0 : -1;

      } else if (target > this.edge[high]) {
        target -= 360.0;
        if (target < this.edge[low])
          return bounded ? n - 1 : -1;
      }
    } */

    if (isAscending) {
      // Check that the point is within range
      if (target < this.edge[low])
        return bounded ? 0 : -1;
      else if (target > this.edge[high])
        return bounded ? n - 1 : -1;

      // do a binary search to find the nearest index
      int mid = low;
      while (high > low + 1) {
        mid = (low + high) / 2;
        double midVal = this.edge[mid];
        if (midVal == target) return mid;
        else if (midVal < target) low = mid;
        else high = mid;
      }

      return low;

    } else {

      // Check that the point is within range
      if (target > this.edge[low])
        return bounded ? 0 : -1;
      else if (target < this.edge[high])
        return bounded ? n - 1 : -1;

      // do a binary search to find the nearest index
      int mid = low;
      while (high > low + 1) {
        mid = (low + high) / 2;
        double midVal = this.edge[mid];
        if (midVal == target) return mid;
        else if (midVal < target) high = mid;
        else low = mid;
      }

      return high - 1;
    }
  }

  /**
   * Given a coordinate position, find what grid element contains it.
   * Only use if isContiguous() == false
   * This algorithm does a linear search in the bound1[] amd bound2[] array.
   * <p/>
   * This means that
   * <pre>
   * edge[i] <= pos < edge[i+1] (if values are ascending)
   * edge[i] > pos >= edge[i+1] (if values are descending)
   * </pre>
   *
   * @param target  The value to search for
   * @param bounded if false, and not in range, return -1, else nearest index
   * @return the index of the element in values whose value is closest to target,
   *         or -1 if the target is out of range
   */
  private int findCoordElementNonContiguous(double target, boolean bounded) {

    double[] bounds1 = getBound1();
    double[] bounds2 = getBound2();
    int n = bounds1.length;

    if (isAscending) {
      // Check that the point is within range
      if (target < bounds1[0])
        return bounded ? 0 : -1;
      else if (target > bounds2[n-1])
        return bounded ? n-1 : -1;

      // do a linear search to find the nearest index
      for (int i=0; i<n; i++) {
        if ((bound1[i] <= target) && (target <= bound2[i]))
           return i;
        if (bound1[i] > target) {
          if (!bounded) return -1;
          double d1 = bound1[i] - target;
          double d2 = target - bound1[i-1];
          return (d1 > d2) ? i-1 : i;
        }
      }
      return bounded ? n-1 : -1;

    } else {

      // Check that the point is within range
      if (target > bounds1[0])
        return bounded ? 0 : -1;
      else if (target < bounds2[n-1])
        return bounded ? n-1 : -1;

      // do a linear search to find the nearest index
      for (int i=0; i<n; i++) {
        if ((bound2[i] <= target) && (target <= bound1[i]))
           return i;
        if (bound2[i] < target) {
          if (!bounded) return -1;
          double d1 = bound2[i] - target;
          double d2 = target - bound2[i-1];
          return (d1 > d2) ? i-1 : i;
        }
      }
      return bounded ? n-1 : -1;
    }
  }

  ////////////////////////////////////
  // hmmm dubious

  // private boolean isLayer = false;

  /*
   * Caution: many datasets do not explicitly specify this info, this is often a guess; default is false.
   *
   * @return true if coordinate lies between a layer, or false if its at a point.
   *
  public boolean isLayer() {
    return isLayer;
  }


  private void setIsLayer() {
    Attribute att = findAttribute(_Coordinate.ZisLayer);
    if ((att != null) && att.getStringValue().equalsIgnoreCase("true"))
      this.isLayer = true;
  } */

  ///////////////////////////////////////////////////////////////////////////////
  // check if Regular

  /**
   * Get starting value if isRegular()
   *
   * @return starting value if isRegular()
   */
  public double getStart() {
    calcIsRegular();
    return start;
  }

  /**
   * Get increment value if isRegular()
   *
   * @return increment value if isRegular()
   */
  public double getIncrement() {
    calcIsRegular();
    return increment;
  }

  /**
   * If true, then value(i) = <i>getStart()</i> + i * <i>getIncrement()</i>.
   *
   * @return if evenly spaced.
   */
  public boolean isRegular() {
    calcIsRegular();
    return isRegular;
  }

  private void calcIsRegular() {
    if (wasCalcRegular) return;
    if (!wasRead) doRead();

    if (!isNumeric())
      isRegular = false;
    else if (getSize() < 2)
      isRegular = true;
    else {
      start = getCoordValue(0);
      int n = (int) getSize();
      increment = (getCoordValue(n - 1) - getCoordValue(0)) / (n - 1);
      isRegular = true;
      for (int i = 1; i < getSize(); i++)
        if (!ucar.nc2.util.Misc.closeEnough(getCoordValue(i) - getCoordValue(i - 1), increment, 5.0e-3)) {
          isRegular = false;
          break;
        }
    }
    wasCalcRegular = true;
  }

  ///////////////////////////////////////////////////////////////////////////////


  private void doRead() {
    if (isNumeric()) {
      readValues();
      wasRead = true;

      if (getSize() < 2)
        isAscending = true;
      else
        isAscending = getCoordValue(0) < getCoordValue(1);
      //  calcIsRegular(); */
    } else

    if (getDataType() == DataType.STRING) {
      readStringValues();
      wasRead = true;
    } else {
      readCharValues();
      wasRead = true;
    }
  }

  // turns longitude coordinate into monotonic, dealing with possible wrap.
  public void correctLongitudeWrap() {
    // correct non-monotonic longitude coords
    if (axisType != AxisType.Lon) {
      return;
    }

    if (!wasRead) doRead();
    if (!wasBoundsDone) makeBounds();

    boolean monotonic = true;
    for (int i = 0; i < coords.length - 1; i++)
      monotonic &= isAscending ? coords[i] < coords[i + 1] : coords[i] > coords[i + 1];

    if (!monotonic) {
      boolean cross = false;
      if (isAscending) {
        for (int i = 0; i < coords.length; i++) {
          if (cross) coords[i] += 360;
          if (!cross && (i < coords.length - 1) && (coords[i] > coords[i + 1]))
            cross = true;
        }
      } else {
        for (int i = 0; i < coords.length; i++) {
          if (cross) coords[i] -= 360;
          if (!cross && (i < coords.length - 1) && (coords[i] < coords[i + 1]))
            cross = true;
        }
      }

      // LOOK - need to make sure we get stuff from the cache
      Array cachedData = Array.factory(DataType.DOUBLE, getShape(), coords);
      if (getDataType() != DataType.DOUBLE)
        cachedData = MAMath.convert(cachedData, getDataType());
      setCachedData(cachedData);

      if (!isInterval) {
        makeEdges();
      }
    }

  }

  // only used if String

  private void readStringValues() {
    int count = 0;
    Array data;
    try {
      data = read();
    } catch (IOException ioe) {
      log.error("Error reading string coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }

    names = new String[(int) data.getSize()];
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext())
      names[count++] = (String) ii.getObjectNext();
  }

  private void readCharValues() {
    int count = 0;
    ArrayChar data;
    try {
      data = (ArrayChar) read();
    } catch (IOException ioe) {
      log.error("Error reading char coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }
    ArrayChar.StringIterator iter = data.getStringIterator();
    names = new String[iter.getNumElems()];
    while (iter.hasNext())
      names[count++] = iter.next();
  }

  private void readValues() {
    coords = new double[(int) getSize()];
    int count = 0;
    Array data;
    try {
      // setUseNaNs(false); // missing values not allowed LOOK not true for point data !!
      data = read();
      // if (!hasCachedData()) setCachedData(data, false); //cache data for subsequent reading
    } catch (IOException ioe) {
      log.error("Error reading coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }

    IndexIterator iter = data.getIndexIterator();
    while (iter.hasNext())
      coords[count++] = iter.getDoubleNext();
  }

  /**
   * Calculate bounds, set isInterval, isContiguous
   */
  private void makeBounds() {
    if (!wasRead) doRead();
    if (isNumeric()) {
      if (!makeBoundsFromAux()) {
        makeEdges();
      }
    }
    wasBoundsDone = true;
  }

  private boolean makeBoundsFromAux() {
    Attribute boundsAtt = findAttributeIgnoreCase(CF.BOUNDS);
    if ((null == boundsAtt) || !boundsAtt.isString()) return false;
    String boundsVarName = boundsAtt.getStringValue();
    VariableDS boundsVar = (VariableDS) ncd.findVariable(getParentGroup(), boundsVarName);
    if (null == boundsVar) return false;
    if (2 != boundsVar.getRank()) return false;

    if (getDimension(0) != boundsVar.getDimension(0)) return false;
    if (2 != boundsVar.getDimension(1).getLength()) return false;

    Array data;
    try {
      boundsVar.setUseNaNs(false); // missing values not allowed
      data = boundsVar.read();
    } catch (IOException e) {
      log.warn("CoordinateAxis1D.hasBounds read failed ", e);
      return false;
    }

    assert (data.getRank() == 2) && (data.getShape()[1] == 2) : "incorrect shape data for variable " + boundsVar;

    // extract the bounds
    int n = shape[0];
    double[] value1 = new double[n];
    double[] value2 = new double[n];
    Index ima = data.getIndex();
    for (int i = 0; i < n; i++) {
      ima.set0(i);
      value1[i] = data.getDouble(ima.set1(0));
      value2[i] = data.getDouble(ima.set1(1));
    }

    // flip if needed
    boolean firstLower = true; // in the first interval, is lower < upper ?
    for (int i=0; i<value1.length; i++) {
      if (Misc.closeEnough(value1[i], value2[i])) continue; // skip when lower == upper
      firstLower = value1[i] < value2[i];
      break;
    }
    // check first against last : lower, unless all lower equal then upper
    boolean goesUp = (n < 2) || value1[n-1] > value1[0] || (Misc.closeEnough(value1[n-1], value2[0]) && value2[n-1] > value2[0]);
    if (goesUp != firstLower) {
      double[] temp = value1;
      value1 = value2;
      value2 = temp;
    }

    // decide if they are contiguous
    boolean contig = true;
    for (int i = 0; i < n - 1; i++) {
      if (!ucar.nc2.util.Misc.closeEnough(value1[i + 1], value2[i]))
        contig = false;
    }

    if (contig) {
      edge = new double[n + 1];
      edge[0] = value1[0];
      for (int i = 1; i < n + 1; i++)
        edge[i] = value2[i - 1];
    } else {                           // what does edge mean when not contiguous ??
      edge = new double[n + 1];
      edge[0] = value1[0];
      for (int i = 1; i < n; i++)
        edge[i] = (value1[i] + value2[i - 1]) / 2;
      edge[n] = value2[n - 1];
      isContiguous = false;
    }

    bound1 = value1;
    bound2 = value2;
    isInterval = true;

    return true;
  }

  private void makeEdges() {
    int size = (int) getSize();
    edge = new double[size + 1];
    if (size < 1) return;
    for (int i = 1; i < size; i++)
      edge[i] = (coords[i - 1] + coords[i]) / 2;
    edge[0] = coords[0] - (edge[1] - coords[0]);
    edge[size] = coords[size - 1] + (coords[size - 1] - edge[size - 1]);
    isContiguous = true;
  }

  private void makeBoundsFromEdges() {
    int size = (int) getSize();
    if (size == 0) return;

    bound1 = new double[size];
    bound2 = new double[size];
    for (int i = 0; i < size; i++) {
      bound1[i] = edge[i];
      bound2[i] = edge[i + 1];
    }

    // flip if needed
    if (bound1[0] > bound2[0]) {
      double[] temp = bound1;
      bound1 = bound2;
      bound2 = temp;
    }
  }

}
