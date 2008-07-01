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
import ucar.nc2.constants.AxisType;
import ucar.unidata.util.Format;

import java.io.IOException;

/**
 * A Coordinate Axis is a Variable that specifies one of the coordinates of a CoordinateSystem.
 * Mathematically it is a scalar function F from index space to S:
 * <pre>
 *  F:D -> S
 *  where D is a product set of dimensions (aka <i>index space</i>), and S is the set of reals (R) or Strings.
 * </pre>
 * <p/>
 * If its element type is char, it is considered a string-valued Coordinate Axis and rank is reduced by one,
 * since the outermost dimension is considered the string length: v(i, j, .., strlen).
 * If its element type is String, it is a string-valued Coordinate Axis.
 * Otherwise it is numeric-valued, and <i>isNumeric()</i> is true.
 * <p/>
 * The one-dimensional case F(i) -> R is the common case which affords important optimizations.
 * In that case, use the subtype CoordinateAxis1D. The factory methods will return
 * either a CoordinateAxis1D if the variable is one-dimensional, a CoordinateAxis2D if its 2D, or a
 * CoordinateAxis for the general case.
 * <p/>
 * A CoordinateAxis is optionally marked as georeferencing with an AxisType. It should have
 * a units string and optionally a description string.
 * <p/>
 * A Structure cannot be a CoordinateAxis, although members of Structures can.
 *
 * @author john caron
 */

