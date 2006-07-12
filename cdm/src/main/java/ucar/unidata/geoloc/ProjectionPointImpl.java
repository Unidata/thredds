/*
 * $Id:ProjectionPointImpl.java 63 2006-07-12 21:50:51Z edavis $
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

package ucar.unidata.geoloc;


import ucar.unidata.util.Format;

import java.awt.geom.Point2D;


/**
 * Our implementation of ProjectionPoint,
 * that subclasses java.awt.geom.Point2D to add serialization.
 *
 * @see ProjectionPoint
 * @author John Caron
 * @version $Id:ProjectionPointImpl.java 63 2006-07-12 21:50:51Z edavis $
 */
public class ProjectionPointImpl extends Point2D.Double
        implements ProjectionPoint, java.io.Serializable {

    /** Default constructor, initialized to 0,0 */
    public ProjectionPointImpl() {
        this(0.0, 0.0);
    }

    /**
     * Constructor that copies Point2D values into this.
     *
     * @param pt  point to copy
     */
    public ProjectionPointImpl(Point2D pt) {
        super(pt.getX(), pt.getY());
    }

    /**
     * Constructor that copies ProjectionPoint values into this.
     *
     * @param pt  point to copy
     */
    public ProjectionPointImpl(ProjectionPoint pt) {
        super(pt.getX(), pt.getY());
    }

    /**
     * Constructor, initialized to x, y
     *
     * @param x   x coordinate
     * @param y   y coordinate
     */
    public ProjectionPointImpl(double x, double y) {
        super(x, y);
    }

    /**
     * Returns true if this represents the same point as pt.
     *
     * @param pt   point to check
     * @return true if this represents the same point as pt.
     */
    public boolean equals(ProjectionPoint pt) {
        return (pt.getX() == getX()) && (pt.getY() == getY());
    }

    /**
     * nicely format this point
     * @return nicely formatted point
     */
    public String toString() {
        return Format.d(getX(), 4) + " " + Format.d(getY(), 4);
    }

    // convenience setting routines

    /**
     * set x,y location from  pt
     *
     * @param pt  point to use for values
     */
    public void setLocation(ProjectionPoint pt) {
        setLocation(pt.getX(), pt.getY());
    }


    /**
     * set x,y location from  pt
     *
     * @param pt  point to use for values
     */
    public void setLocation(Point2D pt) {
        setLocation(pt.getX(), pt.getY());
    }

    /**
     * See if either coordinate is +/- infinite. This happens sometimes
     * in projective geometry.
     *
     * @return true if either coordinate is +/- infinite.
     */
    public boolean isInfinite() {
        return (x == java.lang.Double.POSITIVE_INFINITY)
               || (x == java.lang.Double.NEGATIVE_INFINITY)
               || (y == java.lang.Double.POSITIVE_INFINITY)
               || (y == java.lang.Double.NEGATIVE_INFINITY);
    }

    /**
     * See if either coordinate in <code>pt</code> is +/- infinite.
     * This happens sometimes in projective geometry.
     *
     * @param pt  point to check
     * @return true if either coordinate is +/- infinite.
     */
    static public boolean isInfinite(ProjectionPoint pt) {
        return (pt.getX() == java.lang.Double.POSITIVE_INFINITY)
               || (pt.getX() == java.lang.Double.NEGATIVE_INFINITY)
               || (pt.getY() == java.lang.Double.POSITIVE_INFINITY)
               || (pt.getY() == java.lang.Double.NEGATIVE_INFINITY);
    }
}

/* Change History:
   $Log: ProjectionPointImpl.java,v $
   Revision 1.13  2005/05/13 18:29:10  jeffmc
   Clean up the odd copyright symbols

   Revision 1.12  2004/12/10 15:07:51  dmurray
   Jindent John's changes

   Revision 1.11  2004/09/22 21:22:59  caron
   mremove nc2 dependence

   Revision 1.10  2004/07/30 16:24:40  dmurray
   Jindent and javadoc

   Revision 1.9  2004/02/27 21:21:29  jeffmc
   Lots of javadoc warning fixes

   Revision 1.8  2004/01/29 17:34:58  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.7  2003/04/15 16:03:05  jeffmc
   Lots of changes. Mostly added in a MACROBDY tag in the leaf proj classes
   so we only have to define the actual math once. The script convert.tcl
   is used to generate the different projToLatLon/latLonToProj methods.

   Cleaned up the use of hard coded indices into the latlon and proj arrays.

   Added utility attribute setting methods in ProjectionImpl for the leaf classes
   to use.

   The old way of using the workP and workL objects was not thread safe, there
   was no synchronization on those objects. Changed the leaf methods
   to take a workP and workL object that can be created by the parent class method.
   For individual conversion calls it is still no thread safe but for the bulk
   ones it is.

   Revision 1.6  2003/04/08 15:59:06  caron
   rework for nc2 framework

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:45  caron
   import sources

   Revision 1.5  2000/08/18 04:15:18  russ
   Licensed under GNU LGPL.

   Revision 1.4  2000/05/16 22:24:01  caron
   new setLocation variant

   Revision 1.3  2000/05/09 20:42:49  caron
   change deprecated Format method

   Revision 1.2  2000/02/07 17:46:00  caron
   add equals() to ProjectionPoint

   Revision 1.1  1999/12/16 23:06:57  caron
   projection changes

   Revision 1.2  1999/06/03 01:43:51  caron
   remove the damn controlMs

   Revision 1.1  1999/06/03 01:26:17  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:52  caron
   startAgain

# Revision 1.5  1999/03/16  16:58:09  caron
# fix StationModel editing; add TopLevel
#
# Revision 1.4  1999/03/03  19:58:22  caron
# more java2D changes
#
# Revision 1.3  1999/02/15  23:05:52  caron
# upgrade to java2D, new ProjectionManager
#
# Revision 1.2  1998/12/14  17:10:55  russ
# Add comment for accumulating change histories.
#
*/







