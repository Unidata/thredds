// $Id: GridCoordSys.java,v 1.39 2006/07/14 20:10:19 caron Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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
package ucar.nc2.dataset.grid;

import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.util.NamedObject;
import ucar.nc2.units.*;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.vertical.*;
import ucar.ma2.*;
import ucar.units.ConversionException;

import java.util.*;
import java.io.IOException;

import thredds.datatype.DateRange;

/**
 * A georeferencing "gridded" CoordinateSystem. This describes a "grid" of coordinates, which
 * implies a connected topology such that values next to each other in index space are next to
 * each other in coordinate space.
 * Note: these classes should be considered experimental and will likely be refactored in the next release.
 * <p/>
 * This currently assumes that the CoordinateSystem
 * <ol>
 * <li> is georeferencing (has Lat, Lon or GeoX, GeoY axes)
 * <li> x, y are 1 or 2-dimensional axes.
 * <li> z, t are 1-dimensional axes.
 * </ol>
 * <p/>
 * This is the common case for georeferencing coordinate systems. Mathematically it is a product set:
 * {X,Y} x {Z} x {T}. The x and y axes may be 1 or 2 dimensional.
 * <p/>
 * <p/>
 * A CoordinateSystem may have multiple horizontal and vertical axes. GridCoordSys chooses one
 * axis corresponding to X, Y, Z, and T. It gives preference to one dimensional axes (CoordinateAxis1D).
 *
 * @deprecated (use ucar.nc2.dt.grid)
 * @author caron
 * @version $Revision: 1.39 $ $Date: 2006/07/14 20:10:19 $
 */

public class GridCoordSys extends CoordinateSystem {
  private static SimpleUnit kmUnit = SimpleUnit.factory("km");

  /**
   * Determine if this CoordinateSystem can be made into a GridCoordSys.
   * This currently assumes that the CoordinateSystem:
   * <ol>
   * <li> is georeferencing (cs.isGeoReferencing())
   * <li> x, y are 1 or 2-dimensional axes.
   * <li> z, t, if they exist, are 1-dimensional axes.
   * <li> domain rank > 1
   * </ol>
   *
   * @param cs the CoordinateSystem to test
   * @return true if it can be made into a GridCoordSys.
   * @see CoordinateSystem#isGeoReferencing
   */
  public static boolean isGridCoordSys(StringBuffer sbuff, CoordinateSystem cs) {
    if (cs.getRankDomain() < 2) {
      if (sbuff != null) sbuff.append(cs.getName()+" domain rank < 2\n");
      return false;
    }

    if (!cs.isLatLon()) {
      // do check for GeoXY ourself
      if ((cs.getXaxis() == null) || (cs.getYaxis() == null)) {
        if (sbuff != null) sbuff.append(cs.getName()+" NO Lat,Lon or X,Y axis\n");
        return false;
      }
      if (null == cs.getProjection()) {
        if (sbuff != null) sbuff.append(cs.getName()+" NO projection found\n");
        return false;
      }
    }

    CoordinateAxis xaxis = null, yaxis = null;
    if (cs.isGeoXY()) {
      xaxis = cs.getXaxis();
      yaxis = cs.getYaxis();
      if (!kmUnit.isCompatible(xaxis.getUnitsString())) {
        sbuff.append(cs.getName()+" X axis units must be convertible to km\n");
        return false;
      }
      if (!kmUnit.isCompatible(yaxis.getUnitsString())) {
        sbuff.append(cs.getName()+" Y axis units must be convertible to km\n");
        return false;
      }
    } else if (cs.isLatLon()) {
      xaxis = cs.getLonAxis();
      yaxis = cs.getLatAxis();
    }

    // check ranks
    if ((xaxis.getRank() > 2) || (yaxis.getRank() > 2)) {
      if (sbuff != null) sbuff.append(cs.getName()+" X or Y axis rank must be <= 2\n");
      return false;
    }

    int countRangeRank = 2;

    CoordinateAxis z = cs.getHeightAxis();
    if ((z == null) || !(z instanceof CoordinateAxis1D)) z = cs.getPressureAxis();
    if ((z == null) || !(z instanceof CoordinateAxis1D)) z = cs.getZaxis();
    if ((z != null) && !(z instanceof CoordinateAxis1D)) {
      if (sbuff != null) sbuff.append(cs.getName()+" Z axis must be 1D\n");
      return false;
    }

    CoordinateAxis t = cs.getTaxis();
    if ((t != null) && !(t instanceof CoordinateAxis1D)) {
      if (sbuff != null) sbuff.append(cs.getName()+" T axis must be 1D\n");
      return false;
    }
    if (t != null) countRangeRank++;


    /* if (cs.getRankDomain() < countRangeRank) {
      if (sbuff != null) sbuff.append(" domain rank "+ cs.getRankDomain()+" < range rank "+countRangeRank+" \n");
      return false;
    } */

    return true;
  }

