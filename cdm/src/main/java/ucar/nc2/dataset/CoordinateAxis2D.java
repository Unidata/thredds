// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

import java.io.IOException;
import java.util.ArrayList;

/**
 * A 2-dimensional numeric Coordinate Axis. Must be invertible meaning, roughly, that
 *   if you draw lines connecting the points, none would cross.
 *
 * @see CoordinateAxis#factory
 * @author john caron
 * @version $Revision$ $Date$
 */

public class CoordinateAxis2D extends CoordinateAxis {

  /** create a 2D coordinate axis from an existing Variable */
  public CoordinateAxis2D( VariableDS vds) {
    super( vds);
  }

  /** create a 2D coordinate axis from NcML attributes.
  public CoordinateAxis2D( NetcdfDataset ds, String name, String type, String shapeS, String units,
      String positive, String boundaryRef) {
    super( ds, name, type, shapeS, units, positive, boundaryRef);
  }

  // for subclasses
  protected CoordinateAxis2D(NetcdfDataset dataset, Group group, Structure parentStructure, String shortName) {
    super(dataset, group, parentStructure, shortName);
  } */


  /** Get the coordinate value at the i, j index.
   *  @param i index 0
   *  @param j index 1
   *  @return coordinate value.
   */
  public double getCoordValue(int i, int j) {
    if (data == null) doRead();
    dataIndex.set0(i);
    dataIndex.set1(j);
    return data.getDouble( dataIndex);
  }

  private double[] ddata = null;
  private Array data = null;
  private Index dataIndex = null;
  private void doRead() {
    try { data = read(); }
    catch (IOException ioe) { } // ??
    dataIndex = data.getIndex();
  }

  /** Get the coordinate values as a 1D double array.
   *  @return coordinate values
   *  @exception UnsupportedOperationException if !isNumeric()
   */
  public double[] getCoordValues() {
    if (!isNumeric())
       throw new UnsupportedOperationException("CoordinateAxis2D.getCoordValues() on non-numeric");
    if (ddata == null) {
      if (data == null) doRead();
      ddata = new double[(int)getSize()];
      IndexIterator iter = data.getIndexIterator();
      int count = 0;
      while (iter.hasNext()) {
        ddata[count++] = iter.getDoubleNext();
      }
    }
    return ddata;
  }

    /**
   * Create a new CoordinateAxis2D as a section of this CoordinateAxis2D.
   * @param r1 the section on the first index
   * @param r2 the section on the second index
   * @return a section of this CoordinateAxis2D
   * @throws InvalidRangeException
   */
  public CoordinateAxis2D section(Range r1, Range r2) throws InvalidRangeException {
    CoordinateAxis2D vs = new CoordinateAxis2D( this);
    ArrayList section = new ArrayList();
    section.add(r1);
    section.add(r2);
    makeSection( vs, section);
    return vs;
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