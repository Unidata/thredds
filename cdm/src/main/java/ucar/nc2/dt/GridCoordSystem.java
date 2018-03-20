/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt;

import ucar.nc2.dataset.*;
import ucar.nc2.Dimension;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.unidata.geoloc.LatLonPoint;

import java.util.Collection;
import java.util.Formatter;
import java.util.List;

/**
 * A Coordinate System for gridded data. Assume:
 * <ul>
 * <li> X and Y are 1 or 2 dimensional
 * <li> T is 1 or 2 dimensional. The 2D case is that it depends on runtime.
 * <li> We can create Dates out of the T and RT coordinate values.
 * <li> Z, E, RT are 1-dimensional
 * <li> An optional VerticalTransform can provide a height or pressure coordinate that may be 1-4 dimensional.
 * </ul>
 * <p/>
 *
 * @author caron
 */

public interface GridCoordSystem {

/* Notes
 * We could insist on one dimensional X, Y, Z, T, and treat optional HorizontalTransform the same as VerticalTransform.
 * Its job would be to provide lat(x,y) and lon(x,y) arrays.
 */

  /**
   * The name of the Grid Coordinate System, consisting of the list of coordinate axes, seperated by blanks.
   * @return  name of the Grid Coordinate System
   */
  public String getName();

  /**
   * Get the list of dimensions used by any of the Axes in the Coordinate System.
   * @return List of Dimension
   */
  public Collection<Dimension> getDomain();

  // axes

  /**
   * Get the list of all axes.
   * @return List of CoordinateAxis.
   */
  public List<CoordinateAxis> getCoordinateAxes();

  /**
   * True if all axes are 1 dimensional.
   * @return true if all axes are 1 dimensional.
   */
  public boolean isProductSet();

  /**
   * Get the X axis. May be 1 or 2 dimensional.
   * @return X CoordinateAxis, may not be null.
   */
  public CoordinateAxis getXHorizAxis();

  /**
   * Get the Y axis. May be 1 or 2 dimensional.
   * @return Y CoordinateAxis, may not be null.
   */
  public CoordinateAxis getYHorizAxis();

  /**
   * Get the Z axis. Must be 1 dimensional.
   * @return Y CoordinateAxis, may be null.
   */
  public CoordinateAxis1D getVerticalAxis();

  /**
   * Get the Time axis, if it exists. May be 1 or 2 dimensional.
   * If 1D, will be a CoordinateAxis1DTime. If 2D, then you can use getTimeAxisForRun().
   * A time coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   * Typical meaning is the date of measurement or valid forecast time.
   * @return the time coordinate axis, may be null.
   */
  public CoordinateAxis getTimeAxis();

  /**
   * Get the ensemble axis. Must be 1 dimensional.
   * Typical meaning is an enumeration of ensemble Model runs.
   * @return ensemble CoordinateAxis, may be null.
   */
  public CoordinateAxis1D getEnsembleAxis();

  /**
   * Get the RunTime axis. Must be 1 dimensional.
   * A runtime coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   * Typical meaning is the date that a Forecast Model Run is made.
   * @return RunTime CoordinateAxis, may be null.
   */
  public CoordinateAxis1DTime getRunTimeAxis();

  // transforms

  /**
   * Get the list of all CoordinateTransforms.
   * @return List of CoordinateTransform.
   */
  public List<CoordinateTransform> getCoordinateTransforms();

  /**
   * Get the Projection CoordinateTransform. It must exist if !isLatLon().
   * @return ProjectionCT or null.
   */
  public ProjectionCT getProjectionCT();

  /**
   * Get the Projection that performs the transform math.
   * Same as getProjectionCT().getProjection().
   * @return ProjectionImpl or null.
   */
  public ucar.unidata.geoloc.ProjectionImpl getProjection();

  /**
   * Use the bounding box to set the defaule map are of the projection.
   * This can be expensive if its a 2D coordinate system.
   */
  public void setProjectionBoundingBox();

  /**
   * Get the Vertical CoordinateTransform, it it exists.
   * @return VerticalCT or null.
   */
  public VerticalCT getVerticalCT();

  /**
   * Get the VerticalTransform that performs the transform math.
   * Same as getVerticalCT().getVerticalTransform().
   * @return VerticalTransform or null.
   */
  public ucar.unidata.geoloc.vertical.VerticalTransform getVerticalTransform();

  // horiz

  /**
   * Does this use lat/lon horizontal axes?
   * If not, then the horizontal axes are GeoX, GeoY, and there must be a Projection defined.
   * @return true if lat/lon horizontal axes
   */
  public boolean isLatLon();

