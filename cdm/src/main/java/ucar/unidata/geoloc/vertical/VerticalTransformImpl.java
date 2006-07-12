/*
 * $Id$
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
 * @version $Revision$
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

/* Change History:
   $Log: VerticalTransformImpl.java,v $
   Revision 1.7  2006/06/26 23:33:21  caron
   bug fixes for IDV:
     Vert transform subsets now have correct units
     WRFEta slightly less buggy
     Radial CoordSys caching was stupid
     GribVariable ignore duplicate rcords (take 1st)

   Revision 1.6  2006/02/07 13:28:53  caron
   VerticalTransform subsetting

   Revision 1.5  2005/08/11 22:42:13  dmurray
   jindent (I'll leave the javadoc to those who forgot to)

   Revision 1.4  2005/05/13 18:29:25  jeffmc
   Clean up the odd copyright symbols

   Revision 1.3  2004/11/05 12:48:18  dmurray
   Jindent, fix a javadoc error

   Revision 1.2  2004/11/04 20:07:48  caron
   move readArray() up to superclass.

   Revision 1.1  2004/10/21 17:08:43  dmurray
   refactor.  Create VerticalTransformImpl to handle timeDim and units
   and have others extend this.

   Revision 1.9  2004/09/22 21:19:30  caron
   use Parameter, not Attribute

   Revision 1.8  2004/07/30 17:22:23  dmurray
   Jindent and doclint

   Revision 1.7  2004/07/30 15:24:40  dmurray
   add javadocs.  If I'm wanting Doug to do it, I guess I'd better give
   examples (even if I didn't write the code)

   Revision 1.6  2004/02/27 21:21:48  jeffmc
   Lots of javadoc warning fixes

   Revision 1.5  2004/01/29 17:35:12  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.4  2003/09/19 00:15:48  caron
   javadoc cleanup

   Revision 1.3  2003/07/14 23:04:01  caron
   fix javadoc

   Revision 1.2  2003/07/12 23:09:03  caron
   add cvs headers, trailers

*/
