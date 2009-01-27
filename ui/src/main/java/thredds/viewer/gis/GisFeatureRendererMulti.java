// $Id: GisFeatureRendererMulti.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.viewer.gis;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import thredds.datamodel.gis.*;

import ucar.util.prefs.ui.Debug;

import java.awt.Color;
import java.awt.Shape;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Superclass for rendering collections of GisFeatures.
 *
 * @author: John Caron
 * @version: $Id: GisFeatureRendererMulti.java 50 2006-07-12 16:30:06Z caron $
 */
public abstract class GisFeatureRendererMulti extends GisFeatureRenderer {
  private static boolean useDiscretization = false;
  private static double pixelMatch = 2.0;
  public static void setDiscretization( boolean b) { useDiscretization = b; }
  public static void setPixelMatch( double d) { pixelMatch = d; }

  private ArrayList featSetList = null;    // list of fetaureSets for progressive disclosure

  ////// this is what the subclasses have to implement (besides the constructor)
  public abstract LatLonRect getPreferredArea();
  protected abstract java.util.List getFeatures();      // collection of AbstractGisFeature
  protected abstract ProjectionImpl getDataProjection();    // what projection is the data in?

    /**
     * Sets new projection for subsequent drawing.
     *
     * @param project the new projection
     */
  public void setProjection(ProjectionImpl project) {
    displayProject = project;

    if (featSetList == null)
      return;
    Iterator iter = featSetList.iterator();
    while (iter.hasNext()) {
      FeatureSet fs = (FeatureSet) iter.next();
      fs.newProjection = true;
    }
  }

  public void createFeatureSet(double minDist) {
    // make a FeatureSet out of this, defer actually creating the points
    FeatureSet fs = new FeatureSet(null, minDist);

    // add to the list of featureSets
    if (featSetList == null)
      initFeatSetList();
    featSetList.add(fs);
  }

  ////////////////////////////

  // get the set of shapes to draw.
  // we have to deal with both projections and resolution-dependence
  protected Iterator getShapes(java.awt.Graphics2D g, AffineTransform normal2device) {
    long startTime = System.currentTimeMillis();

    if (featSetList == null)
      initFeatSetList();

    // which featureSet should we ue?
    FeatureSet fs = null;
    if (featSetList.size() == 1) {
      fs = (FeatureSet) featSetList.get(0);
    } else {
        // compute scale
      double scale = 1.0;
      try {
        AffineTransform world2device = g.getTransform();
        AffineTransform world2normal = normal2device.createInverse();
        world2normal.concatenate( world2device);
        scale = Math.max(Math.abs(world2normal.getScaleX()), Math.abs(world2normal.getShearX()));   // drawing or printing
        if (Debug.isSet("GisFeature/showTransform")) {
          System.out.println("GisFeature/showTransform: "+world2normal+ "\n scale = "+ scale);
        }
      } catch ( java.awt.geom.NoninvertibleTransformException e) {
        System.out.println( " GisRenderFeature: NoninvertibleTransformException on " + normal2device);
      }
      if (!displayProject.isLatLon())
        scale *= 111.0;  // km/deg
      double minD = Double.MAX_VALUE;
      for (int i=0; i<featSetList.size(); i++) {
        FeatureSet tryfs = (FeatureSet) featSetList.get(i);
        double d = Math.abs(scale * tryfs.minDist - pixelMatch);  // we want min features ~ 2 pixels
        if (d < minD) {
          minD = d;
          fs = tryfs;
        }
      }
      if (Debug.isSet("GisFeature/MapResolution")) {
        System.out.println("GisFeature/MapResolution: scale = "+scale+" minDist = "+fs.minDist);
      }
    }

    // we may have deferred the actual creation of the points
    if (fs.featureList == null)
      fs.createFeatures();

    // ok, now see if we need to project
    if (!displayProject.equals(fs.project)) {
      fs.setProjection( displayProject);
    } else {    // deal with LatLon
      if (fs.newProjection && displayProject.isLatLon()) {
        fs.setProjection( displayProject);
      }
    }
    fs.newProjection = false;

    if (Debug.isSet("GisFeature/timing/getShapes")) {
      long tookTime = System.currentTimeMillis() - startTime;
      System.out.println("timing.getShapes: " + tookTime*.001 + " seconds");
    }

      // so return it, already
    return fs.getShapes();
  }

