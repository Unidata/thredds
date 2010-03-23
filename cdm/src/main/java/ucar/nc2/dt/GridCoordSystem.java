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
package ucar.nc2.dt;

import ucar.nc2.dataset.*;
import ucar.nc2.Dimension;
import ucar.nc2.units.DateRange;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.unidata.geoloc.LatLonPoint;

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
  public List<Dimension> getDomain();

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
   * Get the Time axis, if it exists, and its 1-dimensional.
   * @return the time coordinate axis, may be null.
   */
  public CoordinateAxis1DTime getTimeAxis1D();

  /**
   * This is the case of a 2D time axis, which depends on the run index.
   * A time coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   * @param run_index which run?
   * @return 1D time axis for that run.
   */
  public CoordinateAxis1DTime getTimeAxisForRun(int run_index);

}