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

import ucar.ma2.*;
import ucar.nc2.Dimension;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.util.NamedObject;

import java.util.List;

/**
 * Experimental Coverage feature type.
 * Superclass of gridded data types
 * Can we do this without indexed reads?
 * @author caron
 * @since Jan 19, 2010
 */
public interface Coverage extends IsMissingEvaluator, VariableSimpleIF, NamedObject {

    /**
     * Convenience function; lookup Attribute value by name. Must be String valued
     *
     * @param attName      name of the attribute
     * @param defaultValue if not found, use this as the default
     * @return Attribute string value, or default if not found.
     */
    public String findAttValueIgnoreCase(String attName, String defaultValue);

    /**
     * Returns a List of Dimension containing the dimensions used by this Coverage.
     * The dimension are put into canonical order: (rt, e, t, z, y, x).
     * Only the x and y are required.
     * If the Horizontal axes are 2D, the x and y dimensions are arbitrarily chosen to be
     * gcs.getXHorizAxis().getDimension(1), gcs.getXHorizAxis().getDimension(0), respectively.
     *
     * @return List with objects of type Dimension, in canonical order.
     */
    public List<Dimension> getDimensions();

    /**
     * get the Coverage's Coordinate System.
     * @return the Coverage's Coordinate System.
     */
    public CoverageCS getCoordinateSystem();

    /**
     * true if there may be missing data
     * @return true if there may be missing data
     */
    public boolean hasMissing();

    /**
     * if val is missing data
     * @param val test this value
     * @return true if val is missing data
     */
    public boolean isMissing(double val);

    /*
     * Get the minimum and the maximum data value of the previously read Array,
     * skipping missing values as defined by isMissingData(double val).
     *
     * @param data Array to get min/max values
     * @return both min and max value.
     *
    public MAMath.MinMax getMinMaxSkipMissingData(Array data); */

    /**
     * This reads an arbitrary data slice, returning the data in
     * canonical order (rt-e-t-z-y-x). If any dimension does not exist, ignore it.
     *
     * @param subset subset that you want. Must be created through this.getCoordinateSystem().makeSubset()
     * @return data[rt,e,t,z,y,x], eliminating missing or fixed dimension.
     * @throws java.io.IOException on io error
     */
    public Array readData(CoverageCS.Subset subset) throws java.io.IOException, InvalidRangeException;

    /*
     * Create a new GeoGrid that is a logical subset of this GeoGrid.
     *
     * @param subset subset that you want
     * @return subsetted GeoGrid
     * @throws ucar.ma2.InvalidRangeException if ranges are invalid
     *
    public Coverage makeSubset(Subset subset) throws ucar.ma2.InvalidRangeException;  */

    /**
     * human readable information about this Coverage.
     * @return human readable information about this Coverage.
     */
    public String getInfo();

  }

