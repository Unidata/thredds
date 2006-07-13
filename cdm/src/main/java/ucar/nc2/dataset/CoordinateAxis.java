// $Id:CoordinateAxis.java 51 2006-07-12 17:13:13Z caron $
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
import ucar.nc2.*;
import ucar.unidata.util.Format;

import java.io.IOException;

/**
 * A Coordinate Axis is a Variable that specifies one of the coordinates of a CoordinateSystem.
 * Mathematically it is a scalar function F from index space to S:
 * <pre>
 *  F:D -> S
 *  where D is a product set of dimensions (aka <i>index space</i>), and S is the set of reals (R) or Strings.
 * </pre>
 *
 * If its element type is char, this it is a string-valued Coordinate Axis of rank-1,
 * where the outermost dimension is considered the string length: v(i, j, .. strlen);
 * Otherwise it is numeric-valued, and <i>isNumeric()</i> is true.
 * <p>
 * The one-dimensional case F(i) -> R is the common case which affords important optimizations.
 * In that case, use the subtype CoordinateAxis1D. The factory methods will return
 * either a CoordinateAxis1D if the variable is one-dimensional, a CoordinateAxis2D if its 2D, or a
 * CoordinateAxis for the general case.
 * <p>
 * A CoordinateAxis is optionally marked as georeferencing with an axisType. It should have
 * a units string and optionally a description string.
 *
 * @author john caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 * @see "NCML documentation"
 */

public class CoordinateAxis extends VariableDS {

  public final static String POSITIVE_UP = "up";
  public final static String POSITIVE_DOWN = "down";

  protected AxisType axisType = null;
  protected String positive = null;
  protected String boundaryRef = null;
  protected boolean isContiguous = true;

  /** Create a coordinate axis from an existing Variable.
   * @param dataset the containing dataset
   * @param vds an existing Variable in dataset.
   * @return CoordinateAxis1D, CoordinateAxis2D, or CoordinateAxis.
   */
  static public CoordinateAxis factory(NetcdfDataset dataset, VariableDS vds){
    if ((vds.getRank() == 1) ||
        (vds.getRank() == 2 && vds.getDataType() == DataType.CHAR))
      return new CoordinateAxis1D( vds);
    else if (vds.getRank() == 2)
      return new CoordinateAxis2D( vds);
    else
      return new CoordinateAxis( vds);
  }

    /************
    Create a coordinate axis from NcML attributes.
    * @param ds the containing dataset.
    * @param name axis name.
    * @param type data type, must match nc2.DataType.
    * @param shapeS list of dimensions
    * @param units units of coordinates, preferably udunit compatible.
    * @return CoordinateAxis or CoordinateAxis1D
    *
  static public CoordinateAxis factory( NetcdfDataset ds, String name, String type, String shapeS, String units) {
    return factory( ds, name, type, shapeS, units, null, null);
  } */


  /** Create a coordinate axis from NcML attributes.
    * @param ds the containing dataset.
    * @param group the containing group; if null, use rootGroup
    * @param shortName axis short name.
    * @param type data type, must match nc2.DataType.
    * @param shapeS list of dimensions
    * @param units units of coordinates, preferably udunit compatible.
    * @param positive "true" if its a z axis with positive = up, else "false"
    * @param boundaryRef name of variable name used as boundaries of this coordinate.
    *
  static public CoordinateAxis factory( NetcdfDataset ds, Group group, String shortName, DataType type,
      String shapeS, String units, String positive, String boundaryRef) {

    int dims = 0;
    StringTokenizer stoke = new StringTokenizer( shapeS);
    while (stoke.hasMoreTokens()) dims++;

    if ((dims == 1) || (dims == 2 && type == DataType.CHAR))
      return new CoordinateAxis( ds, group, shortName, type, shapeS, units, positive, boundaryRef);
    else
      return new CoordinateAxis1D( ds, group, shortName, type, shapeS, units, positive, boundaryRef);
  } */

  /* for subclasses
  protected CoordinateAxis(NetcdfDataset dataset, Group group, Structure parentStructure, String shortName) {
    super(dataset, group, parentStructure, shortName);
  } */