    // make an ArrayList of Shapes from the given featureList and current display Projection
  private ArrayList makeShapes(Iterator featList) {
    Shape shape;
    ArrayList shapeList = new ArrayList();
    ProjectionImpl dataProject = getDataProjection();

    if (Debug.isSet("GisFeature/MapDraw")) {
      System.out.println("GisFeature/MapDraw: makeShapes with "+displayProject);
    }

/*    if (Debug.isSet("bug.drawShapes")) {
      int count =0;
      // make each GisPart a seperate shape for debugging
feats:while (featList.hasNext()) {
        AbstractGisFeature feature = (AbstractGisFeature) featList.next();
        java.util.Iterator pi = feature.getGisParts();
        while (pi.hasNext()) {
          GisPart gp = (GisPart) pi.next();
          int np = gp.getNumPoints();
          GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, np);
          double[] xx = gp.getX();
          double[] yy = gp.getY();
          path.moveTo((float) xx[0], (float) yy[0]);
          if (count == 63)
                System.out.println("moveTo x ="+xx[0]+" y= "+yy[0]);
          for(int i = 1; i < np; i++) {
            path.lineTo((float) xx[i], (float) yy[i]);
            if (count == 63)
                System.out.println("lineTo x ="+xx[i]+" y= "+yy[i]);
          }
          shapeList.add(path);
          if (count == 63)
            break feats;
          count++;
        }
      }
      System.out.println("bug.drawShapes: #shapes =" +shapeList.size());
      return shapeList;
    }  */

    while (featList.hasNext()) {
      AbstractGisFeature feature = (AbstractGisFeature) featList.next();
      if (dataProject.isLatLon())   // always got to run it through if its lat/lon
        shape = feature.getProjectedShape(displayProject);
      else if (dataProject == displayProject)
        shape = feature.getShape();
      else
        shape = feature.getProjectedShape(dataProject, displayProject);

      shapeList.add(shape);
    }

    return shapeList;
  }

  private void initFeatSetList() {
    featSetList = new ArrayList();
    featSetList.add( new FeatureSet( getFeatures(), 0.0)); // full resolution set
  }

  private class FeatureSet {
    List featureList = null;
    double minDist;
    ProjectionImpl project = null;
    ArrayList shapeList = null;
    boolean newProjection = true;
    double centerLon = 0.0;

    FeatureSet(List featureList, double minDist) {
      this.featureList = featureList;
      this.minDist = minDist;
    }

    void setProjection(ProjectionImpl project) {
      this.project = project;
      shapeList = makeShapes( featureList.iterator());

      if (project.isLatLon()) {   // why?
        LatLonProjection llproj = (LatLonProjection) project;
        centerLon = llproj.getCenterLon();
      }
    }

    Iterator getShapes() { return shapeList.iterator(); }

    void createFeatures() {
      ProjectionPointImpl thisW = new ProjectionPointImpl();
      ProjectionPointImpl lastW = new ProjectionPointImpl();

      featureList = new ArrayList();

      Iterator iter = GisFeatureRendererMulti.this.getFeatures().iterator();   // this is the original, full resolution set
      while (iter.hasNext()) {
        AbstractGisFeature feature = (AbstractGisFeature) iter.next();
        FeatureMD featMD = new FeatureMD(minDist);

        Iterator pi = feature.getGisParts();
        while (pi.hasNext()) {
          GisPart gp = (GisPart) pi.next();
          FeatureMD.Part part = featMD.newPart(gp.getNumPoints());

          int np = gp.getNumPoints();
          double[] xx = gp.getX();
          double[] yy = gp.getY();

          part.set( xx[0], yy[0]);
          for(int i=1; i < np-1; i++)
            part.setIfDistant( xx[i], yy[i]);

          if (part.getNumPoints() > 1) {
            part.set( xx[np-1], yy[np-1]); // close polygons
            part.truncateArray();
            featMD.add(part);
          }
        } // loop over parts

        if (featSetList == null)
          initFeatSetList();
        if (featMD.getNumParts() > 0)
          featureList.add( featMD);
      } // loop over featuures

      getStats(featureList.iterator());
    }  // createFeatures()

    private void discretizeArray(double[] d, int n) {
      if (minDist == 0.0)
        return;
      for (int i = 0; i < n; i++) {
        d[i] = (Math.rint(d[i]/minDist) * minDist) + minDist/2;
      }
    }

  } // FeatureSet inner class

    // these are derived Features based on a mimimum distance between points
  private class FeatureMD extends AbstractGisFeature {
    private ArrayList parts = new ArrayList();
    private int total_pts = 0;
    private double minDist;
    private double minDist2;

    FeatureMD(double minDist) {
      this.minDist = minDist;
      minDist2 = minDist * minDist;
    }

    void add( FeatureMD.Part part) {
      total_pts += part.getNumPoints();
      parts.add(part);
    }

    FeatureMD.Part newPart(int maxPts) {
      return new FeatureMD.Part(maxPts);
    }

    private double discretize(double d) {
      if (!useDiscretization || (minDist == 0.0))
        return d;
      return (Math.rint(d/minDist) * minDist) + minDist/2;
    }


