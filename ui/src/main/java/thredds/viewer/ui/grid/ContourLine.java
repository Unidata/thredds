// $Id: ContourLine.java,v 1.2 2004/09/24 03:26:42 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package thredds.viewer.ui.grid;

import thredds.datamodel.gis.*;

import java.awt.geom.*;  // for Point2D.Double
import java.util.*;      // for Iterator and ArrayList

/* ContourLine
 * Encapsulates a contour line, with a GisPart,a contour value,
 * and a boolean indicator if this line was edited or is a new line.
 *
 * @author Stuart Wier
 * @version  $Id: ContourLine.java,v 1.2 2004/09/24 03:26:42 caron Exp $
 *
 * This holds one single connected contour line. It does not hold
 * several unconnected contour lines for the same grid even if there
 * are several such line of the same contour value. Each one has one ContourLine
 * instance.
 *
 * Does not know anything about any display method, particular coordinate system,
 * or line labels.
 */
    public class ContourLine implements GisPart {
        private double contourLevel; // the contour's level or value
        private boolean newLineFlag=false; // true if this line is new or edited
        int npts;
        double [] wx;  // contour line positions found in ContourGrid class
        double [] wy;

        /*
         * Constructors for the ContourLine object for this input data.
         * The object contains a polyline of straight line segments,
         * the contour's level or value, and a "moddifed" indicator
         * used for programs which can edit contour lines and
         * and work with contour lines that have been editied.
         * "ifNew" input argument is optional.
         * points is an ArrayList of Point2D.Double
         */
        public ContourLine(ArrayList points, double level) {
            this.npts = points.size();
            // define coordinate array size for this line
            wx = new double[npts];
            wy = new double[npts];

            // get each coord pair and push into the local x y arrays
            Point2D.Double onexypair = new Point2D.Double();
            for (int i=0; i<points.size(); i++) {
                onexypair = (Point2D.Double)(points.get(i));
                wx[i] = onexypair.getX();
                wy[i] = onexypair.getY();
                //System.out.println("  i="+i+"  ("
                //		   +(((Point2D.Double)(points.get(i))).getX())
                //		   +","+wy[i]+")");
            }
            //System.out.println("  end of line");
            contourLevel = level;
        }

        // constructor with indicator if contour Line is new or modified;
        // used for systems with editable contour lines.
        public ContourLine(ArrayList points, double level, boolean ifNew) {
            this.npts = points.size();
            // define coordinate array size for this line
            wx = new double[npts];
            wy = new double[npts];

            // get one coord pait and push into the local x y arrays
            Point2D.Double onexypair = new Point2D.Double();
            for (int i=0; i<points.size(); i++) {
                onexypair = (Point2D.Double)(points.get(i));
                wx[i] = onexypair.getX();
                wy[i] = onexypair.getY();
            }

            contourLevel = level;
            newLineFlag = ifNew;
        }

    // implement GisPart methods
    public int getNumPoints() { return npts; }
    public double[] getX() { return wx; }
    public double[] getY() { return wy; }

    /*
     * contourLevel is returned, the contour's value
     */
    public double getContourLevel() {
        return contourLevel;
    }

    /*
     * "newLineFlag" is returned, indicator if this contour is new or edited
     */
    public boolean isNewLine()   {
        return newLineFlag;
    }

    /*
     * set the "newLineFlag" flag,
     * the indicator if this contour is new or just edited
     */
    public void setNewLineFlag(boolean newflag)   {
        newLineFlag = newflag;
    }

    }  // end ContourLine class


/* Change History:
    $Log: ContourLine.java,v $
    Revision 1.2  2004/09/24 03:26:42  caron
    merge nj22

    Revision 1.1  2002/12/13 00:55:08  caron
    pass 2

    Revision 1.1.1.1  2002/02/26 17:24:51  caron
    import sources

    Revision 1.7  2000/08/18 04:16:26  russ
    Licensed under GNU LGPL.

    Revision 1.6  2000/02/11 00:55:08  wier
    debug statements added

    Revision 1.5  2000/02/10 23:05:28  wier
    this now implements GisPart

    Revision 1.4  2000/02/10 21:47:11  wier
    fixed re-declaration of gpline

    Revision 1.3  2000/02/10 15:33:30  wier
    added version and change history

 */