  /** Create a coordinate axis from an existing Variable.
   * @param vds an existing Variable in dataset.
   */
   protected CoordinateAxis(Variable vds) {
    super( null, vds, true);

    if (vds instanceof CoordinateAxis) {
      CoordinateAxis axis = (CoordinateAxis) vds;
      this.axisType = axis.axisType;
      this.positive = axis.positive;
    }
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
  public CoordinateAxis(NetcdfDataset ds, Group group, String shortName,
      DataType dataType, String dims, String units, String desc) {

    super( ds, group, null, shortName, dataType, dims, units, desc);
  }

    // for subclasses

  /** Get type of axis, or null if none. */
  public AxisType getAxisType() { return axisType; }
  /** Set type of axis, or null if none. Default is none. */
  public void setAxisType(AxisType axisType) { this.axisType = axisType; }

  public String getUnitsString() {
    String units = super.getUnitsString();
    return units == null ? "" : units;
  }

  /** If the CoordAxis is numeric or string valued. */
  public boolean isNumeric() {
    return (getDataType() != DataType.CHAR) &&
        (getDataType() != DataType.STRING) &&
        (getDataType() != DataType.STRUCTURE);
  }

   /** If the edges are contiguous or disjoint. Assumed true unless set otherwise. */
  public boolean isContiguous() { return isContiguous; }
   /** Set if the edges are contiguous or disjoint. */
  public void setContiguous(boolean isContiguous) { this.isContiguous = isContiguous; }

  /** Get the direction of increasing values, used only for vertical Axes.
   * @return POSITIVE_UP, POSITIVE_DOWN, or null if unknown.
   */
  public String getPositive() { return positive; }

  /** Set the direction of increasing values, used only for vertical Axes.
   *  Set to POSITIVE_UP, POSITIVE_DOWN, or null if you dont know..
   */
  public void setPositive( String positive) { this.positive = positive; }


  /**  Get the name of this coordinate axis' boundary variable, or null if none. */
  public String getBoundaryRef() { return boundaryRef; }

  /** Set a reference to a boundary variable.
   *  Must be name of boundary coordinate variable.
   */
  public void setBoundaryRef (String boundaryRef) { this.boundaryRef = boundaryRef; }


  /* //////////////////////////////////////////////////////////////////////////////
  private CoordinateAxis aux = null;
  private boolean isAuxiliary = false;

  /** Auxiliary information, eg edge or
  public void setAuxilaryAxis(CoordinateAxis aux) {
    this.aux = aux;
    aux.setIsAuxilary(true);
  }
  /** experimental; in NUWGConvention
  public boolean isAuxilary() { return isAuxiliary; }

  /** Set if it is an auxiliary axis.
  void setIsAuxilary(boolean isAuxiliary) { this.isAuxiliary = isAuxiliary; } */

  ////////////////////////////////
  protected boolean cacheOK() { return true; } // always cache

  private MAMath.MinMax minmax = null;
  private void init() {
    try {
      Array data = read();
      minmax = MAMath.getMinMax(data);
    } catch (IOException ioe) { /* what ?? */ }
  }

  /** Get the maximum coordinate value */
  public double getMinValue() {
    if (minmax == null) init();
    return minmax.min;
  }

  /** Get the minimum coordinate value */
  public double getMaxValue() {
    if (minmax == null) init();
    return minmax.max;
  }


  //////////////////////
  /** formatted string representation */
  public String getInfo() {
    StringBuffer buf = new StringBuffer(200);
    buf.append(getName());
    Format.tab(buf, 15, true);
    buf.append(getSize()+"");
    Format.tab(buf, 20, true);
    buf.append(getUnitsString());
    if (axisType != null) {
      Format.tab(buf, 40, true);
      buf.append("type="+axisType.toString());
    }
    Format.tab(buf, 52, true);
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
    } */

    //buf.append("\n");
    return buf.toString();
  }

  /** Standard  sort on Coordinate Axes */
  static public class AxisComparator implements java.util.Comparator {
    public int compare(Object o1, Object o2) {
      CoordinateAxis c1 = (CoordinateAxis) o1;
      CoordinateAxis c2 = (CoordinateAxis) o2;

      AxisType t1 = c1.getAxisType();
      AxisType t2 = c2.getAxisType();

      if ((t1 == null) && (t2 == null))
        return c1.getName().compareTo( c2.getName());
      if (t1 == null)
        return -1;
      if (t2 == null)
        return 1;

      return t1.compareTo( t2);
    }
    public boolean equals(Object obj) { return (this == obj); }
  }

  /**
   * Instances which have same content are equal.
   */
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if ( !(oo instanceof CoordinateAxis)) return false;
    return hashCode() == oo.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = super.hashCode();
      if( getAxisType() != null)
        result = 37*result + getAxisType().hashCode();
      if( getPositive() != null)
        result = 37*result + getPositive().hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0;
}