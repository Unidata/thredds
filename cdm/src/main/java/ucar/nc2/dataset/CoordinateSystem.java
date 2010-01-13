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

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.ma2.DataType;

import java.util.*;

/**
 * A CoordinateSystem specifies the coordinates of a Variable's values.
 *
 *  Mathmatically it is a vector function F from index space to Sn:
 * <pre>
 *  F(i,j,k,...) -> (S1, S2, ...Sn)
 *  where i,j,k are integers, and S is the set of reals (R) or Strings.
 * </pre>
 *  The components of F are just its coordinate axes:
 *  <pre>
 *  F = (A1, A2, ...An)
 *    A1(i,j,k,...) -> S1
 *    A2(i,j,k,...) -> S1
 *    An(i,j,k,...) -> Sn
 * </pre>
 *
 *  Concretely, a CoordinateSystem is a set of coordinate axes, and an optional set
 *   of coordinate transforms.
 *  The domain rank of F is the number of dimensions it is a function of. The range rank is the number
 *   of coordinate axes.
 *
 * <p>
 * An important class of CoordinateSystems are <i>georeferencing</i> Coordinate Systems, that locate a
 *  Variable's values in space and time. A CoordinateSystem that has a Lat and Lon axis, or a GeoX and GeoY
 *  axis and a Projection CoordinateTransform will have <i>isGeoReferencing()</i> true.
 *  A CoordinateSystem that has a Height, Pressure, or GeoZ axis will have <i>hasVerticalAxis()</i> true.
 * <p>
 * Further CoordinateSystems specialization is done by "data type specific" clasess such as
 * ucar.nc2.dt.grid.GridCoordSys.
 *
 * @author caron
 * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/reference/CSObjectModel.html">Coordinate System Object Model</a>
 */
public class CoordinateSystem {

  /**
   * Create standard name from list of axes. Sort the axes first
   * @param axes list of CoordinateAxis
   * @return CoordinateSystem name, created from axes names
   */
  static public String makeName( List<CoordinateAxis> axes) {
    List<CoordinateAxis> axesSorted = new ArrayList<CoordinateAxis>( axes);
    Collections.sort( axesSorted, new CoordinateAxis.AxisComparator());
    StringBuilder buff = new StringBuilder();
    for (int i=0; i<axesSorted.size(); i++) {
      CoordinateAxis axis = axesSorted.get(i);
      if (i>0) buff.append(" ");
      buff.append( axis.getName());
    }
    return buff.toString();
  }

  protected NetcdfDataset ds;
  protected List<CoordinateAxis> coordAxes = new ArrayList<CoordinateAxis>();
  protected List<CoordinateTransform> coordTrans = new ArrayList<CoordinateTransform>();
  protected List<Dimension> domain = new ArrayList<Dimension>(); // set of dimension
  protected String name;
  protected CoordinateAxis xAxis, yAxis, zAxis, tAxis, latAxis, lonAxis, hAxis, pAxis, ensAxis;
  protected CoordinateAxis aziAxis, elevAxis, radialAxis;
  protected boolean isImplicit;
  protected String dataType; // Grid, Station, etc

  // subclasses
  protected CoordinateSystem() {}

  /**
   * Constructor.
   * @param ds the containing dataset
   * @param axes Collection of type CoordinateAxis, must be at least one.
   * @param coordTrans Collection of type CoordinateTransform, may be empty or null.
   */
  public CoordinateSystem(NetcdfDataset ds, Collection<CoordinateAxis> axes, Collection<CoordinateTransform> coordTrans) {
    this.ds = ds;
    this.coordAxes = new ArrayList<CoordinateAxis>( axes);
    this.name = makeName( coordAxes);

    if (coordTrans != null)
      this.coordTrans = new ArrayList<CoordinateTransform>( coordTrans);

    for (CoordinateAxis axis : coordAxes) {
      // look for AxisType
      AxisType axisType = axis.getAxisType();
      if (axisType != null) {
        if (axisType == AxisType.GeoX) xAxis = lesserRank(xAxis, axis);
        if (axisType == AxisType.GeoY) yAxis = lesserRank(yAxis, axis);
        if (axisType == AxisType.GeoZ) zAxis = lesserRank(zAxis, axis);
        if (axisType == AxisType.Time) tAxis = lesserRank(tAxis, axis);
        if (axisType == AxisType.Lat) latAxis = lesserRank(latAxis, axis);
        if (axisType == AxisType.Lon) lonAxis = lesserRank(lonAxis, axis);
        if (axisType == AxisType.Height) hAxis = lesserRank(hAxis, axis);
        if (axisType == AxisType.Pressure) pAxis = lesserRank(pAxis, axis);
        if (axisType == AxisType.Ensemble) ensAxis = lesserRank(ensAxis, axis);

        if (axisType == AxisType.RadialAzimuth) aziAxis = lesserRank(aziAxis, axis);
        if (axisType == AxisType.RadialDistance) radialAxis = lesserRank(radialAxis, axis);
        if (axisType == AxisType.RadialElevation) elevAxis = lesserRank(elevAxis, axis);
      }

      // collect dimensions
      List<Dimension> dims = axis.getDimensions();
      for (Dimension dim : dims) {
        if (!domain.contains(dim))
          domain.add(dim);
      }
    }
  }

