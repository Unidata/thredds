/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis.worldmap;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import ucar.nc2.ui.gis.AbstractGisFeature;
import ucar.nc2.ui.gis.GisFeature;
import ucar.nc2.ui.gis.GisFeatureRenderer;
import ucar.nc2.ui.gis.GisPart;
import ucar.ui.util.Resource;
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
  private static final String WORLD_MAP = "/resources/ui/maps/cil_100km.mapr";
  private static final double SECS_PER_DEG = 3600.0;
  private static boolean debug = false, debugTime = false;

  private static WorldMapFeature worldMapFeature = null;
  private static ArrayList<GisFeature> gisList;
  private static ArrayList<GisPart> partList = null;
  private static int total_pts = 0;

      // read in lat/lon points one time for this class
  private static boolean readWorldMap () {
    long secs = System.currentTimeMillis();
    try (InputStream is = Resource.getFileResource( WORLD_MAP);
        DataInputStream dis = new java.io.DataInputStream(is)) {

      // need an AbstractGisFeature for visad
      worldMapFeature = new WorldMapFeature();
      // need an ArrayList of AbstractGisFeature's for GisFeatureRenderer
      gisList = new ArrayList<>();
      gisList.add(worldMapFeature);

      partList = new ArrayList<>();

      while (true) {
        try {
          int npts = dis.readInt();

          dis.readInt();  // minx -- not used.
          dis.readInt();  // maxx -- not used.
          dis.readInt();  // miny -- not used.
          dis.readInt();  // maxy -- not used.

          MapRun run = new MapRun(npts);
          for (int i = 0; i < npts; i++) {
            run.wx[i] = ((double) dis.readInt()) / SECS_PER_DEG;
            run.wy[i] = ((double) dis.readInt()) / SECS_PER_DEG;
          }
          partList.add(run);
          total_pts += npts;

        } catch (EOFException ex) {
          break;
        } catch (Exception ex) {
          System.err.println("WorldMap exception " + ex);
          break;
        }
      }
    } catch (IOException e) {
      System.err.println("WorldMap read failed on resource " + WORLD_MAP);
    }

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
    public java.util.Iterator<GisPart> getGisParts() { return partList.iterator(); }
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
  private final ProjectionImpl dataProjection;

  // constructor
  public WorldMap () {
    if (partList == null)  // read in world map points the first time
      readWorldMap();
    dataProjection = new LatLonProjection ("Cylindrical Equidistant");
  }

  private LatLonRect defaultLLBB = new LatLonRect( new LatLonPointImpl(-180., -90.), 360., 180.);
  public LatLonRect getPreferredArea() { return defaultLLBB; }
  protected List<GisFeature> getFeatures() { return gisList; }
  protected ProjectionImpl getDataProjection() { return dataProjection; }
}

