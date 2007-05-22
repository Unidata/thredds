// $Id: $
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

import ucar.ma2.*;
import ucar.nc2.Dimension;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.util.List;

/**
 * Interface for scientific datatype Grid.
 * @author caron
 */
public interface GridDatatype {

  /** Get the name of the Grid */
  public String getName();

  /** Get the description/long_name of the Grid */
  public String getDescription();

  /** Get the unit string */
  public String getUnitsString();

  /** get the data type */
  public DataType getDataType();

  /** get the rank */
  public int getRank();

  /** get the shape (canonical ordering) */
  public int[] getShape();

  /** Get a List of Attribute specific to the Grid */
  public List getAttributes();

  /**
   * Convenience function; lookup Attribute by name.
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findAttributeIgnoreCase(String name);

  /**
   * Convenience function; lookup Attribute value by name. Must be String valued
   * @param attName name of the attribute
   * @param defaultValue if not found, use this as the default
   * @return Attribute string value, or default if not found.
   */
  public String findAttValueIgnoreCase( String attName, String defaultValue);

  /**
   * Returns a List of Dimension containing the dimensions used by this grid.
   * The dimension are put into canonical order: (rt, e, t, z, y, x).
   * Only the x and y are required.
   * If the Horizontal axes are 2D, the x and y dimensions are arbitrarily chosen to be
   *   gcs.getXHorizAxis().getDimension(1), gcs.getXHorizAxis().getDimension(0), respectively.
   * @return List with objects of type Dimension, in canonical order.
   */
  public List getDimensions();

  /** get the ith dimension */
  public Dimension getDimension(int i);

    /** get the time Dimension, if it exists */
  public Dimension getTimeDimension();
  /** get the z Dimension, if it exists */
  public Dimension getZDimension();
  /** get the y Dimension, if it exists */
  public Dimension getYDimension();
  /** get the x Dimension, if it exists */
  public Dimension getXDimension();
  /** get the ensemble Dimension, if it exists */
  public Dimension getEnsembleDimension();
  /** get the run time Dimension, if it exists */
  public Dimension getRunTimeDimension();

  /** get the time Dimension index in the geogrid (canonical order), or -1 if none */
  public int getTimeDimensionIndex();
  /** get the z Dimension index in the geogrid (canonical order), or -1 if none */
  public int getZDimensionIndex();
  /** get the y Dimension index in the geogrid (canonical order) */
  public int getYDimensionIndex();
  /** get the x Dimension index in the geogrid (canonical order) */
  public int getXDimensionIndex();
  /** get the ensemble Dimension index in the geogrid (canonical order), or -1 if none */
  public int getEnsembleDimensionIndex();
  /** get the runtime Dimension index in the geogrid (canonical order), or -1 if none */
  public int getRunTimeDimensionIndex();

  /** get the Grid's Coordinate System. */
  public GridCoordSystem getCoordinateSystem();

  /** get the Projection, if it exists. */
  public ProjectionImpl getProjection();

  /** true if there may be missing data, see VariableStandardized.hasMissing() */
  public boolean hasMissingData();

  /** if val is missing data, see VariableStandardized.isMissingData() */
  public boolean isMissingData(double vl);

 /**
   * Get the minimum and the maximum data value of the previously read Array,
   *  skipping missing values as defined by isMissingData(double val).
   * @param data Array to get min/max values
   * @return both min and max value.
   */
  public MAMath.MinMax getMinMaxSkipMissingData(Array data);
  
  /**
   * Convert (in place) all values in the given array that are considered
   * as "missing" to Float.NaN, according to isMissing(val).
   *
   * @param data input array
   * @return input array, with missing values converted to NaNs.
   */
  public float[] setMissingToNaN(float[] data);

  /**
   * This reads an arbitrary data slice, returning the data in
   * canonical order (rt-e-t-z-y-x). If any dimension does not exist, ignore it.
   *
   * @param rt_index if < 0, get all of runtime dim; if valid index, fix slice to that value.
   * @param e_index if < 0, get all of ensemble dim; if valid index, fix slice to that value.
   * @param t_index if < 0, get all of time dim; if valid index, fix slice to that value.
   * @param z_index if < 0, get all of z dim; if valid index, fix slice to that value.
   * @param y_index if < 0, get all of y dim; if valid index, fix slice to that value.
   * @param x_index if < 0, get all of x dim; if valid index, fix slice to that value.
   *
   * @return data[rt,e,t,z,y,x], eliminating missing or fixed dimension.
   */
  public Array readDataSlice(int rt_index, int e_index, int t_index, int z_index, int y_index, int x_index) throws java.io.IOException;

  /** For backwards compatibility for grids with no runtime or ensemble dimensions. */
  public Array readDataSlice(int t_index, int z_index, int y_index, int x_index) throws java.io.IOException;

  /** For backwards compatibility for grids with no runtime or ensemble dimensions. */
  public Array readVolumeData(int t_index) throws java.io.IOException;

  /**
   * Create a new GeoGrid that is a logical subset of this GeoGrid.
   *
   * @param rt_range subset the runtime dimension, or null if you want all of it
   * @param e_range subset the ensemble dimension, or null if you want all of it
   * @param t_range subset the time dimension, or null if you want all of it
   * @param z_range subset the vertical dimension, or null if you want all of it
   * @param y_range subset the y dimension, or null if you want all of it
   * @param x_range subset the x dimension, or null if you want all of it
   *
   * @return subsetted GeoGrid
   */
  public GridDatatype makeSubset(Range rt_range, Range e_range, Range t_range, Range z_range, Range y_range, Range x_range) throws ucar.ma2.InvalidRangeException;

  /**
   * Create a new GeoGrid that is a logical subset of this GeoGrid.
   * For backwards compatibility for grids with no runtime or ensemble dimensions.
   *
   * @param t_range subset the time dimension, or null if you want all of it
   * @param z_range subset the vertical dimension, or null if you want all of it
   * @param bbox a lat/lon bounding box, or null if you want all x,y
   * @param z_stride use only if z_range is null, then take all z with this stride (1 means all)
   * @param y_stride use this stride on the y coordinate (1 means all)
   * @param x_stride use this stride on the x coordinate (1 means all)
   *
   * @return subsetted GeoGrid
   * @throws ucar.ma2.InvalidRangeException
   */
  public GridDatatype makeSubset(Range t_range, Range z_range, LatLonRect bbox, int z_stride, int y_stride, int x_stride) throws InvalidRangeException;

  /** human readable information about this Grid. */
  public String getInfo();

  /** get the underlying Variable, if it exists.*/
  public VariableEnhanced getVariable();
}
