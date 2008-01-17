/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dataset;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.NamedObject;
import ucar.unidata.util.Format;
import ucar.unidata.geoloc.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A 1-dimensional Coordinate Axis. Its values must be monotonic.
 *
 * If this is char valued, it will have rank 2, otherwise it will have rank 1.
 * <p>
 * If string or char valued, only <i>getCoordName()</i> can be called.
 * <p>
 * If the coordinates are regularly spaced, <i>isRegular()</i> is true, and the values are equal to
 *   <i>getStart()</i> + i * <i>getIncrement()</i>.
 *
 * @see CoordinateAxis#factory
 * @author john caron
 */

public class CoordinateAxis1D extends CoordinateAxis {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinateAxis1D.class);


  /** Create a 1D coordinate axis from an existing Variable
   * @param ncd the containing dataset
   * @param vds wrap this VariableDS, which is not changed.
   */
  public CoordinateAxis1D(NetcdfDataset ncd, VariableDS vds) {
    super( ncd, vds);
    setIsLayer();
  }

  /** Constructor when theres no underlying variable. You better set the values too!
    * @param ds the containing dataset.
    * @param group the containing group; if null, use rootGroup
    * @param shortName axis name.
    * @param dataType data type
    * @param dims list of dimension names
    * @param units units of coordinates, preferably udunit compatible.
    * @param desc long name.
    */
  public CoordinateAxis1D(NetcdfDataset ds, Group group, String shortName,
                          DataType dataType, String dims, String units, String desc) {

    super( ds, group, shortName, dataType, dims, units, desc);
    setIsLayer();
  }

  /**
   * Create a new CoordinateAxis1D as a section of this CoordinateAxis1D.
   * @param r the section range
   * @return a new CoordinateAxis1D as a section of this CoordinateAxis1D
   * @throws InvalidRangeException if IllegalRange
   */
  public CoordinateAxis1D section(Range r) throws InvalidRangeException {
    Section section = new Section().appendRange(r);
    return (CoordinateAxis1D) section(section);
  }

   // for section and slice
  @Override
  protected Variable copy() {
    return new CoordinateAxis1D(this.ncd, this);
  }

  /** The "name" of the ith coordinate. If nominal, this is all there is to a coordinate.
   *  If numeric, this will return a String representation of the coordinate.
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

  /** Get the ith coordinate value. This is the value of the coordinate axis at which
   *  the data value is associated. These must be strictly monotonic.
   *  @param index which coordinate. Between 0 and getNumElements()-1 inclusive.
   *  @return coordinate value.
   *  @exception UnsupportedOperationException if !isNumeric()
   */
  public double getCoordValue(int index) {
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis1D.getCoordValue() on non-numeric");
    if (!wasRead) doRead();
    return midpoint[index];
  }

  public double getMinValue() {
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis1D.getCoordValue() on non-numeric");
    if (!wasRead) doRead();

    return Math.min( midpoint[0], midpoint[(int) getSize() -1]);
  }

  public double getMaxValue() {
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis1D.getCoordValue() on non-numeric");
    if (!wasRead) doRead();

    return Math.max( midpoint[0], midpoint[(int) getSize() -1]);
  }

  /** Get the ith coordinate edge. Exact only if isContiguous() is true, otherwise use getBound1() and getBound2().
   *  This is the value where the underlying grid element switches
   *  from "belonging to" coordinate value i-1 to "belonging to" coordinate value i.
   *  In some grids, this may not be well defined, and so should be considered an
   *  approximation or a visualization hint.
   *  <p><pre>
   *  Coordinate edges must be strictly monotonic:
   *    coordEdge(0) < coordValue(0) < coordEdge(1) < coordValue(1) ...
   *    ... coordEdge(i) < coordValue(i) < coordEdge(i+1) < coordValue(i+1) ...
   *    ... coordEdge(n-1) < coordValue(n-1) < coordEdge(n)
   *  </pre>
   *  @param index which coordinate. Between 0 and getNumElements() inclusive.
   *  @return coordinate edge.
   *  @exception UnsupportedOperationException if !isNumeric()
   */
  public double getCoordEdge(int index) {
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis1D.getCoordEdge() on non-numeric");
    if (!wasRead) doRead();
    return edge[index];
  }

  /** Get the coordinate values as a double array.
   *  @return coordinate value.
   *  @exception UnsupportedOperationException if !isNumeric()
   */
  public double[] getCoordValues() {
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis1D.getCoordValues() on non-numeric");
    if (!wasRead) doRead();
    return midpoint.clone();
  }

  /** Get the coordinate edges as a double array.
   * Exact only if isContiguous() is true, otherwise use getBound1() and getBound2().
   *  @return coordinate edges.
   *  @exception UnsupportedOperationException if !isNumeric()
   */
  public double[] getCoordEdges() {
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis1D.getCoordEdges() on non-numeric");
    if (!wasRead) doRead();
    return edge.clone();
  }

  /** Get the coordinate bound1 as a double array.
   *  bound1[i] # coordValue[i] # bound2[i], where # is < if increasing (bound1[i] < bound1[i+1])
   *  else < if decreasing.
   *  @return coordinate bound1.
   *  @exception UnsupportedOperationException if !isNumeric()
   */
  public double[] getBound1() {
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis1D.getBound1() on non-numeric");
    if (!wasRead) doRead();
    return bound1.clone();
  }

  /** Get the coordinate bound1 as a double array.
   *  bound1[i] # coordValue[i] # bound2[i],  where # is < if increasing (bound1[i] < bound1[i+1])
   *  else < if decreasing.
   *  @return coordinate bound2.
   *  @exception UnsupportedOperationException if !isNumeric()
   */
  public double[] getBound2() {
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis1D.getBound2() on non-numeric");
    if (!wasRead) doRead();
    return bound2.clone();
  }


  /** Get the coordinate edges for the ith coordinate.
   *  Can use this for isContiguous() true or false.
   * @param i coordinate index
   * @return double[2] edges for ith coordinate
   */
  public double[] getCoordEdges(int i) {
    if (!wasRead) doRead();
    double[] e = new double[2];
    if (isContiguous()) {
      e[0] = getCoordEdge(i);
      e[1] = getCoordEdge(i+1);
    } else {
      e[0] = bound1[i];
      e[1] = bound2[i];
    }
    return e;
  }

  /** Given a coordinate position, find what grid element contains it.
    This means that
    <pre>
    edge[i] <= pos < edge[i+1] (if values are ascending)
    edge[i] > pos >= edge[i+1] (if values are descending)
    </pre>

    @param pos position in this coordinate system
    @return index of grid point containing it, or -1 if outside grid area
  */
  public int findCoordElement(double pos) {
    return findCoordElement(pos, -1);
  }


  /** Given a coordinate position, find what grid element contains it.
    This means that
    <pre>
    edge[i] <= pos < edge[i+1] (if values are ascending)
    edge[i] > pos >= edge[i+1] (if values are descending)
    </pre>

    @param pos position in this coordinate system
    @param lastIndex last position we looked for, or -1 if none
    @return index of grid point containing it, or -1 if outside grid area
  */
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

  /**
   * Given a coordinate position, find what grid element contains it, but always return valid index.
    @param pos position in this coordinate system
    @return index of grid point containing it
   */
  public int findCoordElementBounded(double pos) {
    return findCoordElementBounded(pos, -1);
  }

  /** Given a coordinate position, find what grid element contains it, but always return valid index.
    This means that
    <pre>
      if values are ascending:
        pos < edge[0] return 0
        edge[n] < pos return n-1
        edge[i] <= pos < edge[i+1] return i

      if values are descending:
        pos > edge[0] return 0
        edge[n] > pos return n-1
        edge[i] > pos >= edge[i+1] return i
    </pre>

    @param pos position in this coordinate system
    @param lastIndex last position we looked for, or -1 if none
    @return index of grid point containing it
  */
  public int findCoordElementBounded(double pos, int lastIndex) {
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis.findCoordElement() on non-numeric");

    if (axisType == AxisType.Lon) {
      for (int x=0; x < getSize(); x++) {
        if (LatLonPointImpl.betweenLon( pos, getCoordEdge(x), getCoordEdge(x+1)))
          return x;
      }
      return pos <= getCoordEdge(0) ? 0 : (int)getSize()-1;  // LOOK could screw up if pos not normalized to longitude interval
    }

    if (lastIndex < 0) lastIndex = (int) getSize()/2;
    int n = (int)getSize();

    if (isAscending) {

      if (pos < getCoordEdge(0))
        return 0;

      if (pos > getCoordEdge(n))
        return n-1;

      while (pos < getCoordEdge(lastIndex))
        lastIndex--;

      while (pos > getCoordEdge(lastIndex+1))
        lastIndex++;

      return lastIndex;

    } else {

      if (pos > getCoordEdge(0))
        return 0;

      if (pos < getCoordEdge(n))
        return n-1;

      while (pos > getCoordEdge(lastIndex))
        lastIndex--;

      while (pos < getCoordEdge(lastIndex+1))
        lastIndex++;

      return lastIndex;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////
  // checkIsRegular
  private boolean isRegular = false;
  private double start, increment;

  /** @return Starting value if isRegular() */
  public double getStart() {
    calcIsRegular();
    return start;
  }
  /** @return Increment value if isRegular() */
  public double getIncrement() {
    calcIsRegular();
    return increment;
  }


  private boolean isLayer = false;
  /**
   * Caution: many datasets do not explicitly specify this info, this is often a guess; default is false.
   * @return true if coordinate lies between a layer, or false if its at a point.
   */
  public boolean isLayer() { return isLayer; }
  /** Set if coordinate lies between a layer, or is at a point.
   * @param isLayer true if coordinate lies between a layer, or false if its at a point
   */
  public void setLayer(boolean isLayer) { this.isLayer = isLayer; }

  private void setIsLayer() {
    Attribute att = findAttribute(_Coordinate.ZisLayer) ;
    if ((att != null) && att.getStringValue().equalsIgnoreCase("true"))
      this.isLayer = true;
  }

  /**
   * If true, then value(i) = <i>getStart()</i> + i * <i>getIncrement()</i>.
   * @return if evenly spaced.
   */
  public boolean isRegular() {
    calcIsRegular();
    return isRegular;
  }

  private void calcIsRegular() {
    if (wasCalc) return;
    if (!wasRead) doRead();

    if (!isNumeric())
      isRegular = false;
    else if (getSize() < 2)
      isRegular = true;
    else {
      start = getCoordValue(0);
      int n = (int) getSize();
      increment = (getCoordValue(n-1) - getCoordValue(0))/(n-1);
      isRegular = true;
      for (int i=1; i< getSize(); i++)
        if (!ucar.nc2.util.Misc.closeEnough(getCoordValue(i) - getCoordValue(i-1), increment, 5.0e-3)) {
          isRegular = false;
          // double diff = Math.abs(getCoordValue(i) - getCoordValue(i-1) - increment);
          //System.out.println(i+" diff= "+getCoordValue(i)+" "+getCoordValue(i-1));
          break;
        }
    }
    wasCalc = true;
  }
  private boolean wasCalc = false;

  ///////////////////////////////////////////////////////////////////////////////

  private boolean isAscending;
  private boolean wasRead = false;
  private void doRead() {
    if (isNumeric()) {
      readValues();
      wasRead = true;

      if (getSize() < 2)
        isAscending = true;
      else
        isAscending = getCoordValue(0) < getCoordValue(1);

      //  calcIsRegular();
    } else if (getDataType() == DataType.STRING) {
      readStringValues();
      wasRead = true;
    } else {
      readCharValues();
      wasRead = true;
    }
  }

  private String[] names = null;
  private void readStringValues() {
    int count = 0;
    Array data;
    try {
      data = read();
    } catch (IOException ioe) {
      log.error("Error reading string coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }

    names = new String[ (int) data.getSize()];
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
    } catch (IOException ioe) {
      log.error("Error reading coordinate values ",ioe);
      throw new IllegalStateException(ioe);
    }

    IndexIterator iter = data.getIndexIterator();
    while (iter.hasNext())
      midpoint[count++] = iter.getDoubleNext();

    if (!makeBoundsFromAux()) {
      makeEdges();
      makeBoundsFromEdges();
    }
  }

  private double[] bound1, bound2;
  private boolean makeBoundsFromAux() {
    Attribute boundsAtt = findAttributeIgnoreCase("bounds");
    if ((null == boundsAtt) || !boundsAtt.isString()) return false;
    String boundsVarName = boundsAtt.getStringValue();
    Variable boundsVar = ncd.findVariable(boundsVarName);
    if (null == boundsVar) return false;
    if (2 != boundsVar.getRank()) return false;

    if (getDimension(0) != boundsVar.getDimension(0)) return false;
    if (2 != boundsVar.getDimension(1).getLength()) return false;

    Array data;
    try {
      data = boundsVar.read();
    } catch (IOException e) {
      log.warn("CoordinateAxis1D.hasBounds read failed ", e);
      return false;
    }
    
    // extract the bounds
    int n = shape[0];
    if (n < 2) return false;
    double[] value1 = new double[n];
    double[] value2 = new double[n];
    int[] shape = data.getShape();
    Index ima = data.getIndex();
    for (int i = 0; i < shape[0]; i++) {
      ima.set0(i);
      value1[i] = data.getDouble(ima.set1(0));
      value2[i] = data.getDouble(ima.set1(1));
    }

    // flip if needed
    boolean goesUp = (n < 2) || value1[1] > value1[0];
    boolean firstLower = value1[0] < value2[0];
    if (goesUp != firstLower) {
      double[] temp = value1;
      value1 = value2;
      value2 = temp;
    }

    // decide if they are contiguous
    boolean contig = true;
    for (int i = 0; i < n-1; i++) {
      if (!ucar.nc2.util.Misc.closeEnough(value1[i+1], value2[i]))
        contig = false;
    }

    if (contig) {
      edge = new double[n+1];
      edge[0] = value1[0];
      for(int i=1; i<n+1; i++)
        edge[i] = value2[i-1];
    } else {
      edge = new double[n+1];
      edge[0] = value1[0];
      for(int i=1; i<n; i++)
        edge[i] = (value1[i] + value2[i-1])/2;
      edge[n] = value2[n-1];
      setContiguous(false);
    }

    bound1 = value1;
    bound2 = value2;

    return true;
  }

  private void makeEdges() {
    int size = (int) getSize();
    edge = new double[size+1];
    if (size < 1) return;
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

  private void makeBoundsFromEdges() {
    int size = (int) getSize();
    if (size == 0) return;
    
    bound1 = new double[size];
    bound2 = new double[size];
    for(int i=0; i<size; i++) {
      bound1[i] = edge[i];
      bound2[i] = edge[i+1];
    }

    // flip if needed
    if (bound1[0] > bound2[0]) {
      double[] temp = bound1;
      bound1 = bound2;
      bound2 = temp;
    }
  }

  ////////////////////////////////////////////////////////////////////
  protected ArrayList<NamedObject> named;
  private void makeNames() {
    named = new ArrayList<NamedObject>();
    int n = (int) getSize();
    for (int i = 0; i < n; i++)
      named.add(new NamedAnything(getCoordName(i), getUnitsString()));
  }

  /**
   * Get the list of names, to be used for user selection.
   * The ith one refers to the ith coordinate.
   *
   * @return List of ucar.nc2.util.NamedObject, or empty list.
   */
  public List<NamedObject> getNames() {
    if (named == null) makeNames();
    return named;
  }

  /**
   * Get the index corresponding to the name. Reverse of getCoordName(i)
   * @param name getCoordName(i)
   * @return index, or -1 if not found
   */
  public int getIndex(String name) {
    if (named == null) makeNames();
    for (int i = 0; i < named.size(); i++) {
      NamedObject level = named.get(i);
      if (level.getName().trim().equals(name)) return i;
    }
    return -1;
  }

  protected static class NamedAnything implements NamedObject {
    private String name, desc;

    NamedAnything(String name, String desc) {
      this.name = name;
      this.desc = desc;
    }

    public String getName() { return name; }
    public String getDescription() { return desc; }
    public String toString() { return name; }
  }

}