  // prefer smaller ranks, in case more than one
  private CoordinateAxis lesserRank( CoordinateAxis a1, CoordinateAxis a2) {
    if (a1 == null) return a2;
    return (a1.getRank() <= a2.getRank()) ? a1 : a2;
  }

  /** add a CoordinateTransform
   * @param ct add this CoordinateTransform
   */
  public void addCoordinateTransform(CoordinateTransform ct) {
    coordTrans.add( ct);
    ds.addCoordinateTransform( ct);
  }

  /** add a Collection of CoordinateTransform
   * @param ct add all CoordinateTransform in this collection
   */
  public void addCoordinateTransforms(Collection<CoordinateTransform> ct) {
    if (ct != null)
      coordTrans.addAll( ct);
  }

  /**
   * get the List of CoordinateAxis objects
   * @return the List of CoordinateAxis objects
   * */
  public List<CoordinateAxis> getCoordinateAxes() { return coordAxes; }

  /**
   * get the List of CoordinateTransform objects
   * @return the List of CoordinateTransform objects
   */
  public List<CoordinateTransform> getCoordinateTransforms() { return coordTrans; }

  /**
   * get the name of the Coordinate System
   * @return the name of the Coordinate System
   */
  public String getName() { return name; }

  /**
   * Get the underlying NetcdfDataset
   * @return the underlying NetcdfDataset.
   */
  public NetcdfDataset getNetcdfDataset() { return ds; }

  /**
   * List of Dimensions that constitute the domain.
   * @return the List of Dimensions that constitute the domain.
   */
  public List<Dimension> getDomain() { return domain; }

  /**
   * Get the domain rank of the coordinate system = number of dimensions it is a function of.
   * @return domain.size()
   */
  public int getRankDomain() { return domain.size(); }

  /** Get the range rank of the coordinate system = number of coordinate axes.
   * @return coordAxes.size()
   */
  public int getRankRange() { return coordAxes.size(); }

  /* Scientific Data type, if known, eg Grid, Station, etc. Considered Experimental
  public String getDataType() { return dataType; }
  /* Set the Scientific Data type, eg Grid, Station, etc. Considered Experimental
  public void setDataType(String dataType) { this.dataType = dataType; } */

  ///////////////////////////////////////////////////////////////////////////
  // Convenience routines for finding georeferencing axes

  /**
   * Find the CoordinateAxis that has the given AxisType.
   * If more than one, return the one with lesser rank.
   * @param type look for this axisType
   * @return  CoordinateAxis of the given AxisType, else null.
   */
  public CoordinateAxis findAxis( AxisType type) {
    CoordinateAxis result = null;
    for (CoordinateAxis axis : coordAxes) {
      AxisType axisType = axis.getAxisType();
      if ((axisType != null) && (axisType == type))
        result = lesserRank(result, axis);
    }
    return result;
  }

  /** get the CoordinateAxis with AxisType.GeoX, or null if none.
   *  if more than one, choose one with smallest rank
   * @return axis of type AxisType.GeoX, or null if none
   */
  public CoordinateAxis getXaxis() { return xAxis; }

  /** get the CoordinateAxis with AxisType.GeoY, or null if none.
   *  if more than one, choose one with smallest rank
   * @return axis of type AxisType.GeoY, or null if none
   */
  public CoordinateAxis getYaxis() { return yAxis; }

  /** get the CoordinateAxis with AxisType.GeoZ, or null if none.
   *  if more than one, choose one with smallest rank
   * @return axis of type AxisType.GeoZ, or null if none
   */
  public CoordinateAxis getZaxis() { return zAxis; }

  /** get the CoordinateAxis with AxisType.Time, or null if none.
   *  if more than one, choose one with smallest rank
   * @return axis of type AxisType.Time, or null if none
   */
  public CoordinateAxis getTaxis() { return tAxis; }

  /** get the CoordinateAxis with AxisType.Lat, or null if none.
   *  if more than one, choose one with smallest rank
   * @return axis of type AxisType.Lat, or null if none
   */
  public CoordinateAxis getLatAxis() { return latAxis; }

