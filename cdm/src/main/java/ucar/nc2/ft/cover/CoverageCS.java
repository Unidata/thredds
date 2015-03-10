/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.cover;

import ucar.nc2.Dimension;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.dataset.*;
import ucar.ma2.InvalidRangeException;

import java.util.Formatter;
import java.util.List;

/**
 * A Coordinate System for coverage data. Where:
 * <ul>
 * <li> CS:D -> Rn</li>
 * <li> D is the domain
 * <li> R is a coordinate axis. n is getCoordinateAxes().size()
 * </ul>
 * <p/>
 * <p/>
 *
 * @author caron
 * @since Jan 19, 2010
 */

/* Notes
 * We could insist on one dimensional X, Y, Z, T, and treat optional HorizontalTransform the same as VerticalTransform.
 * Its job would be to provide lat(x,y) and lon(x,y) arrays.
 *
 * At the moment this is just a rearrangement  of dt.grid.GridCoordSystem
 *
 * TODO
 *  horiz time dependence
 *  curvilinear  - 2D lat,lon with no projection. need lat,lon <-> i,j
 *
 *  subsetting only in coordinate space
 *  allow different indexes
 *  each coordinate must correspond to a dimension (?)
 *
 *  From Martin Schultz:
 *  http://redmine.iek.fz-juelich.de/projects/julich_wcs_interface/wiki/MetOcean_data_types
 *
 *  "rectified"  : all coordinates are 1D
 *      (a "grid for which there is an affine transformation between the grid coordinates and the coordinates of an external coordinate reference system")
 *      (seems to imply CRS coordinates are 1D)
 *
 *
 */

public interface CoverageCS {

  public enum Type {Coverage, Curvilinear, Grid, Swath, Fmrc}

  /**
   * The name of the Grid Coordinate System.
   *
   * @return name of the Grid Coordinate System
   */
  public String getName();

  /**
   * Get the list of dimensions used by any of the Axes in the Coordinate System.
   *
   * @return List of Dimension
   */
  public List<Dimension> getDomain();

  /**
   * Get the list of all axes.
   *
   * @return List of CoordinateAxis.
   */
  public List<CoordinateAxis> getCoordinateAxes();

  /**
   * Get the list of axes that are not x,y,z,t.
   *
   * @return List of CoordinateAxis.
   */
  public List<CoordinateAxis> getOtherCoordinateAxes();

  /**
   * True if all axes are 1 dimensional.
   *
   * @return true if all axes are 1 dimensional.
   */
  public boolean isProductSet();

  /**
   * Get the list of all CoordinateTransforms.
   *
   * @return List of CoordinateTransform.
   */
  public List<CoordinateTransform> getCoordinateTransforms();


  /////////////////////////////////////////////
  // horizontal axes

   /**
   * Get the X axis. May be 1 or 2 dimensional.
   *
   * @return X CoordinateAxis, may not be null.
   */
  public CoordinateAxis getXHorizAxis();

  /**
   * Get the Y axis. May be 1 or 2 dimensional.
   *
   * @return Y CoordinateAxis, may not be null.
   */
  public CoordinateAxis getYHorizAxis();

  /**
   * Does this use lat/lon horizontal axes?
   * If not, then the horizontal axes are GeoX, GeoY, and there must be a Projection defined.
   *
   * @return true if lat/lon horizontal axes
   */
  public boolean isLatLon();

  /**
   * Get horizontal bounding box in lat, lon coordinates.
   * For projection, only an approximation based on corners.
   *
   * @return LatLonRect bounding box.
   */
  public ucar.unidata.geoloc.LatLonRect getLatLonBoundingBox();

  /**
   * Get horizontal bounding box in projection coordinates.
   * For lat/lon, the ProjectionRect has units of degrees north and east.
   *
   * @return ProjectionRect bounding box.
   */
  public ucar.unidata.geoloc.ProjectionRect getBoundingBox();

  /*
   * Get the Projection CoordinateTransform. It must exist if !isLatLon().
   *
   * @return ProjectionCT or null.
   *
  public ProjectionCT getProjectionCT();   */

  /**
   * Get the Projection that performs the transform math.
   * Same as getProjectionCT().getProjection().
   *
   * @return ProjectionImpl or null.
   */
  public ucar.unidata.geoloc.ProjectionImpl getProjection();


  /////////////////
  // vertical axis

  /**
   * Get the Z axis.
   *
   * @return Y CoordinateAxis, may be null.
   */
  public CoordinateAxis getVerticalAxis();

  /**
   * True if increasing z coordinate values means "up" in altitude
   *
   * @return true if increasing z coordinate values means "up" in altitude
   */
  public boolean isZPositive();

  /**
   * Get the Vertical CoordinateTransform, it it exists.
   *
   * @return VerticalCT or null.
   */
  public VerticalCT getVerticalCT();

  /**
   * Get the VerticalTransform that performs the transform math.
   * Same as getVerticalCT().getVerticalTransform().
   *
   * @return VerticalTransform or null.
   */
  public ucar.unidata.geoloc.vertical.VerticalTransform getVerticalTransform();


  /////////////////
  // time axis

  /**
   * True if there is a Time Axis.
   *
   * @return true if there is a Time Axis.
   */
  public boolean hasTimeAxis();

  /**
   * Get the Time axis, if it exists. May be 1 or 2 dimensional.
   * If 1D, will be a CoordinateAxis1DTime. If 2D, then you can use getTimeAxisForRun().
   * A time coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   * Typical meaning is the date of measurement or valid forecast time.
   *
   * @return the time coordinate axis, may be null.
   */
  public CoordinateAxis getTimeAxis();

  /**
   * If there is a time coordinate, get the time covered.
   *
   * @return DateRange or null if no time coordinate
   */
  public CalendarDateRange getCalendarDateRange();

  /**
   * Show information about this
   * @param f put info here
   * @param showCoordValues  optionally show the coordinate values
   */
  public void show(Formatter f, boolean showCoordValues);

  ///////////////////////////////////////////////////////////////////////////

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on corners.
   * Must have CoordinateAxis1D or 2D for x and y axis.
   *
   * @param llbb a lat/lon bounding box.
   * @return list of 2 Range objects, first y then x.
   * @throws ucar.ma2.InvalidRangeException if llbb generates bad ranges
   */
  public Subset makeSubsetFromLatLonRect(ucar.unidata.geoloc.LatLonRect llbb) throws InvalidRangeException;
  public Subset getSubset();

  public interface Subset {
    void setLevel(int idx);
    void setTime(int idx);
  }

}
