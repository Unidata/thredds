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
package ucar.unidata.geoloc.vertical;

import ucar.ma2.Range;

/**
 * A transformation to a vertical reference coordinate system,
 * such as height or pressure.
 *
 * @author  Unidata Development Team
 */

public interface VerticalTransform {

    /**
     * Get the 3D vertical coordinate array for this time step.
     * Must be in "canonical order" : z, y, x.
     *
     * @param timeIndex the time index. Ignored if !isTimeDependent().
     *
     * @return  vertical coordinate array
     *
     * @throws java.io.IOException problem reading the data
     * @throws ucar.ma2.InvalidRangeException timeIndex out of bounds
     */
    public ucar.ma2.ArrayDouble.D3 getCoordinateArray(int timeIndex)
     throws java.io.IOException, ucar.ma2.InvalidRangeException;

    /**
     * Get the unit string for the vertical coordinate.
     * @return unit string
     */
    public String getUnitString();

    /**
     * Get whether this coordinate is time dependent.
     * @return true if time dependent
     */
    public boolean isTimeDependent();

    /**
     *  Create a VerticalTransform as a section of an existing VerticalTransform.
     * 
     *  @param t_range subset the time dimension, or null if you want all of it
     *  @param z_range subset the vertical dimension, or null if you want all of it
     *  @param y_range subset the y dimension, or null if you want all of it
     *  @param x_range subset the x dimension, or null if you want all of it
     *  @return a new VerticalTransform for the given subset
     *  @throws ucar.ma2.InvalidRangeException if any Range is incompatible with the existing VerticalTransform
     */
    public VerticalTransform subset(Range t_range, Range z_range,
                                    Range y_range, Range x_range)
     throws ucar.ma2.InvalidRangeException;
}