  /** get the CoordinateAxis with AxisType.Lon, or null if none.
   *  if more than one, choose one with smallest rank *
   * @return axis of type AxisType.Lon, or null if none
   */
  public CoordinateAxis getLonAxis() { return lonAxis; }

  /** get the CoordinateAxis with AxisType.Height, or null if none.
   *  if more than one, choose one with smallest rank
   * @return axis of type AxisType.Height, or null if none
   */
  public CoordinateAxis getHeightAxis() { return hAxis; }

  /** get the CoordinateAxis with AxisType.Pressure, or null if none.
   *  if more than one, choose one with smallest rank.
   *  @return axis of type AxisType.Pressure, or null if none
   */
  public CoordinateAxis getPressureAxis() { return pAxis; }


  /** get the CoordinateAxis with AxisType.Ensemble, or null if none.
   *  if more than one, choose one with smallest rank.
   *  @return axis of type AxisType.Ensemble, or null if none
   */
  public CoordinateAxis getEnsembleAxis() { return ensAxis; }

  /** get the CoordinateAxis with AxisType.RadialAzimuth, or null if none.
   *  if more than one, choose one with smallest rank
   * @return axis of type AxisType.RadialAzimuth, or null if none
   */
  public CoordinateAxis getAzimuthAxis() { return aziAxis; }

  /** get the CoordinateAxis with AxisType.RadialDistance, or null if none.
   *  if more than one, choose one with smallest rank
    * @return axis of type AxisType.RadialDistance, or null if none
   */
  public CoordinateAxis getRadialAxis() { return radialAxis; }

  /** get the CoordinateAxis with AxisType.RadialElevation, or null if none.
   *  if more than one, choose one with smallest rank
    * @return axis of type AxisType.RadialElevation, or null if none
   */
  public CoordinateAxis getElevationAxis() { return elevAxis; }

  /**
   * Find the first ProjectionCT from the list of CoordinateTransforms.
   *  @return ProjectionCT or null if none.
   */
  public ProjectionCT getProjectionCT() {
    for (CoordinateTransform ct : coordTrans) {
      if (ct instanceof ProjectionCT)
        return (ProjectionCT) ct;
    }
    return null;
  }

  /** Get the Projection for this coordinate system.
   *  If isLatLon(), then returns a LatLonProjection. Otherwise, extracts the
   *  projection from any ProjectionCT CoordinateTransform.
   *  @return ProjectionImpl or null if none.
   */
  public ProjectionImpl getProjection() {
    if (projection == null) {
      if (isLatLon()) projection = new LatLonProjection();
      ProjectionCT projCT = getProjectionCT();
      if (null != projCT) projection = projCT.getProjection();
    }
    return projection;
  }
  private ProjectionImpl projection = null;

  ////////////////////////////////////////////////////////////////////////////
  // classification
  /**
   * true if it has X and Y CoordinateAxis, and a CoordTransform Projection
   * @return true if it has X and Y CoordinateAxis, and a CoordTransform Projection
   */
  public boolean isGeoXY() {
    if ((xAxis == null) || (yAxis == null)) return false;
    if (null == getProjection()) return false;
    if (projection instanceof LatLonProjection) return false;
    return true;
  }

  /**
   * true if it has Lat and Lon CoordinateAxis
   * @return true if it has Lat and Lon CoordinateAxis
   */
  public boolean isLatLon() { return (latAxis != null) && (lonAxis != null); }

  /** true if it has radial distance and azimuth CoordinateAxis
   * @return true if it has radial distance and azimuth CoordinateAxis
   */
  public boolean isRadial() { return (radialAxis != null) && (aziAxis != null); }

  /** true if isGeoXY or isLatLon
   * @return true if isGeoXY or isLatLon
   */
  public boolean isGeoReferencing() { return isGeoXY() || isLatLon(); }

  /**
   * true if all axes are CoordinateAxis1D
   * @return true if all axes are CoordinateAxis1D
   */
  public boolean isProductSet() {
    for (CoordinateAxis axis : coordAxes) {
      if (!(axis instanceof CoordinateAxis1D)) return false;
    }
    return true;
  }

  /** true if all axes are CoordinateAxis1D and are regular
   *
   * @return true if all axes are CoordinateAxis1D and are regular
   */
  public boolean isRegular() {
    for (CoordinateAxis axis : coordAxes) {
      if (!(axis instanceof CoordinateAxis1D)) return false;
      if (!((CoordinateAxis1D) axis).isRegular()) return false;
    }
    return true;
  }

  /**
   * Check if this Coordinate System is complete,
   *   ie if all its dimensions are also used by the Variable.
   * @param v check for this variable
   * @return true if all dimensions in V (including parents) are in the domain of this coordinate system.
   */
  public boolean isComplete(VariableIF v) {
    return /* isCoordinateSystemFor(v) && */ isSubset(v.getDimensionsAll(), domain);
  }

