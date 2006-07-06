// $Id: AbstractGisFeature.java,v 1.5 2005/06/11 19:03:56 caron Exp $
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
package thredds.datamodel.gis;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import java.awt.geom.GeneralPath;
import java.awt.Shape;

/**
 * Abstract class that implements common methods for concrete
 * implementations of GisFeature.
 *
 * @author Russ Rew
 * @author John Caron
 * @version $Id: AbstractGisFeature.java,v 1.5 2005/06/11 19:03:56 caron Exp $
 */

public abstract class AbstractGisFeature implements GisFeature {

      // subclasses must implement these methods
    public abstract java.awt.geom.Rectangle2D getBounds2D();  // may be null
    public abstract int getNumPoints();
    public abstract int getNumParts();
    public abstract java.util.Iterator getGisParts();

    /**
     * Convert this GisFeature to a java.awt.Shape, using the default
     * coordinate system, mapping gisFeature(x,y) -> screen(x,y).
     * LOOK STILL HAVE TO crossSeam()
     * @return shape corresponding to this feature.
     */
  public Shape getShape() {
    int npts = getNumPoints();
    GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, npts);

    java.util.Iterator pi = getGisParts();
    while (pi.hasNext()) {
      GisPart gp = (GisPart) pi.next();
      double[] xx = gp.getX();
      double[] yy = gp.getY();
      int np = gp.getNumPoints();
      if (np > 0)
        path.moveTo((float) xx[0], (float) yy[0]);
      for(int i = 1; i < np; i++) {
        path.lineTo((float) xx[i], (float) yy[i]);
      }
    }
    return path;
  }

    /**
     * Convert this GisFeature to a java.awt.Shape. The data coordinate system
     * is assumed to be (lat, lon), use the projection to transform points, so
     * project.latLonToProj(gisFeature(x,y)) -> screen(x,y).
     *
     * @param displayProject Projection to use to display
     * @return shape corresponding to this feature
     */
    public Shape getProjectedShape(ProjectionImpl displayProject) {
        LatLonPointImpl workL = new LatLonPointImpl();
        ProjectionPointImpl lastW = new ProjectionPointImpl();
        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, getNumPoints());

        boolean showPts = false; // ucar.util.prefs.ui.Debug.isSet("projection/showPoints");

        java.util.Iterator pi = getGisParts();
        while (pi.hasNext()) {
            GisPart gp = (GisPart) pi.next();
            double[] xx = gp.getX();
            double[] yy = gp.getY();
            for (int i=0; i < gp.getNumPoints(); i++) {
              workL.set(yy[i], xx[i]);
              ProjectionPoint pt = displayProject.latLonToProj(workL);

              if (showPts) {
                System.out.println( "getProjectedShape 1 "+xx[i]+" "+ yy[i]+" === "+pt.getX() +" "+ pt.getY());
                if (displayProject.crossSeam(pt, lastW)) System.out.println( "***cross seam");
              }

              if ((i==0) || displayProject.crossSeam(pt, lastW))
                path.moveTo((float) pt.getX(), (float) pt.getY());
              else
                path.lineTo((float) pt.getX(), (float) pt.getY());

              lastW.setLocation(pt);
            }
        }
        return path;
    }

    /**
     * Convert this GisFeature to a java.awt.Shape. The data coordinate system
     * is in the coordinates of dataProject, and the screen is in the coordinates of
     * displayProject. So:
     * displayProject.latLonToProj( dataProject.projToLatLon(gisFeature(x,y))) -> screen(x,y).
     *
     * @param dataProject: data Projection to use.
     * @param displayProject: display Projection to use.
     * @return shape corresponding to this feature
     */
    public Shape getProjectedShape(ProjectionImpl dataProject, ProjectionImpl displayProject) {
        ProjectionPointImpl pt1 = new ProjectionPointImpl();
        ProjectionPointImpl lastW = new ProjectionPointImpl();
        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, getNumPoints());

        boolean showPts = false; // ucar.util.prefs.ui.Debug.isSet("projection/showPoints");

        java.util.Iterator pi = getGisParts();
        while (pi.hasNext()) {
            GisPart gp = (GisPart) pi.next();
            double[] xx = gp.getX();
            double[] yy = gp.getY();
            for (int i=0; i < gp.getNumPoints(); i++) {
              pt1.setLocation(xx[i], yy[i]);
              LatLonPoint llpt = dataProject.projToLatLon(pt1);
              ProjectionPoint pt2 = displayProject.latLonToProj(llpt);

              if (showPts) {
                System.out.println( "getProjectedShape 2 "+xx[i]+" "+ yy[i]+" === "+pt2.getX() +" "+ pt2.getY());
                if (displayProject.crossSeam(pt2, lastW)) System.out.println( "***cross seam");
              }

              if ((i==0) || displayProject.crossSeam(pt2, lastW))
                path.moveTo((float) pt2.getX(), (float) pt2.getY());
              else
                path.lineTo((float) pt2.getX(), (float) pt2.getY());

              lastW.setLocation(pt2);
            }
        }
        return path;
    }

} // AbstractGisFeature

/* Change History:
   $Log: AbstractGisFeature.java,v $
   Revision 1.5  2005/06/11 19:03:56  caron
   no message

   Revision 1.4  2004/09/24 03:26:32  caron
   merge nj22

   Revision 1.3  2003/04/08 18:16:19  john
   nc2 v2.1

   Revision 1.2  2003/03/17 21:12:33  john
   new viewer

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:46  caron
   import sources

   Revision 1.12  2001/04/30 23:38:21  caron
   debug

   Revision 1.11  2000/10/03 21:08:53  caron
   bug in getShape() when GisPart has 0 points

   Revision 1.10  2000/08/18 04:15:23  russ
   Licensed under GNU LGPL.

   Revision 1.9  2000/02/17 20:20:15  caron
   trivial

   Revision 1.8  2000/02/10 17:45:09  caron
   add GisFeatureRenderer,GisFeatureAdapter

*/