  /**
   * Determine if the CoordinateSystem cs can be made into a GridCoordSys for the Variable v.
   *
   * @param sbuff put debug information into this StringBuffer; may be null.
   * @param cs    CoordinateSystem to check.
   * @param v     Variable to check.
   * @return the GridCoordSys made from cs, else null.
   */
  public static GridCoordSys makeGridCoordSys(StringBuffer sbuff, CoordinateSystem cs, VariableEnhanced v) {
    if (sbuff != null) {
      sbuff.append(" ");
      v.getNameAndDimensions(sbuff, true, false);
      sbuff.append(" check CS " + cs.getName());
    }
    if (isGridCoordSys(sbuff, cs)) {
      GridCoordSys gcs = new GridCoordSys(cs);
      if (gcs.isComplete(v)) {
        if (sbuff != null) sbuff.append(" OK\n");
        return gcs;
      } else {
        if (sbuff != null) sbuff.append(" NOT complete\n");
      }
    }

    return null;
  }


  /////////////////////////////////////////////////////////////////////////////
  private ProjectionImpl proj;
  private CoordinateAxis horizXaxis, horizYaxis;
  private CoordinateAxis1D vertZaxis, timeTaxis;
  private VerticalCT vCT;
  private Dimension timeDim;

  private boolean isDate = false;
  private boolean isLatLon = false;
  private ArrayList levels = null;
  private ArrayList times = null;
  private Date[] timeDates = null;

  /**
   * Create a GridCoordSys from an existing Coordinate System.
   * This will choose which axes are the XHoriz, YHoriz, Vertical, and Time.
   * If theres a Projection, it will set its map area
   */
  public GridCoordSys(CoordinateSystem cs) {
    super();
    //super( cs.getCoordinateAxes(), cs.getCoordinateTransforms());

    if (cs.isGeoXY()) {
      horizXaxis = xAxis = cs.getXaxis();
      horizYaxis = yAxis = cs.getYaxis();
      // LOOK shold we make a copy of the axes here, so original CS stays intact ??
      convertUnits( horizXaxis);
      convertUnits( horizYaxis);
    } else if (cs.isLatLon()) {
      horizXaxis = lonAxis = cs.getLonAxis();
      horizYaxis = latAxis = cs.getLatAxis();
      isLatLon = true;
    } else
      throw new IllegalArgumentException("CoordinateSystem is not geoReferencing");

    coordAxes.add(horizXaxis);
    coordAxes.add(horizYaxis);
    proj = cs.getProjection();

    // set canonical area
    if (proj != null)
      proj.setDefaultMapArea(getBoundingBox());

    // need to generalize to non 1D vertical.
    CoordinateAxis z = hAxis = cs.getHeightAxis();
    if ((z == null) || !(z instanceof CoordinateAxis1D)) z = pAxis = cs.getPressureAxis();
    if ((z == null) || !(z instanceof CoordinateAxis1D)) z = zAxis = cs.getZaxis();
    if ((z != null) && !(z instanceof CoordinateAxis1D)) z = null;
    if (z != null) {
      vertZaxis = (CoordinateAxis1D) z;
      coordAxes.add(vertZaxis);
    } else {
      hAxis = pAxis = zAxis = null;
    }

    // look for VerticalCT
    List list = cs.getCoordinateTransforms();
    for (int i = 0; i < list.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) list.get(i);
      if (ct instanceof VerticalCT) {
        vCT = (VerticalCT) ct;
        break;
      }
    }

    // need to generalize to non 1D time?.
    CoordinateAxis t = cs.getTaxis();
    if ((t != null) && (t instanceof CoordinateAxis1D)) {
      tAxis = t;
      timeTaxis = (CoordinateAxis1D) t;
      coordAxes.add(timeTaxis);
      timeDim = t.getDimension(0);
    }

    // make name based on coordinate
    Collections.sort(coordAxes, new CoordinateAxis.AxisComparator()); // canonical ordering of axes
    this.name = makeName(coordAxes);