 /**
   * Check if this Coordinate System can be used for the given variable.
   * A CoordinateAxis can only be part of a Variable's CoordinateSystem if the CoordinateAxis' set of Dimensions is a
   * subset of the Variable's set of Dimensions.
   * So, a CoordinateSystem' set of Dimensions must be a subset of the Variable's set of Dimensions.
   * @param v check for this variable
   * @return true if all dimensions in the domain of this coordinate system are in V (including parents).
   */
 public boolean isCoordinateSystemFor(VariableIF v) {
   return isSubset(domain, v.getDimensionsAll());
 }

  /**
   * Test if all the Dimensions in subset are in set
   * @param subset is this a subset
   * @param set of this?
   * @return true if all the Dimensions in subset are in set
   */
  public static boolean isSubset(List<Dimension> subset, List<Dimension> set) {
    for (Dimension d : subset) {
      if (!(set.contains(d)))
        return false;
    }
    return true;
  }

  public static List<Dimension> makeDomain(Variable[] axes) {
    List<Dimension> domain = new ArrayList<Dimension>(10);
    for (Variable axis : axes) {
      for (Dimension dim : axis.getDimensions()) {
        if (!domain.contains(dim))
          domain.add(dim);
      }
    }
    return domain;
  }

  /**
   * Implicit Coordinate System are constructed based on which Coordinate Variables exist for the Dimensions of the Variable.
   * This is in contrast to a Coordinate System that is explicitly specified in the file.
   * @return true if this coordinate system was constructed implicitly.
   */
  public boolean isImplicit() { return isImplicit; }

  /** Set whether this Coordinate System is implicit
   * @param isImplicit true if constructed implicitly.
   */
  protected void setImplicit(boolean isImplicit) { this.isImplicit = isImplicit; }

  /** true if has Height, Pressure, or GeoZ axis
   * @return true if has a vertical axis
   */
  public boolean hasVerticalAxis() { return (hAxis != null) || (pAxis != null) || (zAxis != null); }

  /** true if has Time axis
   * @return true if has Time axis
   */
  public boolean hasTimeAxis() { return (tAxis != null); }

  /**
    * Do we have all the axes in the list?
    * @param wantAxes List of CoordinateAxis
    * @return true if all in our list.
    */
   public boolean containsAxes(List<CoordinateAxis> wantAxes) {
    for (CoordinateAxis ca : wantAxes) {
      if (!containsAxis(ca.getName()))
        return false;
    }
    return true;
   }

  /**
    * Do we have the named axis?
    * @param axisName (full) name of axis
    * @return true if we have an axis of that name
    */
   public boolean containsAxis(String axisName) {
    for (CoordinateAxis ca : coordAxes) {
      if (ca.getName().equals(axisName))
        return true;
    }
    return false;
   }

  /**
    * Do we have all the dimensions in the list?
    * @param wantDimensions List of Dimensions
    * @return true if all in our list.
    */
   public boolean containsDomain(List<Dimension> wantDimensions) {
    for (Dimension d : wantDimensions) {
      if (!domain.contains(d))
        return false;
    }
    return true;
   }

  /**
    * Do we have all the axes types in the list?
    * @param wantAxes List of AxisType
    * @return true if all in our list.
    */
   public boolean containsAxisTypes(List<AxisType> wantAxes) {
    for (AxisType wantAxisType : wantAxes) {
      if (!containsAxisType(wantAxisType)) return false;
    }
    return true;
   }


  /**
    * Do we have an axes of the given type?
    * @param wantAxisType want this AxisType
    * @return true if we have at least one axis of that type.
    */
   public boolean containsAxisType(AxisType wantAxisType) {
    for (CoordinateAxis ca : coordAxes) {
      if (ca.getAxisType() == wantAxisType) return true;
    }
    return false;
   }


  ////////////////////////////////////////////////////////////////////////////
  /**
   * Instances which have same name are equal.
   */
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if ( !(oo instanceof CoordinateSystem))
      return false;
    CoordinateSystem o = (CoordinateSystem) oo;

    if (!getName().equals(o.getName())) return false;

    List<CoordinateAxis> oaxes = o.getCoordinateAxes();
    for (CoordinateAxis axis : getCoordinateAxes()) {
      if (!oaxes.contains(axis)) return  false;
    }

    List<CoordinateTransform> otrans = o.getCoordinateTransforms();
    for (CoordinateTransform tran : getCoordinateTransforms()) {
      if (!otrans.contains(tran)) return  false;
    }

    return true;
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37*result + getName().hashCode();
      result = 37*result + getCoordinateAxes().hashCode();
      result = 37*result + getCoordinateTransforms().hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0;

  public String toString() { return name; }

}