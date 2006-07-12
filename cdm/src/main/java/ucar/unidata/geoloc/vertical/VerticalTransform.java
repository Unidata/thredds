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

import ucar.ma2.Range;


/**
 * A transformation to a vertical reference coordinate system,
 * such as height or pressure.
 *
 * @author  Unidata Development Team
 * @version $Revision$
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

/* Change History:
   $Log: VerticalTransform.java,v $
   Revision 1.11  2006/02/07 13:28:53  caron
   VerticalTransform subsetting

   Revision 1.10  2005/05/13 18:29:25  jeffmc
   Clean up the odd copyright symbols

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
