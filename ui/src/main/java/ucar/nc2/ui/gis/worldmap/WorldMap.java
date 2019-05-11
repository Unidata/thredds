/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis.worldmap;

import ucar.nc2.ui.gis.AbstractGisFeature;
import ucar.nc2.ui.gis.GisFeatureRenderer;
import ucar.nc2.ui.gis.GisPart;
import ucar.nc2.ui.util.Resource;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.LatLonProjection;

import java.io.EOFException;
import java.util.ArrayList;

/** A simple "default" world map Renderer.
 * @author John Caron
 */

public class WorldMap extends GisFeatureRenderer {
  private static final double SECS_PER_DEG = 3600.0;
  private static ArrayList gisList;
  private static ArrayList partList = null;
  private static int total_pts = 0;
  private static WorldMapFeature worldMapFeature = null;

  private static boolean debug = false, debugTime = false;

      // read in lat/lon points one time for this class
  private static boolean readWorldMap () {
    java.io.DataInputStream dis;
    String filename = "/resources/nj22/ui/maps/cil_100km.mapr";
    java.io.InputStream is;
    long secs = System.currentTimeMillis();

    is = Resource.getFileResource( filename);

    if (is == null) {
        System.err.println("WorldMap read failed on resource " + filename);
        return false;
    } else {
        dis = new java.io.DataInputStream(is);
    }

      // need an AbstractGisFeature for visad
    worldMapFeature = new WorldMapFeature();
      // need an ArrayList of AbstractGisFeature's for GisFeatureRenderer
    gisList = new ArrayList();
    gisList.add(worldMapFeature);

    partList = new ArrayList();

    while (true) {
      try {
        int npts = dis.readInt();

        dis.readInt();  // minx -- not used.
        dis.readInt();  // maxx -- not used.
        dis.readInt();  // miny -- not used.
        dis.readInt();  // maxy -- not used.

        MapRun run = new MapRun(npts);
        for (int i=0; i<npts; i++) {
          run.wx[i] = ((double) dis.readInt()) / SECS_PER_DEG;
          run.wy[i] = ((double) dis.readInt()) / SECS_PER_DEG;
        }
        partList.add( run);
        total_pts += npts;

      } catch (EOFException ex) {
        break;
      } catch (Exception ex) {
        System.err.println("WorldMap exception " + ex);
        break;
      }
    }

    try { is.close();
    } catch (Exception ex) { }

    if (debugTime) {
      secs = System.currentTimeMillis() - secs;
      System.out.println("WorldMap read file: " + secs*.001 + " seconds");
    }
    return true;
  }

  private static class WorldMapFeature extends AbstractGisFeature {
    public java.awt.geom.Rectangle2D getBounds2D() { return null; }
    public int getNumPoints() { return total_pts; }
    public int getNumParts() { return partList.size(); }
    public java.util.Iterator getGisParts() { return partList.iterator(); }
  }

  private static class MapRun implements GisPart {
    int npts;
    double [] wx;  // lat/lon coords
    double [] wy;

    // constructor
    MapRun ( int npts) {
        this.npts = npts;

        wx = new double[npts];
        wy = new double[npts];
    }

      // implement GisPart
    public int getNumPoints() { return npts; }
    public double[] getX() { return wx; }
    public double[] getY() { return wy; }
  }

  public static AbstractGisFeature getWorldMap() {
    if (worldMapFeature == null)  // read in world map points the first time
      readWorldMap();
    return worldMapFeature;
  }

  /////////////////////////////////////////////////
  private ProjectionImpl dataProject = new LatLonProjection ("Cylindrical Equidistant");


  // constructor
  public WorldMap () {
    if (partList == null)  // read in world map points the first time
      readWorldMap();

    dataProject = new LatLonProjection ("Cylindrical Equidistant");
  }

  private LatLonRect defaultLLBB = new LatLonRect( new LatLonPointImpl(-180., -90.), 360., 180.);
  public LatLonRect getPreferredArea() { return defaultLLBB; }
  protected java.util.List getFeatures() { return gisList; }
  protected ProjectionImpl getDataProjection() { return dataProject; }

