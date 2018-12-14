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
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.dataset.conv.COARDSConvention;
import ucar.nc2.time.Calendar;

import java.io.IOException;
import java.util.Formatter;

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
  static public int axisSizeToCache = 100 * 1000; // bytes

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
    if ((vds.getRank() == 1) || (vds.getRank() == 2 && vds.getDataType() == DataType.CHAR)) {
        return new CoordinateAxis1D(ncd, vds);
    } else if (vds.getRank() == 2)
      return new CoordinateAxis2D(ncd, vds);
    else
      return new CoordinateAxis(ncd, vds);
  }

  /**
   * Create a coordinate axis from an existing Variable.
   * General case.
   *
   * @param ncd the containing dataset
   * @param vds an existing Variable
   */
  protected CoordinateAxis(NetcdfDataset ncd, VariableDS vds) {
    super(vds, false);
    this.ncd = ncd;

    if (vds instanceof CoordinateAxis) {
      CoordinateAxis axis = (CoordinateAxis) vds;
      this.axisType = axis.axisType;
      this.boundaryRef = axis.boundaryRef;
      this.isContiguous = axis.isContiguous;
      this.positive = axis.positive;
    }
    setSizeToCache(axisSizeToCache);
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
    setSizeToCache(axisSizeToCache);
  }

  /**
   * Make a copy, with an independent cache.
   *
   * @return copy of this CoordinateAxis
   */
  public CoordinateAxis copyNoCache() {
    CoordinateAxis axis = new CoordinateAxis(ncd, getParentGroup(), getShortName(), getDataType(), getDimensionsString(),
            getUnitsString(), getDescription());

    // other state
    axis.axisType = this.axisType;
    axis.boundaryRef = this.boundaryRef;
    axis.isContiguous = this.isContiguous;
    axis.positive = this.positive;

    axis.cache = new Variable.Cache(); // decouple cache
    return axis;
  }

  // for section and slice

  @Override
  protected Variable copy() {
    return new CoordinateAxis(this.ncd, this);
  }

  /**
   * Get type of axis
   *
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

  @Override
  public String getUnitsString() {
    String units = super.getUnitsString();
    return units == null ? "" : units;
  }

  /**
   * Does the axis have numeric values.
   *
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
   * An interval coordinate consists of two numbers, bound1 and bound2.
   * The coordinate value must lie between them, but otherwise is somewhat arbitrary.
   * If not interval, then it has one number, the coordinate value.
   * @return true if its an interval coordinate.
   */
  public boolean isInterval() {
    return false; // interval detection is done in subclasses
  }

  // causes TDS ERROR thredds.server.opendap.NcDDS:  NcDDS: Variable time1_run missing coordinate variable in hash; dataset=fmrc/NCEP/GFS/Global_onedeg/NCEP-GFS-Global_onedeg_best.ncd
  //@Override
  //public boolean isCoordinateVariable() {
  //  return true;
  //}

  /*
   * Set if the edges are contiguous or disjoint.
   *
   * @param isContiguous true if the adjacent edges touch
   *
  protected void setContiguous(boolean isContiguous) {
    this.isContiguous = isContiguous;
  } */

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
   * The smallest coordinate value. Only call if isNumeric.
   *
   * @return the minimum coordinate value
   */
  public double getMinValue() {
    if (minmax == null) init();
    return minmax.min;
  }

  /**
   * The largest coordinate value. Only call if isNumeric.
   *
   * @return the maximum coordinate value
   */
  public double getMaxValue() {
    if (minmax == null) init();
    return minmax.max;
  }

  //////////////////////

  /**
   * Get a string representation
   *
   * @param buf place info here
   */
  public void getInfo(Formatter buf) {
    buf.format("%-30s", getNameAndDimensions());
    buf.format("%-20s", getUnitsString());
    if (axisType != null) {
      buf.format("%-10s", axisType.toString());
    }
    buf.format("%s", getDescription());

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
  }

  /**
   * Standard  sort on Coordinate Axes
   */
  static public class AxisComparator implements java.util.Comparator<CoordinateAxis> {
    public int compare(CoordinateAxis c1, CoordinateAxis c2) {

      AxisType t1 = c1.getAxisType();
      AxisType t2 = c2.getAxisType();

      if ((t1 == null) && (t2 == null))
        return c1.getShortName().compareTo(c2.getShortName());
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
      if (!getAxisType().equals(o.getAxisType())) return false;

    if (getPositive() != null)
      if (!getPositive().equals(o.getPositive())) return false;

    return true;
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  public int hashCode() {
    int result = super.hashCode();
    if (getAxisType() != null)
      result = 37 * result + getAxisType().hashCode();
    if (getPositive() != null)
      result = 37 * result + getPositive().hashCode();
    return result;
  }

  /////////////////////////////////////

  // needed by time coordinates
  protected ucar.nc2.time.Calendar getCalendarFromAttribute() {
    Attribute cal = findAttribute(CF.CALENDAR);
    String s = (cal == null) ? null : cal.getStringValue();
    if (s == null) {     // default for CF and COARDS
      Attribute convention = (ncd == null) ? null : ncd.getRootGroup().findAttribute(CDM.CONVENTIONS);
      if (convention != null) {
        String hasName = convention.getStringValue();
        int version = CF1Convention.getVersion(hasName);
        if (version >= 0) {
          return Calendar.gregorian;
          //if (version < 7 ) return Calendar.gregorian;
          //if (version >= 7 ) return Calendar.proleptic_gregorian; //
        }
        if (COARDSConvention.isMine(hasName)) return Calendar.gregorian;
      }
    }
    return ucar.nc2.time.Calendar.get(s);
  }

  @Override
  public boolean isCoordinateVariable() {
    return true;
  }

}