    // copy all coordfinate transforms into here
    this.coordTrans = new ArrayList( cs.getCoordinateTransforms());

    // collect dimensions
    for (int i = 0; i < coordAxes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) coordAxes.get(i);
      List dims = axis.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        Dimension dim = (Dimension) dims.get(j);
        if (!domain.contains(dim))
          domain.add(dim);
      }
    }

    makeLevels();
    makeTimes();
  }

  /**
   * Create a GridCoordSys as a section of an existing GridCoordSys.
   * This will create sections of the corresponding CoordinateAxes.
   *
   * @param from copy this GridCoordSys
   * @param t_range subset the time dimension, or null if you want all of it
   * @param z_range subset the vertical dimension, or null if you want all of it
   * @param y_range subset the y dimension, or null if you want all of it
   * @param x_range subset the x dimension, or null if you want all of it
   * @throws InvalidRangeException
   */
  public GridCoordSys(GridCoordSys from, Range t_range, Range z_range, Range y_range, Range x_range) throws InvalidRangeException {
    super();

    CoordinateAxis xaxis = from.getXHorizAxis();
    CoordinateAxis yaxis = from.getYHorizAxis();

    if ((xaxis instanceof CoordinateAxis1D) && (yaxis instanceof CoordinateAxis1D)) {
      CoordinateAxis1D xaxis1 = (CoordinateAxis1D) xaxis;
      CoordinateAxis1D yaxis1 = (CoordinateAxis1D) yaxis;

      horizXaxis = (x_range == null) ? xaxis1 : xaxis1.section( x_range);
      horizYaxis = (y_range == null) ? yaxis : yaxis1.section( y_range);

    } else if ((xaxis instanceof CoordinateAxis2D) && (yaxis instanceof CoordinateAxis2D) && from.isLatLon()) {
      CoordinateAxis2D lon_axis = (CoordinateAxis2D) xaxis;
      CoordinateAxis2D lat_axis = (CoordinateAxis2D) yaxis;

      horizXaxis = lon_axis.section( y_range, x_range);
      horizYaxis = lat_axis.section( y_range, x_range);
    } else
      throw new IllegalArgumentException("must be 1D or 2D/LatLon ");

    if (from.isGeoXY()) {
      xAxis = horizXaxis;
      yAxis = horizYaxis;
    } else {
      lonAxis = horizXaxis;
      latAxis = horizYaxis;
      isLatLon = true;
    }

    coordAxes.add(horizXaxis);
    coordAxes.add(horizYaxis);
    proj = (ProjectionImpl) from.getProjection().clone();

    // set canonical area
    if ((proj != null) && (getBoundingBox() != null))
      proj.setDefaultMapArea(getBoundingBox());

    CoordinateAxis1D zaxis = from.getVerticalAxis();
    if (zaxis != null) {
      vertZaxis = (z_range == null) ? zaxis : zaxis.section( z_range);
      coordAxes.add(vertZaxis);
      // LOOK assign hAxis, pAxis or zAxis
    }

    if (from.getVerticalTransform2() != null) {
      VerticalTransform vt = from.getVerticalTransform();
      if (vt != null)
        vt = vt.subset(t_range, z_range, y_range, x_range);

      vCT = new VerticalCT(from.getVerticalTransform2());
      vCT.setVerticalTransform(vt);
    }

    CoordinateAxis1D taxis = from.getTimeAxis();
    if (taxis != null) {
      tAxis = timeTaxis = (t_range == null) ? taxis : taxis.section( t_range);
      coordAxes.add(timeTaxis);
      timeDim = timeTaxis.getDimension(0);
    }

    // make name based on coordinate
    Collections.sort(coordAxes, new CoordinateAxis.AxisComparator()); // canonical ordering of axes
    this.name = makeName(coordAxes);

    this.coordTrans = new ArrayList( from.getCoordinateTransforms());

    // collect dimensions
    for (int i = 0; i < coordAxes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) coordAxes.get(i);
      List dims = axis.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        Dimension dim = (Dimension) dims.get(j);
        dim.setShared( true); // make them shared (section will make them unshared)
        if (!domain.contains(dim))
          domain.add(dim);
      }
    }

    makeLevels();
    makeTimes();
  }

  private void convertUnits(CoordinateAxis axis) {
    String units = axis.getUnitsString();
    SimpleUnit axisUnit = SimpleUnit.factory(units);
    double factor;
    try {
      factor =  axisUnit.convertTo(1.0, kmUnit);
    } catch (ConversionException e) {
      e.printStackTrace();
      return;
    }
    if (factor == 1.0) return;

    Array data;
    try {
      data = axis.read();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext())
      ii.setDoubleCurrent( factor * ii.getDoubleNext());

    axis.setCachedData( data, false);
    axis.setUnitsString("km");
  }

  //private VerticalTransform vt = null;

  /**
   * Get the vertical transform, if any.
   */
  public VerticalTransform getVerticalTransform() {
    return vCT == null ? null : vCT.getVerticalTransform();
  }

  public VerticalCT getVerticalTransform2() {
    return vCT;
  }

  // we have to delay making these, since we dont identify the dimensions specifically until now
  void makeVerticalTransform(GridDataset gds, StringBuffer parseInfo) {
    if (vCT == null) return;
    if (vCT.getVerticalTransform() != null) return; // already done

    vCT.makeVerticalTransform(gds.getNetcdfDataset(), timeDim);

    if (vCT.getVerticalTransform() == null)
      parseInfo.append("  - CAN'T make VerticalTransform = " + vCT.getVerticalTransformType() + "\n");
    else
      parseInfo.append("  - makeVerticalTransform = " + vCT.getVerticalTransformType() + "\n");
  }


  /** Create a GridCoordSys from an existing Coordinate System and explcitly set
   *  which axes are the x, y, z, and time axes.
   *
   public GridCoordSys( CoordinateSystem cs, CoordinateAxis xaxis, CoordinateAxis yaxis,
   CoordinateAxis1D zaxis, CoordinateAxis1D taxis) {
   super( cs.getCoordinateAxes(), cs.getCoordinateTransforms());

   if (!isGeoReferencing())
   throw new IllegalArgumentException("CoordinateSystem is not geoReferencing");

   this.xaxis = xaxis;
   this.yaxis = yaxis;
   this.zaxis = zaxis;
   this.taxis = taxis;

   makeLevels();
   makeTimes();
   } */

  /**
   * get the X Horizontal axis (either GeoX or Lon)
   */
  public CoordinateAxis getXHorizAxis() { return horizXaxis; }

  /**
   * get the Y Horizontal axis (either GeoY or Lat)
   */
  public CoordinateAxis getYHorizAxis() { return horizYaxis; }

  /**
   * get the Vertical axis (either Geoz, Height, or Pressure)
   */
  public CoordinateAxis1D getVerticalAxis() { return vertZaxis; }

  /**
   * get the Time axis (same as getTaxis())
   */
  public CoordinateAxis1D getTimeAxis() { return timeTaxis; }

  /**
   * get the projection
   */
  public ProjectionImpl getProjection() { return proj; }

  /**
   * Get the list of level names, to be used for user selection.
   * The ith one refers to the ith level coordinate.
   *
   * @return ArrayList of ucar.nc2.util.NamedObject, or empty list.
   */
  public ArrayList getLevels() { return levels; }

  /**
   * Get the list of time names, to be used for user selection.
   * The ith one refers to the ith time coordinate.
   *
   * @return ArrayList of ucar.nc2.util.NamedObject, or empty list.
   */
  public ArrayList getTimes() { return times; }

  /**
   * Get the list of times as Dates. Only valid if isDate() is true;
   *
   * @return array of java.util.Date, or null.
   */
  public java.util.Date[] getTimeDates() { return timeDates; }

  /**
   * is this a Lat/Lon coordinate system?
   */
  public boolean isLatLon() { return isLatLon; }

  /**
   * is there a time coordinate, and can it be expressed as a Date?
   */
  public boolean isDate() { return isDate; }

  /**
   * true if increasing z coordinate values means "up" in altitude
   */
  public boolean isZPositive() {
    if (vertZaxis == null) return false;
    if (vertZaxis.getPositive() != null) {
      return vertZaxis.getPositive().equalsIgnoreCase(CoordinateAxis.POSITIVE_UP);
    }
    if (vertZaxis.getAxisType() == AxisType.Height) return true;
    if (vertZaxis.getAxisType() == AxisType.Pressure) return false;
    return true; // default
  }

  /**
   * true if all spatial axes are CoordinateAxis1D and are regular
   */
  public boolean isRegularSpatial() {
    if (!isRegularSpatial(getXHorizAxis())) return false;
    if (!isRegularSpatial(getYHorizAxis())) return false;
    // if (!isRegularSpatial(getVerticalAxis())) return false;  // LOOK! removed for the sake of WCS server
    return true;
  }

  private boolean isRegularSpatial(CoordinateAxis axis) {
    if (axis == null) return true;
    if (!(axis instanceof CoordinateAxis1D)) return false;
    if (!((CoordinateAxis1D) axis).isRegular()) return false;
    return true;
  }

  /**
   * Given a point in x,y coordinate space, find the x,y index in the coordinate system.
   * Not implemented yet for 2D.
   *
   * @param xpos   position in x coordinate space.
   * @param ypos   position in y coordinate space.
   * @param result put result in here, may be null
   * @return int[2], 0=x,1=y indices in the coordinate system of the point. These will be -1 if out of range.
   */
  public int[] findXYCoordElement(double xpos, double ypos, int[] result) {
    if (result == null)
      result = new int[2];

    if ((horizXaxis instanceof CoordinateAxis1D) && (horizYaxis instanceof CoordinateAxis1D)) {
      result[0] = ((CoordinateAxis1D) horizXaxis).findCoordElement(xpos);
      result[1] = ((CoordinateAxis1D) horizYaxis).findCoordElement(ypos);
      return result;
    } else if ((horizXaxis instanceof CoordinateAxis2D) && (horizYaxis instanceof CoordinateAxis2D)) {
      result[0] = -1;
      result[1] = -1;
      return result;
      //return ((CoordinateAxis2D) xaxis).findXYCoordElement( xpos, ypos, result);
    }

    // cant happen
    throw new IllegalStateException("GridCoordSystem.findXYCoordElement");
  }


  /**
   * Given a Date, find the corresponding time index on the time coordinate axis.
   * Can only call this is hasDate() is true.
   * This will return
   * <ul>
   * <li> i, if time(i) <= d < time(i+1).
   * <li> -1, if d < time(0)
   * <li> n-1, if d > time(n-1),  where n is length of time coordinates
   * </ul>
   *
   * @param d date to look for
   * @return corresponding time index on the time coordinate axis
   * @throws UnsupportedOperationException is no time axis or isDate() false
   */
  public int findTimeCoordElement(java.util.Date d) {
    if (timeTaxis == null || !isDate())
      throw new UnsupportedOperationException("GridCoordSys: ti");

    int n = (int) timeTaxis.getSize();
    long m = d.getTime();
    int index = 0;
    while (index < n) {
      if (m < timeDates[index].getTime())
        break;
      index++;
    }
    return index - 1;
  }

  /**
   * Get the String name for the ith level(z) coordinate.
   *
   * @param index which level coordinate
   * @return level name
   */
  public String getLevelName(int index) {
    if ((vertZaxis == null) || (index < 0) || (index >= vertZaxis.getSize()))
      throw new IllegalArgumentException("getLevelName = " + index);

    NamedAnything name = (NamedAnything) levels.get(index);
    return name.getName();
  }

  /**
   * Get the index corresponding to the level name.
   *
   * @param name level name
   * @return level index, or -1 if not found
   */
  public int getLevelIndex(String name) {
    if ((vertZaxis == null) || (name == null)) return -1;

    for (int i = 0; i < levels.size(); i++) {
      NamedAnything level = (NamedAnything) levels.get(i);
      if (level.getName().trim().equals(name)) return i;
    }
    return -1;
  }

  /**
   * Get the string name for the ith time coordinate.
   *
   * @param index which time coordinate
   * @return time name.
   */
  public String getTimeName(int index) {
    if ((timeTaxis == null) || (index < 0) || (index >= timeTaxis.getSize()))
      throw new IllegalArgumentException("getTimeName = " + index);

    NamedAnything name = (NamedAnything) times.get(index);
    return name.getName();
  }

  /**
   * Get the index corresponding to the level name.
   *
   * @param name level name
   * @return level index, or -1 if not found
   */
  public int getTimeIndex(String name) {
    if ((timeTaxis == null) || (name == null)) return -1;

    for (int i = 0; i < times.size(); i++) {
      NamedAnything time = (NamedAnything) times.get(i);
      if (time.getName().trim().equals(name)) return i;
    }
    return -1;
  }

  public DateRange getDateRange() {
    if (isDate()) {
      Date[] dates = getTimeDates();
      return new DateRange(dates[0], dates[dates.length - 1]);
    }

    return null;
  }

  public DateUnit getDateUnit() throws Exception {
    String tUnits = getTimeAxis().getUnitsString();
    return (DateUnit) SimpleUnit.factory(tUnits);
  }

  /** only if isRegular() */
  public TimeUnit getTimeResolution() throws Exception {
    CoordinateAxis1D taxis = getTimeAxis();
    String tUnits = taxis.getUnitsString();
    StringTokenizer stoker = new StringTokenizer( tUnits);
    double tResolution = taxis.getIncrement();
    return new TimeUnit( tResolution, stoker.nextToken());
  }

  private ProjectionRect mapArea = null;

  /**
   * Get the x,y bounding box in projection coordinates.
   */
  public ProjectionRect getBoundingBox() {
    if (mapArea == null) {

      if ((horizXaxis == null) || !horizXaxis.isNumeric() || (horizYaxis == null) || !horizYaxis.isNumeric())
        return null; // impossible

      // x,y may be 2D
      if (!(horizXaxis instanceof CoordinateAxis1D) || !(horizYaxis instanceof CoordinateAxis1D)) {
        mapArea = new ProjectionRect(horizXaxis.getMinValue(), horizYaxis.getMinValue(),
            horizXaxis.getMaxValue(), horizYaxis.getMaxValue());

      } else {

        CoordinateAxis1D xaxis1 = (CoordinateAxis1D) horizXaxis;
        CoordinateAxis1D yaxis1 = (CoordinateAxis1D) horizYaxis;

        /* add one percent on each side if its a projection. WHY?
        double dx = 0.0, dy = 0.0;
        if (!isLatLon()) {
          dx = .01 * (xaxis1.getCoordEdge((int) xaxis1.getSize()) - xaxis1.getCoordEdge(0));
          dy = .01 * (yaxis1.getCoordEdge((int) yaxis1.getSize()) - yaxis1.getCoordEdge(0));
        }

        mapArea = new ProjectionRect(xaxis1.getCoordEdge(0) - dx, yaxis1.getCoordEdge(0) - dy,
            xaxis1.getCoordEdge((int) xaxis1.getSize()) + dx,
            yaxis1.getCoordEdge((int) yaxis1.getSize()) + dy); */

        mapArea = new ProjectionRect(xaxis1.getCoordEdge(0), yaxis1.getCoordEdge(0),
            xaxis1.getCoordEdge((int) xaxis1.getSize()),
            yaxis1.getCoordEdge((int) yaxis1.getSize()));
      }
    }

    return mapArea;
  }

  private LatLonRect llbb = null;

  /**
   * Get horizontal bounding box in lat, lon coordinates.
   *
   * @return lat, lon bounding box.
   */
  public LatLonRect getLatLonBoundingBox() {

    if (llbb == null) {

      if (isLatLon()) {
        double startLat = horizYaxis.getMinValue();
        double startLon = horizXaxis.getMinValue();

        double deltaLat = horizYaxis.getMaxValue() - startLat;
        double deltaLon = horizXaxis.getMaxValue() - startLon;

        LatLonPoint llpt = new LatLonPointImpl(startLat, startLon);
        llbb = new LatLonRect(llpt, deltaLat, deltaLon);

      } else {
        Projection dataProjection = getProjection();
        ProjectionRect bb = getBoundingBox();

        // look at all 4 corners of the bounding box
        LatLonPointImpl llpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getLowerLeftPoint(), new LatLonPointImpl());
        LatLonPointImpl urpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getUpperRightPoint(), new LatLonPointImpl());
        LatLonPointImpl ulpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getUpperLeftPoint(), new LatLonPointImpl());
        LatLonPointImpl lrpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getLowerRightPoint(), new LatLonPointImpl());

        double latMin = Math.min( llpt.getLatitude(), lrpt.getLatitude());
        double latMax = Math.max( ulpt.getLatitude(), urpt.getLatitude());

        // longitude is a bit tricky as usual
        double lonMin = getMinOrMaxLon( llpt.getLongitude(), ulpt.getLongitude(), true);
        double lonMax = getMinOrMaxLon( lrpt.getLongitude(), urpt.getLongitude(), false);

        llpt.set(latMin, lonMin);
        urpt.set(latMax, lonMax);

        llbb = new LatLonRect(llpt, urpt);
        // System.out.println("GridCoordSys: bb= "+bb+" llbb= "+llbb);
      }
    }

    return llbb;
  }

  private double getMinOrMaxLon(double lon1, double lon2, boolean wantMin ) {
    double midpoint = (lon1 + lon2)/2;
    lon1 = LatLonPointImpl.lonNormal(lon1, midpoint);
    lon2 = LatLonPointImpl.lonNormal(lon2, midpoint);

    return wantMin ? Math.min(lon1, lon2) : Math.max(lon1, lon2);
  }

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on latlon corners.
   * Must have CoordinateAxis1D or 2D for x and y axis.
   *
   * @return list of 2 Range objects, first y then x.
   */
  public List getLatLonBoundingBox(LatLonRect rect) {
    double minx, maxx, miny, maxy;

    LatLonPointImpl llpt = rect.getLowerLeftPoint();
    LatLonPointImpl urpt = rect.getUpperRightPoint();
    LatLonPointImpl lrpt = rect.getLowerRightPoint();
    LatLonPointImpl ulpt = rect.getUpperLeftPoint();

    if (isLatLon()) {
      minx = getMinOrMaxLon( llpt.getLongitude(), ulpt.getLongitude(), true);
      miny = Math.min( llpt.getLatitude(), lrpt.getLatitude());
      maxx = getMinOrMaxLon( urpt.getLongitude(), lrpt.getLongitude(), false);
      maxy = Math.min( ulpt.getLatitude(), urpt.getLatitude());

    } else {
      Projection dataProjection = getProjection();
      ProjectionPoint ll = dataProjection.latLonToProj(llpt, new ProjectionPointImpl());
      ProjectionPoint ur = dataProjection.latLonToProj(urpt, new ProjectionPointImpl());
      ProjectionPoint lr = dataProjection.latLonToProj(lrpt, new ProjectionPointImpl());
      ProjectionPoint ul = dataProjection.latLonToProj(ulpt, new ProjectionPointImpl());

      minx = Math.min( ll.getX(), ul.getX());
      miny = Math.min( ll.getY(), lr.getY());
      maxx = Math.max( ur.getX(), lr.getX());
      maxy = Math.max( ul.getY(), ur.getY());
    }

    CoordinateAxis xaxis = getXHorizAxis();
    CoordinateAxis yaxis = getYHorizAxis();

    if ((xaxis instanceof CoordinateAxis1D) && (yaxis instanceof CoordinateAxis1D)) {
      CoordinateAxis1D xaxis1 = (CoordinateAxis1D) xaxis;
      CoordinateAxis1D yaxis1 = (CoordinateAxis1D) yaxis;

      int minxIndex = xaxis1.findCoordElementBounded(minx);
      int minyIndex = yaxis1.findCoordElementBounded(miny);

      int maxxIndex = xaxis1.findCoordElementBounded(maxx);
      int maxyIndex = yaxis1.findCoordElementBounded(maxy);

      ArrayList list = new ArrayList();
      try {
        list.add(new Range(Math.min(minyIndex, maxyIndex), Math.max(minyIndex, maxyIndex)));
        list.add(new Range(Math.min(minxIndex, maxxIndex), Math.max(minxIndex, maxxIndex)));
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
      return list;

    } else if ((xaxis instanceof CoordinateAxis2D) && (yaxis instanceof CoordinateAxis2D) && isLatLon()) {
      CoordinateAxis2D lon_axis = (CoordinateAxis2D) xaxis;
      CoordinateAxis2D lat_axis = (CoordinateAxis2D) yaxis;
      int shape[] = lon_axis.getShape();
      int nj = shape[0];
      int ni = shape[1];

      int mini = Integer.MAX_VALUE, minj = Integer.MAX_VALUE;
      int maxi = -1, maxj = -1;

      // brute force, examine every point
      for (int j = 0; j < nj; j++) {
        for (int i = 0; i < ni; i++) {
          double lat = lat_axis.getCoordValue(j, i);
          double lon = lon_axis.getCoordValue(j, i);

          if ((lat >= miny) && (lat <= maxy) && (lon >= minx) && (lon <= maxx)) {
            if (i > maxi) maxi = i;
            if (i < mini) mini = i;
            if (j > maxj) maxj = j;
            if (j < minj) minj = j;
            //System.out.println(j+" "+i+" lat="+lat+" lon="+lon);
          }
        }
      }

      // this is the case where no point are included
      if ((mini > maxi) || (minj > maxj)) {
        mini = 0; minj = 0;
        maxi = -1; maxj = -1;
      }

      ArrayList list = new ArrayList();
      try {
        list.add(new Range(minj, maxj));
        list.add(new Range(mini, maxi));
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
      return list;

    } else {
      throw new IllegalArgumentException("must be 1D or 2D/LatLon ");
    }

  }

  /* private int getCrossing(CoordinateAxis2D axis) {
    for (int i=0; i<n; i++)
      if (axis.getCoordValue(i,j) > min) return i;
  } */


  /* private GeneralPath bbShape = null;
  public Shape getLatLonBoundingShape() {
    if (isLatLon())
      return getBoundingBox();

    if (bbShape == null) {
      ProjectionRect bb = getBoundingBox();
      Projection displayProjection = displayMap.getProjection();
      Projection dataProjection = getProjection();

      bbShape = new GeneralPath();

      LatLonPoint llpt = dataProjection.projToLatLon( bb.getX(), bb.getY());
      ProjectionPoint pt = displayProjection.latLonToProj( llpt);
      bbShape.lineTo(pt.getX(), pt.getY());

      llpt = dataProjection.projToLatLon( bb.getX(), bb.getY()+bb.getHeight());
      pt = displayProjection.latLonToProj( llpt);
      bbShape.lineTo(pt.getX(), pt.getY());

      llpt = dataProjection.projToLatLon( bb.getX()+bb.getWidth(), bb.getY()+bb.getHeight());
      pt = displayProjection.latLonToProj( llpt);
      bbShape.lineTo(pt.getX(), pt.getY());

      llpt = dataProjection.projToLatLon( bb.getX()+bb.getWidth(), bb.getY());
      pt = displayProjection.latLonToProj( llpt);
      bbShape.lineTo(pt.getX(), pt.getY());


      bbShape.closePath();
    }

    return bbShape;
  } */

  /**
   * String representation.
   */
  public String toString() {
    StringBuffer buff = new StringBuffer(200);
    buff.setLength(0);
    buff.append("(" + getName() + ") ");

    /* if (xdim >= 0) buff.append("x="+xaxis.getName()+",");
    if (ydim >= 0) buff.append("y="+yaxis.getName()+",");
    if (zdim >= 0) buff.append("z="+zaxis.getName()+",");
    if (tdim >= 0) buff.append("t="+taxis.getName()); */

    if (proj != null)
      buff.append("  Projection:" + proj.getName() + " " + proj.getClassName());
    return buff.toString();
  }

  /////////////////////////////////////////////////////////////////

  private void makeLevels() {
    levels = new ArrayList();
    if (vertZaxis == null)
      return;

    int n = (int) vertZaxis.getSize();
    for (int i = 0; i < n; i++)
      levels.add(new NamedAnything(vertZaxis.getCoordName(i), vertZaxis.getUnitsString()));
  }

  private void makeTimes() {
    times = new ArrayList();
    if ((timeTaxis == null) || (timeTaxis.getSize() == 0))
      return;
    int n = (int) timeTaxis.getSize();
    timeDates = new Date[n];

    // see if it has a valid udunits unit
    SimpleUnit su = null;
    String units = timeTaxis.getUnitsString();
    if (units != null)
      su = SimpleUnit.factory( units);
    if ((su != null) && (su instanceof DateUnit)) {
      DateFormatter formatter = new DateFormatter();
      DateUnit du = (DateUnit) su;
      for (int i = 0; i < n; i++) {
        Date d = du.makeDate(timeTaxis.getCoordValue(i));
        String name = formatter.toDateTimeString(d);
        if (name == null)  // LOOK bug in udunits ??
          name = Double.toString(timeTaxis.getCoordValue(i));
        times.add(new NamedAnything(name, "date/time"));
        timeDates[i] = d;
      }
      isDate = true;
      return;
    }

    // otherwise, see if its a String, and if we can parse the values as an ISO date
    if ((timeTaxis.getDataType() == DataType.STRING) || (timeTaxis.getDataType() == DataType.CHAR)) {
      isDate = true;
      DateFormatter formatter = new DateFormatter();
      for (int i=0; i<n; i++) {
        String coordValue = timeTaxis.getCoordName(i);
        Date d = formatter.getISODate(coordValue);
        if (d == null) {
          isDate = false;
          times.add(new NamedAnything(coordValue, timeTaxis.getUnitsString()));
        } else {
          times.add(new NamedAnything(formatter.toDateTimeString(d), "date/time"));
          timeDates[i] = d;
        }
      }
      return;
    }

    // otherwise
    for (int i = 0; i < n; i++) {
      times.add(new NamedAnything(timeTaxis.getCoordName(i), timeTaxis.getUnitsString()));
    }

  }

  private static class NamedAnything implements NamedObject {
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