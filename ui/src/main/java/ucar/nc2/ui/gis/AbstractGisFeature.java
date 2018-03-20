/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis;

import ucar.unidata.geoloc.*;

import java.awt.geom.GeneralPath;
import java.awt.Shape;

/**
 * Abstract class that implements common methods for concrete
 * implementations of GisFeature.
 *
 * @author Russ Rew
 * @author John Caron
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
   *
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
      for (int i = 1; i < np; i++) {
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

    boolean showPts = ucar.util.prefs.ui.Debug.isSet("projection/showPoints");

    java.util.Iterator pi = getGisParts();
    while (pi.hasNext()) {
      GisPart gp = (GisPart) pi.next();
      double[] xx = gp.getX();
      double[] yy = gp.getY();
      boolean skipPrev = false;
      int count = 0;
      for (int i = 0; i < gp.getNumPoints(); i++) {
        workL.set(yy[i], xx[i]);
        ProjectionPoint pt = displayProject.latLonToProj(workL);

        if (showPts) {
          System.out.println("AbstractGisFeature getProjectedShape 1 " + xx[i] + " " + yy[i] + " === " + pt.getX() + " " + pt.getY());
          if (displayProject.crossSeam(pt, lastW)) System.out.println("***cross seam");
        }

        // deal with possible NaNs
        if (Double.isNaN(pt.getX()) || Double.isNaN(pt.getY())) {
          skipPrev = true;
          continue;
        }

        if ((count == 0) || skipPrev || displayProject.crossSeam(pt, lastW))
          path.moveTo((float) pt.getX(), (float) pt.getY());
        else
          path.lineTo((float) pt.getX(), (float) pt.getY());

        count++;
        skipPrev = false;

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
   * @param dataProject    data Projection to use.
   * @param displayProject display Projection to use.
   * @return shape corresponding to this feature
   */
  public Shape getProjectedShape(ProjectionImpl dataProject, ProjectionImpl displayProject) {
    ProjectionPointImpl pt1 = new ProjectionPointImpl();
    ProjectionPointImpl lastW = new ProjectionPointImpl();
    GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, getNumPoints());

    boolean showPts = ucar.util.prefs.ui.Debug.isSet("projection/showPoints");

    java.util.Iterator pi = getGisParts();
    while (pi.hasNext()) {
      GisPart gp = (GisPart) pi.next();
      double[] xx = gp.getX();
      double[] yy = gp.getY();
      boolean skipPrev = false;
      int count = 0;
      for (int i = 0; i < gp.getNumPoints(); i++) {
        pt1.setLocation(xx[i], yy[i]);
        LatLonPoint llpt = dataProject.projToLatLon(pt1);
        ProjectionPoint pt2 = displayProject.latLonToProj(llpt);

        if (showPts) {
          System.out.println("AbstractGisFeature getProjectedShape 2 " + xx[i] + " " + yy[i] + " === " + pt2.getX() + " " + pt2.getY());
          if (displayProject.crossSeam(pt2, lastW)) System.out.println("***cross seam");
        }

        // deal with possible NaNs
         if (Double.isNaN(pt2.getX()) || Double.isNaN(pt2.getY())) {
           skipPrev = true;
           continue;
         }

        if ((count == 0) || skipPrev || displayProject.crossSeam(pt2, lastW))
          path.moveTo((float) pt2.getX(), (float) pt2.getY());
        else
          path.lineTo((float) pt2.getX(), (float) pt2.getY());
        count++;
        skipPrev = false;

        lastW.setLocation(pt2);
      }
    }
    return path;
  }

} // AbstractGisFeature