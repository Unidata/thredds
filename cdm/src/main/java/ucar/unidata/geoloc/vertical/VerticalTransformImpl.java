/*
 * $Id: VerticalTransformImpl.java 64 2006-07-12 22:30:50Z edavis $
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


import ucar.nc2.*;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

import java.io.IOException;


/**
 * A transformation to a vertical reference coordinate system,
 * such as height or pressure.
 *
 * @author  Unidata Development Team
 * @version $Revision: 64 $
 */

public abstract class VerticalTransformImpl implements VerticalTransform {

    /** unit string */
    protected String units;

    /** time dimension */
    private Dimension timeDim;

    /**
     * Construct a VerticalCoordinate
     *
     * @param timeDim  time dimension
     */
    public VerticalTransformImpl(Dimension timeDim) {
        this.timeDim = timeDim;
    }

    /**
     * Get the 3D vertical coordinate array for this time step.
     *
     * @param timeIndex the time index. Ignored if !isTimeDependent().
     *
     * @return  vertical coordinate array
     *
     * @throws java.io.IOException problem reading the data
     */
    public abstract ucar.ma2.ArrayDouble.D3 getCoordinateArray(int timeIndex)
            throws java.io.IOException, InvalidRangeException;

    /**
     * Get the unit string for the vertical coordinate.
     * @return unit string
     */
    public String getUnitString() {
        return units;
    }

    /**
     * Get whether this coordinate is time dependent.
     * @return true if time dependent
     */
    public boolean isTimeDependent() {
        return (timeDim != null);
    }

    /**
     * Get the time Dimension
     *
     * @return time Dimension
     */
    protected Dimension getTimeDimension() {
        return timeDim;
    }


    /**
     * Read the data {@link ucar.ma2.Array} from the variable, at the specified
     * time index if applicable.  If the variable does not have a time
     * dimension, the data array will have the same rank as the Variable.
     * If the variable has a time dimension, the data array will have rank-1.
     *
     * @param v             variable to read
     * @param timeIndex     time index, ignored if !isTimeDependent()
     * @return Array from   the variable at that time index
     *
     * @throws IOException problem reading data
     */
    protected Array readArray(Variable v, int timeIndex) throws IOException, InvalidRangeException {
        int[] shape  = v.getShape();
        int[] origin = new int[v.getRank()];

        if (getTimeDimension() != null) {
            int dimIndex = v.findDimensionIndex(getTimeDimension().getName());
            if (dimIndex >= 0) {
                shape[dimIndex]  = 1;
                origin[dimIndex] = timeIndex;
            }
        }

        return v.read(origin, shape).reduce();
    }

  public VerticalTransform subset(Range t_range, Range z_range, Range y_range, Range x_range) throws ucar.ma2.InvalidRangeException {
    return new VerticalTransformSubset(this, t_range, z_range, y_range, x_range);
  }

}