public class CoordinateAxis extends VariableDS {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinateAxis.class);

  public final static String POSITIVE_UP = "up";
  public final static String POSITIVE_DOWN = "down";

  protected NetcdfDataset ncd; // container dataset
  protected AxisType axisType = null;
  protected String positive = null;
  protected String boundaryRef = null;
  protected boolean isContiguous = true;

  /**
   * Create a coordinate axis from an existing Variable.
   *
   * @param ncd the containing dataset
   * @param vds an existing Variable in dataset.
   * @return CoordinateAxis or one of its subclasses (CoordinateAxis1D, CoordinateAxis2D, or CoordinateAxis1DTime).
   */
  static public CoordinateAxis factory(NetcdfDataset ncd, VariableDS vds) {
    if ((vds.getRank() == 1) ||
        (vds.getRank() == 2 && vds.getDataType() == DataType.CHAR))
      return new CoordinateAxis1D(ncd, vds);
    else if (vds.getRank() == 2)
      return new CoordinateAxis2D(ncd, vds);
    else
      return new CoordinateAxis(ncd, vds);
  }

  /**
   * Create a coordinate axis from an existing Variable.
   *
   * @param ncd the containing dataset
   * @param vds an existing Variable
   */
  protected CoordinateAxis(NetcdfDataset ncd, VariableDS vds) {
    super(vds);
    this.ncd = ncd;

    if (vds instanceof CoordinateAxis) {
      CoordinateAxis axis = (CoordinateAxis) vds;
      this.axisType = axis.axisType;
      this.boundaryRef = axis.boundaryRef;
      this.isContiguous = axis.isContiguous;
      this.positive = axis.positive;
    }
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
  public CoordinateAxis(NetcdfDataset ds, Group group, String shortName, DataType dataType, String dims, String units, String desc) {
    super(ds, group, null, shortName, dataType, dims, units, desc);
    this.ncd = ds;
  }

  // for section and slice
  @Override
  protected Variable copy() {
    return new CoordinateAxis(this.ncd, this);
  }

  /**
   * @return type of axis, or null if none.
   */
  public AxisType getAxisType() {
    return axisType;
  }

  /**
   * Set type of axis, or null if none. Default is none.
   *
   * @param axisType set to this value
   */
  public void setAxisType(AxisType axisType) {
    this.axisType = axisType;
  }

  public String getUnitsString() {
    String units = super.getUnitsString();
    return units == null ? "" : units;
  }

  /**
   * @return true if the CoordAxis is numeric, false if its string valued ("nominal").
   */
  public boolean isNumeric() {
    return (getDataType() != DataType.CHAR) &&
        (getDataType() != DataType.STRING) &&
        (getDataType() != DataType.STRUCTURE);
  }

  /**
   * If the edges are contiguous or disjoint
   * Caution: many datasets do not explicitly specify this info, this is often a guess; default is true.
   *
   * @return true if the edges are contiguous or false if disjoint. Assumed true unless set otherwise.
   */
  public boolean isContiguous() {
    return isContiguous;
  }

  /**
   * Set if the edges are contiguous or disjoint.
   *
   * @param isContiguous true if the adjacent edges touch
   */
  public void setContiguous(boolean isContiguous) {
    this.isContiguous = isContiguous;
  }

  /**
   * Get the direction of increasing values, used only for vertical Axes.
   *
   * @return POSITIVE_UP, POSITIVE_DOWN, or null if unknown.
   */
  public String getPositive() {
    return positive;
  }

  /**
   * Set the direction of increasing values, used only for vertical Axes.
   *
   * @param positive POSITIVE_UP, POSITIVE_DOWN, or null if you dont know..
   */
  public void setPositive(String positive) {
    this.positive = positive;
  }

  /**
   * The name of this coordinate axis' boundary variable
   *
   * @return the name of this coordinate axis' boundary variable, or null if none.
   */
  public String getBoundaryRef() {
    return boundaryRef;
  }

  /**
   * Set a reference to a boundary variable.
   *
   * @param boundaryRef the name of a boundary coordinate variable in the same dataset.
   */
  public void setBoundaryRef(String boundaryRef) {
    this.boundaryRef = boundaryRef;
  }

  ////////////////////////////////

  private MAMath.MinMax minmax = null;

  private void init() {
    try {
      Array data = read();
      minmax = MAMath.getMinMax(data);
    } catch (IOException ioe) {
      log.error("Error reading coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }
  }

  /**
   * @return the minimum coordinate value
   */
  public double getMinValue() {
    if (minmax == null) init();
    return minmax.min;
  }

  /**
   * @return the maximum coordinate value
   */
  public double getMaxValue() {
    if (minmax == null) init();
    return minmax.max;
  }

  //////////////////////
  /**
   * @return formatted string representation
   */
  public String getInfo() {
    StringBuilder buf = new StringBuilder(200);
    buf.append(getName());
    Format.tab(buf, 15, true);
    buf.append(getSize()).append("");
    Format.tab(buf, 20, true);
    buf.append(getUnitsString());
    if (axisType != null) {
      Format.tab(buf, 40, true);
      buf.append("type=").append(axisType.toString());
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

  /**
   * Standard  sort on Coordinate Axes
   */
  static public class AxisComparator implements java.util.Comparator<CoordinateAxis> {
    public int compare(CoordinateAxis c1, CoordinateAxis c2) {

      AxisType t1 = c1.getAxisType();
      AxisType t2 = c2.getAxisType();

      if ((t1 == null) && (t2 == null))
        return c1.getName().compareTo(c2.getName());
      if (t1 == null)
        return -1;
      if (t2 == null)
        return 1;

      return t1.axisOrder() - t2.axisOrder();
    }

    public boolean equals(Object obj) {
      return (this == obj);
    }
  }

  /**
   * Instances which have same content are equal.
   */
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if (!(oo instanceof CoordinateAxis)) return false;
    if (!super.equals(oo)) return false;
    CoordinateAxis o = (CoordinateAxis) oo;

    if (getAxisType() != null)
      if(!getAxisType().equals(o.getAxisType())) return false;

    if (getPositive() != null)
      if(!getPositive().equals(o.getPositive())) return false;

    return true;
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = super.hashCode();
      if (getAxisType() != null)
        result = 37 * result + getAxisType().hashCode();
      if (getPositive() != null)
        result = 37 * result + getPositive().hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  private int hashCode = 0;

}