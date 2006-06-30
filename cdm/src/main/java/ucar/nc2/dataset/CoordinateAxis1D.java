// $Id: CoordinateAxis1D.java,v 1.19 2006/05/03 21:29:28 caron Exp $
/*
 * Copyright 2002-2004 Unidata Program Center/University Corporation for
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
import ucar.unidata.util.Format;
import ucar.unidata.geoloc.*;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A 1-dimensional Coordinate Axis. Its values must be monotonic.
 *
 * If this is string valued, it will have rank 2, otherwise it will have rank 1.
 * <p>
 * If string valued, only <i>getCoordName()</i> can be called.
 * <p>
 * If the coordinates are regularly spaced, <i>isRegular()</i> is true, and the values are equal to
 *   <i>getStart()</i> + i * <i>getIncrement()</i>.
 *
 * @see CoordinateAxis#factory
 * @author caron
 */

public class CoordinateAxis1D extends CoordinateAxis {


  /** create a 1D coordinate axis from an existing Variable */
  public CoordinateAxis1D(VariableDS vds) {
    super( vds);
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
  }

  /**
   * Create a new CoordinateAxis1D as a section of this CoordinateAxis1D.
   * @param r the section range
   * @return a new CoordinateAxis1D as a section of this CoordinateAxis1D
   * @throws InvalidRangeException
   */
  public CoordinateAxis1D section(Range r) throws InvalidRangeException {
    CoordinateAxis1D vs = new CoordinateAxis1D( this);
    ArrayList section = new ArrayList();
    section.add(r);
    makeSection( vs, section);
    return vs;
  }

  /** Create a 1D coordinate axis from NcML attributes.
    * @param ds the containing dataset.
    * @param group the containing group; if null, use rootGroup
    * @param name axis name.
    * @param type data type, must match nc2.DataType.
    * @param shapeS list of dimensions
    * @param units units of coordinates, preferably udunit compatible.
    * @param positive "true" if its a z axis with positive = up, else "false", null if dont know
    * @param boundaryRef name of variable name used as boundaries of this coordinate.
    *
  public CoordinateAxis1D( NetcdfDataset ds, Group group, String shortName, DataType type, String shapeS,
      String units, String positive, String boundaryRef) {

    super( ds, group, shortName, type, shapeS, units, positive, boundaryRef);
  }


  // for subclasses
  protected CoordinateAxis1D(NetcdfDataset dataset, Group group, Structure parentStructure, String shortName) {
    super(dataset, group, parentStructure, shortName);
  } */

  /** The "name" of the ith coordinate. If nominal, this is all there is to a coordinate.
   *  If numeric, this will return a String representation of the coordinate.
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

  /** Get the ith coordinate edge. This is the value where the underlying grid element switches
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
    return midpoint;
  }

  /** Get the coordinate edges as a double array; only use this if isContiguous() is true.
   *  @return coordinate edges.
   *  @exception UnsupportedOperationException if !isNumeric()
   */
  public double[] getCoordEdges() {
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis1D.getCoordEdges() on non-numeric");
    if (!wasRead) doRead();
    return edge;
  }

  /** Get the coordinate edges for the ith coordinate.
   *  Can use this for isContiguous() true or false.
   * @param i coordinate index
   * @return double[2] edges for ith coordinate
   */
  public double[] getCoordEdges(int i) {
    if (!wasRead) doRead();
    if (isContiguous()) {
      double[] e = new double[2];
      e[0] = getCoordEdge(i);
      e[1] = getCoordEdge(i+1);
      return e;
    } else
      throw new UnsupportedOperationException("not yet implemented");
  }

  /* public double getMinValue() {
    return Math.min( getCoordValue(0), getCoordValue( (int) getSize() - 1));
  }

  public double getMaxValue() {
    return Math.max( getCoordValue(0), getCoordValue( (int) getSize() - 1));
  } */

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
      return -1;
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

  /** Get Starting value if isRegular() */
  public double getStart() {
    calcIsRegular();
    return start;
  }
  /** Get Increment value if isRegular() */
  public double getIncrement() {
    calcIsRegular();
    return increment;
  }

  /**
   * If evenly spaced.
   * Then value(i) = <i>getStart()</i> + i * <i>getIncrement()</i>.
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
      increment = getCoordValue(1) - getCoordValue(0);
      isRegular = true;
      for (int i=1; i< getSize(); i++)
        if (!closeEnough(getCoordValue(i) - getCoordValue(i-1), increment)) {
          isRegular = false;
          //double diff = Math.abs(getCoordValue(i) - getCoordValue(i-1) - increment);
          //System.out.println(" diff= "+diff);
          break;
        }
    }
    wasCalc = true;
  }
  private boolean wasCalc = false;

  private boolean closeEnough( double d1, double d2) {
    return Math.abs(d2-d1) < 1.0e-4;
  }

  ///////////////////////////////////////////////////////////////////////////////

  private boolean isAscending;
  private boolean wasRead = false;
  private void doRead() {
    if (isNumeric()) {
      readValues();
      wasRead = true;
      isAscending = (getSize() < 2) || getCoordEdge(0) < getCoordEdge(1);
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
    } catch (IOException ioe) { return; }

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
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    IndexIterator iter = data.getIndexIterator();
    while (iter.hasNext())
      midpoint[count++] = iter.getDoubleNext();

    makeEdges();
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


  //////////////////////
  /* nicely formatted string representation
  public String getInfo() {
    StringBuffer buf = new StringBuffer(200);
    buf.append(getName());
    Format.tab(buf, 15, true);
    buf.append(getSize()+"");
    Format.tab(buf, 20, true);
    buf.append(getUnitString());
    if (axisType != null) {
      Format.tab(buf, 40, true);
      buf.append(axisType.toString());
    }
    Format.tab(buf, 47, true);
    buf.append(getDescription());


    /* if (isNumeric) {
      boolean debugCoords = ucar.util.prefs.ui.Debug.isSet("Dataset/showCoordValues");
      int ndigits = debugCoords ? 9 : 4;
      for (int i=0; i< getNumElements(); i++) {
        buf.append(Format.d(getCoordValue(i), ndigits));
        buf.append(" ");
      }
      if (debugCoords) {
        buf.append("\n      ");
        for (int i=0; i<=getNumElements(); i++) {
          buf.append(Format.d(getCoordEdge(i), ndigits));
          buf.append(" ");
        }
      }
    } else {
      for (int i=0; i< getNumElements(); i++) {
        buf.append(getCoordName(i));
        buf.append(" ");
      }
    }

    //buf.append("\n");
    return buf.toString();
  } */

}
