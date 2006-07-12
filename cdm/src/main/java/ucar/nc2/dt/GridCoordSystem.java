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
package ucar.nc2.dt;

import ucar.nc2.dataset.*;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.units.DateUnit;

import java.util.Date;
import java.util.List;

import thredds.datatype.DateRange;

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
 * We could insist on one dimensional X, Y, Z, T, and treat optional HorizontalTransform the same as VerticalTransform.
 * Its job would be to provide lat(x,y) and lon(x,y) arrays.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public interface GridCoordSystem {

  /**
   * The name of the Grid Coordinate System.
   */
  public String getName();

  /**
   * Get the list of dimensions used by any of the Axes in the Coordinate System.
   * @return List of Dimension
   */
  public List getDomain();

  // axes

  /**
   * Get the list of all axes.
   * @return List of CoordinateAxis.
   */
  public List getCoordinateAxes();

  /**
   * True if all axes are 1 dimensional.
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
   * Get the Time axis, if has1DTimeAxis() is true. Otherwise use getTimeAxisForRun.
   * A time coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   * Typical meaning is the date of measurement or valid forecast time.
   * @return the time coordinate axis, may be null.
   */
  public CoordinateAxis1DTime getTimeAxis();

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
  public List getCoordinateTransforms();

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
   * Does this uese lat/lon horizontal axes?
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
   */
  public boolean isRegularSpatial();

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on corners.
   * Must have CoordinateAxis1D or 2D for x and y axis.
   *
   * @param llbb a lat/lon bounding box.
   * @return list of 2 Range objects, first y then x.
   */
  public java.util.List getRangesFromLatLonRect(ucar.unidata.geoloc.LatLonRect llbb);

  /**
   * Given a point in x,y coordinate space, find the x,y indices.
   * Not implemented yet for 2D.
   *
   * @param x_coord position in x coordinate space, ie, units of getXHorizAxis().
   * @param y_coord position in y coordinate space, ie, units of getYHorizAxis().
   * @param result  optionally pass in the result array to use.
   * @return int[2], 0=x, 1=y indices of the point. These will be -1 if out of range.
   */
  public int[] findXYindexFromCoord(double x_coord, double y_coord, int[] result);

  // vertical

  /**
   * True if increasing z coordinate values means "up" in altitude
   */
  public boolean isZPositive();

  // time

  /**
   * If there is a time coordinate, get the
   * @return DateRange or null if no time coordinate
   */
  public thredds.datatype.DateRange getDateRange();

  /**
   * True if getTimeAxis() != null, and getTimeAxis() instanceof CoordinateAxis1D.
   */
  public boolean has1DTimeAxis();

  /**
   * This is the case of a 2D time axis, which depends on the run index.
   * A time coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   * @param run_index which run?
   * @return 1D time axis for that run.
   */
  public CoordinateAxis1DTime getTimeAxisForRun(int run_index);

}