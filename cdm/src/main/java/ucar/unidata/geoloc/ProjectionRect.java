/*
 * $Id: ProjectionRect.java,v 1.15 2006/11/18 19:03:14 dmurray Exp $
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


import java.awt.geom.Rectangle2D;

import java.io.*;


/**
 * Bounding box for ProjectionPoint's.
 * This is a subclass of java.awt.geom.Rectangle2D.Double.
 * Note that getX() getY() really means getMinX(), getMinY(), rather than
 *   "upper left point" of the rectangle.
 *
 * @author John Caron
 * @version $Id: ProjectionRect.java,v 1.15 2006/11/18 19:03:14 dmurray Exp $
 */
public class ProjectionRect extends java.awt.geom.Rectangle2D.Double implements java.io.Serializable {

    /** default constructor, initialized to center (0,0) and width (10000, 10000) */
    public ProjectionRect() {
        this(-5000, -5000, 5000, 5000);
    }

    /**
     * Copy Constructor
     *
     * @param r  rectangle to copy
     */
    public ProjectionRect(Rectangle2D r) {
        this(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY());
    }

    /**
     * construct a MapArea from any two opposite corner points
     *  @param x1 x coord of any corner of the bounding box
     *  @param y1 y coord of the same corner as x1
     *  @param x2 x coord of opposite corner from x1,y1
     *  @param y2 y coord of same corner as x2
     */
    public ProjectionRect(double x1, double y1, double x2, double y2) {
        double wx0    = 0.5 * (x1 + x2);
        double wy0    = 0.5 * (y1 + y2);
        double width  = Math.abs(x1 - x2);
        double height = Math.abs(y1 - y2);
        setRect(wx0 - width / 2, wy0 - height / 2, width, height);
    }




    /**
     * Get the Lower Right Point
     *
     * @return the Lower Right Point
     */
    public ProjectionPoint getLowerRightPoint() {
        return new ProjectionPointImpl(getMaxPoint().getX(),
                                       getMinPoint().getY());
    }

    /**
     * Get the Upper Left Point (same as getMaxPoint)
     *
     * @return the Upper Left Point
     */
    public ProjectionPoint getUpperRightPoint() {
        return getMaxPoint();
    }

    /**
     * Get the Lower Right Point (same as getMinPoint)
     *
     * @return the Lower Right Point
     */
    public ProjectionPoint getLowerLeftPoint() {
        return getMinPoint();
    }

    /**
     * Get the Upper Left Point
     *
     * @return the Upper Left Point
     */
    public ProjectionPoint getUpperLeftPoint() {
        return new ProjectionPointImpl(getMinPoint().getX(),
                                       getMaxPoint().getY());
    }

    /**
     * Get the minimum corner of the bounding box.
     * @return minimum corner of the bounding box
     */
    public ProjectionPoint getMinPoint() {
        return new ProjectionPointImpl(getX(), getY());
    }

    /**
     * Get the maximum corner of the bounding box.
     * @return maximum corner of the bounding box
     */
    public ProjectionPoint getMaxPoint() {
        return new ProjectionPointImpl(getX() + getWidth(),
                                       getY() + getHeight());
    }

    /**
     * Get a String representation of this object.
     * @return a String representation of this object.
     */
    public String toString() {
        return "min: " + getX() + " " + getY() + " size: " + getWidth() + " "
               + getHeight();
    }

    // bean serialization

    /**
     * set minimum X
     *
     * @param x  minimum x
     */
    public void setX(double x) {
        setRect(x, getY(), getWidth(), getHeight());
    }

    /**
     * set minimum Y
     *
     * @param y  minimum y
     */
    public void setY(double y) {
        setRect(getX(), y, getWidth(), getHeight());
    }

    /**
     * set X width
     *
     * @param w  x width
     */
    public void setWidth(double w) {
        setRect(getX(), getY(), w, getHeight());
    }

    /**
     * set Y height
     *
     * @param h  Y height
     */
    public void setHeight(double h) {
        setRect(getX(), getY(), getWidth(), h);
    }

    // serialization

    /**
     * Read the object from the input stream of the serialized object
     *
     * @param s  stream to read
     *
     * @throws ClassNotFoundException   couldn't file the class
     * @throws IOException  Problem reading from stream
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        double x = s.readDouble();
        double y = s.readDouble();
        double w = s.readDouble();
        double h = s.readDouble();
        setRect(x, y, w, h);
    }

    /**
     * Wrtie the object to the output stream
     *
     * @param s  stream to write
     *
     * @throws IOException  Problem writing to stream
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeDouble(getX());
        s.writeDouble(getY());
        s.writeDouble(getWidth());
        s.writeDouble(getHeight());
    }
}

/*
 *  Change History:
 *  $Log: ProjectionRect.java,v $
 *  Revision 1.15  2006/11/18 19:03:14  dmurray
 *  jindent
 *
 *  Revision 1.14  2006/04/07 21:14:20  jeffmc
 *  Clean up extraneous method
 *
 *  Revision 1.13  2006/04/01 02:30:22  caron
 *  netcdf Server
 *
 *  Revision 1.12  2005/11/07 13:11:08  jeffmc
 *  Get some x/y lat/lon problems figured out
 *
 *  Revision 1.11  2005/05/13 18:29:11  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.10  2004/09/22 21:22:59  caron
 *  mremove nc2 dependence
 *
 *  Revision 1.9  2004/07/30 16:24:41  dmurray
 *  Jindent and javadoc
 *
 *  Revision 1.8  2004/06/07 20:22:50  caron
 *  javadoc
 *
 *  Revision 1.7  2004/02/27 21:21:29  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.6  2004/01/29 17:34:58  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.5  2003/06/03 20:06:18  caron
 *  fix javadocs
 *
 *  Revision 1.4  2003/04/08 15:59:06  caron
 *  rework for nc2 framework
 *
 *  Revision 1.1  2002/12/13 00:53:09  caron
 *  pass 2
 *
 *  Revision 1.2  2002/04/29 22:45:40  caron
 *  bean sericaliztion fields
 *
 *  Revision 1.1.1.1  2002/02/26 17:24:45  caron
 *  import sources
 *
 *  Revision 1.3  2001/04/30 23:35:44  caron
 *  new ProjectionRect.java constructor
 *
 *  Revision 1.2  2000/08/18 04:15:19  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.1  1999/12/16 22:57:23  caron
 *  gridded data viewer checkin
 *
 *  Revision 1.2  1999/06/03 01:43:50  caron
 *  remove the damn controlMs
 *
 *  Revision 1.1  1999/06/03 01:26:15  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:52  caron
 *  startAgain
 *
 * # Revision 1.6  1999/03/16  16:58:17  caron
 * # fix StationModel editing; add TopLevel
 * #
 * # Revision 1.5  1999/03/08  19:45:20  caron
 * # world coord now Point2D
 * #
 * # Revision 1.4  1999/03/03  19:58:23  caron
 * # more java2D changes
 * #
 * # Revision 1.3  1999/02/15  23:05:53  caron
 * # upgrade to java2D, new ProjectionManager
 * #
 * # Revision 1.2  1998/12/14  17:10:56  russ
 * # Add comment for accumulating change histories.
 * #
 */