    /*
      Draws the World Map.
      @param g the Graphics2D context on which to draw
     * @param pixelAT   use this AffineTransform to draw in constant-pixel coordinates. ignored here.
     *
  public void draw(java.awt.Graphics2D g, java.awt.geom.AffineTransform pixelAT) {
    long  secs = System.currentTimeMillis();
    Rectangle2D clipRect = (Rectangle2D) g.getClip();

    if (debug) System.out.println("WorldMap draw clip "+ clipRect + " "+ g.getClipBounds());

    if ((null == project) || (null == path))
      return;
    g.setColor(color);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    g.setStroke(new java.awt.BasicStroke(0.0f));

      // special processing for latlon projection
    /* if (project instanceof LatLonProjection) {
      LatLonProjection llproj = (LatLonProjection) project;
      g.draw( new latlonShape(llproj));
    } else *
      g.draw(path);

    if (debugTime) {
      secs = System.currentTimeMillis() - secs;
      System.out.println("WorldMap redraw: " + secs*.001 + " seconds");
    }
  }

  public void setProjection(ProjectionImpl project) {
    this.project = project;
    long secs = System.currentTimeMillis();

    // recalculate Shape for this projection
    path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, total_pts);

    for (int i=0; i<runs.size(); i++) {
      MAPrun run = (MAPrun) runs.get(i);

      for (int pt=0; pt<run.npts; pt++) {
        workL.setLatitude(run.wy[pt]);
        workL.setLongitude(run.wx[pt]);
        ProjectionPoint ppt = project.latLonToProj( workL);

        if ((pt == 0) || project.crossSeam(ppt, lastW))  // deal with wrapping around the world
          path.moveTo((float)ppt.getX(), (float)ppt.getY());
        else
          path.lineTo((float)ppt.getX(), (float)ppt.getY());

        lastW.setLocation(ppt.getX(), ppt.getY());
      }
    }
    path.closePath();

    if (debugTime) {
      secs = System.currentTimeMillis() - secs;
      System.out.println("WorldMap recompute Projection: " + secs*.001 + " seconds");
    }
  }



/*  private class latlonShape implements Shape {
    private double centerLon;

    latlonShape(LatLonProjection ll) {
      centerLon = ll.getCenterLon();
      //System.out.println("  latlonShape centerLon: "+ centerLon);
    }

    public Rectangle getBounds() { return new Rectangle((int)(centerLon-360), -90, 720, 180); }
    public Rectangle2D getBounds2D(){ return new Rectangle2D.Double(centerLon-360, -90, 720, 180); }
    public boolean contains(double x, double y) { return false; }
    public boolean contains(Point2D p) { return false; }
    public boolean intersects(double x, double y, double w, double h) { return false; }
    public boolean intersects(Rectangle2D r) { return false; }
    public boolean contains(double x, double y, double w, double h) { return false; }
    public boolean contains(Rectangle2D r) { return false; }
    public PathIterator getPathIterator(AffineTransform at) { return new latlonPI(at); }
    public PathIterator getPathIterator(AffineTransform at, double flatness) { return new latlonPI(at); }

    private class latlonPI implements PathIterator {
      int nruns = runs.size();
      int runno = 0;
      MAPrun run = (MAPrun) runs.get(runno);
      int pt = 0;
      boolean newrun = true;
      boolean done = false;
      AffineTransform at;

        // minimize GC
      Point2D src = new Point2D.Double();
      Point2D dst = new Point2D.Double();

      latlonPI(AffineTransform at) {
        this.at = at;
        calcPt();       // get first point ready
      }

      public int getWindingRule() { return WIND_EVEN_ODD; }
      public boolean isDone() { return done; }
      public void next() {
        if (pt < run.npts-1) {
          pt++;
          newrun = false;
        } else if (runno < nruns -1) {
          runno++;
          run = (MAPrun) runs.get(runno);
          pt = 0;
          newrun = true;
        } else {
          done = true;
          return;
        }
        calcPt();
      }

      private void calcPt() {
          // convert to cyl.eq.
        workL.setLatitude(run.wy[pt]);
        workL.setLongitude(run.wx[pt]);
        ProjectionPoint ppt = project.latLonToProj( workL);

            // deal with wrapping around the world
        if (!done && !newrun && project.crossSeam(ppt, lastW))
          newrun = true;
        lastW.setLocation(ppt.getX(), ppt.getY());

          // affine transform if needed
        if (at != null) {
          src.setLocation( ppt.getX(), ppt.getY());
          at.transform(src, dst);
        } else
          dst.setLocation( ppt.getX(), ppt.getY());
      }

      public int currentSegment(float[] coords) {
        coords[0] = (float) dst.getX();
        coords[1] = (float) dst.getY();
        return newrun ? SEG_MOVETO : SEG_LINETO;
      }
      public int currentSegment(double[] coords) {
        coords[0] = dst.getX();
        coords[1] = dst.getY();
        return newrun ? SEG_MOVETO : SEG_LINETO;
      }
    } //inner class latlonPI
  } // inner class latlonShape  */
}

