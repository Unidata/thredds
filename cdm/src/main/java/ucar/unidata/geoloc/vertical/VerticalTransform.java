/*
 * $Id: VerticalTransform.java 64 2006-07-12 22:30:50Z edavis $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.geoloc.vertical;

import ucar.ma2.Range;


/**
 * A transformation to a vertical reference coordinate system,
 * such as height or pressure.
 *
 * @author  Unidata Development Team
 * @version $Revision: 64 $
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
    public ucar.ma2.ArrayDouble.D3 getCoordinateArray(int timeIndex) throws java.io.IOException, ucar.ma2.InvalidRangeException;

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
     * Create a VerticalTransform as a section of an existing VerticalTransform.
     *
     * @param t_range subset the time dimension, or null if you want all of it
     * @param z_range subset the vertical dimension, or null if you want all of it
     * @param y_range subset the y dimension, or null if you want all of it
     * @param x_range subset the x dimension, or null if you want all of it
     * @return a new VerticalTransform for the given subset
     * @throws ucar.ma2.InvalidRangeException if any Range is incompatible with the existing VerticalTransform
     */
    public VerticalTransform subset(Range t_range, Range z_range, Range y_range, Range x_range) throws ucar.ma2.InvalidRangeException;
}
