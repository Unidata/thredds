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

package ucar.nc2.ft.grid;

import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.util.List;

/**
 * Experimental Grid fetaure type
 * TODO
 *  has extra dimension(s)
 *
 * @author caron
 * @since Jan 19, 2010
 */
public interface Grid {

    /**
     * Get the name of the Grid
     *
     * @return the name of the Grid
     */
    public String getName();

    /**
     * Get the description/long_name of the Grid
     * @return the description/long_name of the Grid
     */
    public String getDescription();

    /**
     * Get the unit string
     * @return the unit string
     */
    public String getUnitsString();

    /**
     * get the data type
     * @return the data type
     */
    public DataType getDataType();

    /**
     * get the rank
     * @return the rank
     */
    public int getRank();

    /**
     * get the shape (canonical ordering)
     * @return the shape (canonical ordering)
     */
    public int[] getShape();

    /**
     * Get a List of Attribute specific to the Grid
     * @return a List of Attribute
     */
    public List<Attribute> getAttributes();

    /**
     * Convenience function; lookup Attribute by name.
     *
     * @param name the name of the attribute
     * @return the attribute, or null if not found
     */
    public Attribute findAttributeIgnoreCase(String name);

    /**
     * Convenience function; lookup Attribute value by name. Must be String valued
     *
     * @param attName      name of the attribute
     * @param defaultValue if not found, use this as the default
     * @return Attribute string value, or default if not found.
     */
    public String findAttValueIgnoreCase(String attName, String defaultValue);

    /**
     * Returns a List of Dimension containing the dimensions used by this grid.
     * The dimension are put into canonical order: (rt, e, t, z, y, x).
     * Only the x and y are required.
     * If the Horizontal axes are 2D, the x and y dimensions are arbitrarily chosen to be
     * gcs.getXHorizAxis().getDimension(1), gcs.getXHorizAxis().getDimension(0), respectively.
     *
     * @return List with objects of type Dimension, in canonical order.
     */
    public List<Dimension> getDimensions();

    /**
     * get the ith dimension
     * @param i index of dimension
     * @return the ith dimension
     */
    public Dimension getDimension(int i);

    /**
     * get the Grid's Coordinate System.
     * @return the Grid's Coordinate System.
     */
    public GridCoordSystem getCoordinateSystem();

    /**
     * get the Projection, if it exists.
     * @return the Projection, or null
     */
    public ProjectionImpl getProjection();

    /**
     * true if there may be missing data
     * @return true if there may be missing data
     */
    public boolean hasMissingData();

    /**
     * if val is missing data
     * @param val test this value
     * @return true if val is missing data
     */
    public boolean isMissingData(double val);

    /**
     * Get the minimum and the maximum data value of the previously read Array,
     * skipping missing values as defined by isMissingData(double val).
     *
     * @param data Array to get min/max values
     * @return both min and max value.
     */
    public MAMath.MinMax getMinMaxSkipMissingData(Array data);

    /**
     * This reads an arbitrary data slice, returning the data in
     * canonical order (rt-e-t-z-y-x). If any dimension does not exist, ignore it.
     *
     * @param rt_index if < 0, get all of runtime dim; if valid index, fix slice to that value.
     * @param e_index  if < 0, get all of ensemble dim; if valid index, fix slice to that value.
     * @param t_index  if < 0, get all of time dim; if valid index, fix slice to that value.
     * @param z_index  if < 0, get all of z dim; if valid index, fix slice to that value.
     * @param y_index  if < 0, get all of y dim; if valid index, fix slice to that value.
     * @param x_index  if < 0, get all of x dim; if valid index, fix slice to that value.
     * @return data[rt,e,t,z,y,x], eliminating missing or fixed dimension.
     * @throws java.io.IOException on io error
     */
    public Array readDataSlice(int rt_index, int e_index, int t_index, int z_index, int y_index, int x_index) throws java.io.IOException;

    /**
     * Create a new GeoGrid that is a logical subset of this GeoGrid.
     *
     * @param rt_range subset the runtime dimension, or null if you want all of it
     * @param e_range  subset the ensemble dimension, or null if you want all of it
     * @param t_range  subset the time dimension, or null if you want all of it
     * @param z_range  subset the vertical dimension, or null if you want all of it
     * @param y_range  subset the y dimension, or null if you want all of it
     * @param x_range  subset the x dimension, or null if you want all of it
     * @return subsetted GeoGrid
     * @throws ucar.ma2.InvalidRangeException if ranges are invlaid
     */
    public Grid makeSubset(Range rt_range, Range e_range, Range t_range, Range z_range, Range y_range, Range x_range) throws ucar.ma2.InvalidRangeException;

    /**
     * human readable information about this Grid.
     * @return human readable information about this Grid.
     */
    public String getInfo();

    /**
     * Get the underlying Variable, if it exists.
     * @return the underlying Variable, if it exists, else null
     */
    public VariableDS getVariable();

  }