  /**
   * Is this a global coverage over longitude ?
   * @return true if isLatLon and longitude extent >= 360 degrees
   */
  public boolean isGlobalLon();

  /**
   * Get horizontal bounding box in lat, lon coordinates.
   * For projection, only an approximation based on corners.
   * @return LatLonRect bounding box.
   */
  public ucar.unidata.geoloc.LatLonRect getLatLonBoundingBox();

   /**
   * Get horizontal bounding box in projection coordinates.
   * For lat/lon, the ProjectionRect has units of degrees north and east.
   * @return ProjectionRect bounding box.
   */
  public ucar.unidata.geoloc.ProjectionRect getBoundingBox();

  /**
   * True if both X and Y axes are 1 dimensional and are regularly spaced.
   * @return true if both X and Y axes are 1 dimensional and are regularly spaced.
   */
  public boolean isRegularSpatial();

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on corners.
   * Must have CoordinateAxis1D or 2D for x and y axis.
   *
   * @param llbb a lat/lon bounding box.
   * @return list of 2 Range objects, first y then x.
   * @throws ucar.ma2.InvalidRangeException if llbb generates bad ranges
   */
  public java.util.List<Range> getRangesFromLatLonRect(ucar.unidata.geoloc.LatLonRect llbb) throws InvalidRangeException;

  /**
   * Given a point in x,y coordinate space, find the x,y indices.
   *
   * @param x_coord position in x coordinate space, ie, units of getXHorizAxis().
   * @param y_coord position in y coordinate space, ie, units of getYHorizAxis().
   * @param result  optionally pass in the result array to use.
   * @return int[2], 0=x, 1=y indices of the point. These will be -1 if out of range.
   */
  public int[] findXYindexFromCoord(double x_coord, double y_coord, int[] result);

  /**
   * Given a point in x,y coordinate space, find the x,y indices.
   * If outside the range, the closest point is returned
   *
   * @param x_coord position in x coordinate space, ie, units of getXHorizAxis().
   * @param y_coord position in y coordinate space, ie, units of getYHorizAxis().
   * @param result  optionally pass in the result array to use.
   * @return int[2], 0=x, 1=y indices of the point.
   */
  public int[] findXYindexFromCoordBounded(double x_coord, double y_coord, int[] result);

  /**
   * Given a lat,lon point, find the x,y index of the containing grid point.
   *
   * @param lat latitude position.
   * @param lon longitude position.
   * @param result  put result in here, may be null
   * @return int[2], 0=x,1=y indices in the coordinate system of the point. These will be -1 if out of range.
   */
  public int[] findXYindexFromLatLon(double lat, double lon, int[] result) ;

  /**
   * Given a lat,lon point, find the x,y index of the containing grid point.
   * If outside the range, the closest point is returned
   *
   * @param lat latitude position.
   * @param lon longitude position.
   * @param result return result here, may be null
   * @return int[2], 0=x,1=y indices in the coordinate system of the point.
   */
  public int[] findXYindexFromLatLonBounded(double lat, double lon, int[] result) ;

  /**
   * Get the Lat/Lon coordinates of the midpoint of a grid cell, using the x,y indices.
   *
   * @param xindex  x index
   * @param yindex  y index
   * @return lat/lon coordinate of the midpoint of the cell
   */
  public LatLonPoint getLatLon(int xindex, int yindex);


  // vertical

  /**
   * True if increasing z coordinate values means "up" in altitude
   * @return true if increasing z coordinate values means "up" in altitude
   */
  public boolean isZPositive();

  // time

  /**
   * If there is a time coordinate, get the time covered.
   * @return DateRange or null if no time coordinate
   * @deprecated use getCalendarDateRange()
   */
  public DateRange getDateRange();
   
  /**
   * True if there is a Time Axis.
   * @return true if there is a Time Axis.
   */
  public boolean hasTimeAxis();

  /**
   * True if there is a Time Axis and it is 1D.
   * @return true if there is a Time Axis and it is 1D.
   */
  public boolean hasTimeAxis1D();

  /**
   * Get the Time axis, if it exists, and if its 1-dimensional.
   * @return the time coordinate axis, may be null.
   */
  public CoordinateAxis1DTime getTimeAxis1D();

  /**
   * This is the case of a 2D time axis, which depends on the run index.
   * A time coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   * @param run_index which run?
   * @return 1D time axis for that run. Null if not 2D time
   */
  public CoordinateAxis1DTime getTimeAxisForRun(int run_index);

  public List<CalendarDate> getCalendarDates();

  public CalendarDateRange getCalendarDateRange();

  public String getHorizStaggerType();

  public void show(Formatter buff, boolean showCoords);
}