      // implement GisFeature
    public java.awt.geom.Rectangle2D getBounds2D() { return null; }
    public int getNumPoints() { return total_pts; }
    public int getNumParts() { return parts.size(); }
    public java.util.Iterator getGisParts() { return parts.iterator(); }

    class Part implements GisPart {
      private int size;
      private double [] wx;  // lat/lon coords
      private double [] wy;

      // constructor
      Part ( int maxPts) {
        wx = new double[maxPts];
        wy = new double[maxPts];
        size = 0;
        minDist2 = minDist * minDist;
      }

      void set(double x, double y) {
        wx[size] = discretize(x);
        wy[size] = discretize(y);
        size++;
      }

      private void setNoD(double x, double y) {
        wx[size] = x;
        wy[size] = y;
        size++;
      }


      void setIfDistant(double x, double y) {
        x = discretize(x);
        y = discretize(y);
        double dx = x - wx[size-1];
        double dy = y - wy[size-1];
        double dist2 = dx*dx + dy*dy;
        if (dist2 >= minDist2)
     //   if ((x != wx[size-1]) || (y != wy[size-1]))
          setNoD(x, y);
      }

      void truncateArray() {
        double [] x = new double[size];
        double [] y = new double[size];

        for (int i=0; i<size; i++) {
          x[i] = wx[i];                 // arraycopy better?
          y[i] = wy[i];                 // arraycopy better?
        }
        wx = x;
        wy = y;
      }

        // implement GisPart
      public int getNumPoints() { return size; }
      public double[] getX() { return wx; }
      public double[] getY() { return wy; }
    }
  }


  protected double getStats(Iterator featList) {
    int total_pts = 0;
    int total_parts = 0;
    int total_feats = 0;
    int cross_pts = 0;
    double avgD = 0;
    double minD = Double.MAX_VALUE;
    double maxD = -Double.MAX_VALUE;

    ProjectionImpl dataProject = getDataProjection();
    ProjectionPointImpl thisW = new ProjectionPointImpl();
    ProjectionPointImpl lastW = new ProjectionPointImpl();

    while (featList.hasNext()) {
      AbstractGisFeature feature = (AbstractGisFeature) featList.next();
      total_feats++;

      Iterator pi = feature.getGisParts();
      while (pi.hasNext()) {
        GisPart gp = (GisPart) pi.next();
        total_parts++;

        double[] xx = gp.getX();
        double[] yy = gp.getY();
        int np = gp.getNumPoints();

        lastW.setLocation( xx[0], yy[0]);

        for(int i = 1; i < np; i++) {
          thisW.setLocation( xx[i], yy[i]);
          if (!dataProject.crossSeam(thisW, lastW)) {
            double dx = (xx[i] - xx[i-1]);
            double dy = (yy[i] - yy[i-1]);
            double dist = Math.sqrt(dx*dx + dy*dy);

            total_pts++;
            avgD += dist;
            minD = Math.min(minD, dist);
            maxD = Math.max(maxD, dist);
          } else
            cross_pts++;

          lastW.setLocation( xx[i], yy[i]);
        }
      }
    }

    avgD = (avgD/total_pts);
    if (Debug.isSet("GisFeature/MapResolution")) {
      System.out.println("Map.resolution: total_feats = "+ total_feats);
      System.out.println(" total_parts = "+ total_parts);
      System.out.println(" total_pts = "+ total_pts);
      System.out.println(" cross_pts = "+ cross_pts);
      System.out.println(" avg distance = "+ avgD);
      System.out.println(" min distance = "+ minD);
      System.out.println(" max distance = "+ maxD);
    }

    return avgD;
  }
}

/* Change History:
   $Log: GisFeatureRendererMulti.java,v $
   Revision 1.5  2005/03/05 03:48:19  caron
   bug using Double.MIN_VALUE instead of -Double.MAX_VALUE when finding limits

   Revision 1.4  2004/09/24 03:26:37  caron
   merge nj22

   Revision 1.3  2003/04/08 18:16:21  john
   nc2 v2.1

   Revision 1.2  2003/03/17 21:12:36  john
   new viewer

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:49  caron
   import sources

   Revision 1.2  2000/08/18 04:15:24  russ
   Licensed under GNU LGPL.

   Revision 1.1  2000/05/16 22:38:01  caron
   factor GisFeatureRenderer

   Revision 1.4  2000/03/01 19:31:24  caron
   setProjection bug

   Revision 1.3  2000/02/17 20:18:02  caron
   make printing work for zoom resolution maps

   Revision 1.2  2000/02/11 01:24:42  caron
   add getDataProjection()

   Revision 1.1  2000/02/10 17:45:11  caron
   add GisFeatureRenderer,GisFeatureAdapter

*/
