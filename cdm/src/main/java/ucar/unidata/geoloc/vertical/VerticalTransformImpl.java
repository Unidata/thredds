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

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;


import ucar.nc2.*;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.List;

/**
 * A transformation to a vertical reference coordinate system,
 * such as height or pressure.
 *
 * @author  Unidata Development Team
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
     * @return  vertical coordinate array
     * @throws InvalidRangeException _more_
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
     * @throws InvalidRangeException _more_
     */
    protected Array readArray(Variable v, int timeIndex)
            throws IOException, InvalidRangeException {
        int[] shape  = v.getShape();
        int[] origin = new int[v.getRank()];

        if (getTimeDimension() != null) {
            int dimIndex = v.findDimensionIndex(getTimeDimension().getName());
            if (dimIndex >= 0) {
                shape[dimIndex]  = 1;
                origin[dimIndex] = timeIndex;
                return v.read(origin, shape).reduce(dimIndex);
            }
        }

        return v.read(origin, shape);
    }

    /**
     * Create a subset of this VerticalTransform.
     *
     * @param t_range subset the time dimension, or null if you want all of it
     * @param z_range subset the vertical dimension, or null if you want all of it
     * @param y_range subset the y dimension, or null if you want all of it
     * @param x_range subset the x dimension, or null if you want all of it
     *
     * @return the subsetted VerticalTransform
     *
     * @throws ucar.ma2.InvalidRangeException if any of the range parameters are illegal
     */
    public VerticalTransform subset(Range t_range, Range z_range,
                                    Range y_range, Range x_range)
            throws ucar.ma2.InvalidRangeException {
        return new VerticalTransformSubset(this, t_range, z_range, y_range, x_range);
    }

  protected String getParameterStringValue(List<Parameter> params, String name) {
    for (Parameter a : params) {
      if (name.equalsIgnoreCase(a.getName()))
        return a.getStringValue();
    }
    return null;
  }

  protected boolean getParameterBooleanValue(List<Parameter> params, String name) {
    for (Parameter p : params) {
      if (name.equalsIgnoreCase(p.getName()))
        return Boolean.valueOf(p.getStringValue());
    }
    return false;
  }

}

