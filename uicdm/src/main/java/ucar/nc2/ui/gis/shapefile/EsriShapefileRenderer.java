/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis.shapefile;

import ucar.nc2.ui.gis.GisFeatureRendererMulti;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Provides a convenient interface to ESRI shapefiles by creating lists of
 * ucar.unidata.gis.AbstractGisFeature.  Java2D Shape or VisAD SampledSet
 * objects can be created from these.
 *
 * @author Russ Rew
 * @author John Caron
 */
public class EsriShapefileRenderer extends GisFeatureRendererMulti {
  private static java.util.Map<String, EsriShapefileRenderer> sfileHash;
  private static double defaultCoarseness = 0.0; // expose later?

  /**
   * Use factory to obtain a EsriShapefileRenderer.  This caches the EsriShapefile for reuse.
   * <p/>
   * Implementation note: should switch to weak references.
   */
  public static EsriShapefileRenderer factory(String filename) {
    if (sfileHash == null)
      sfileHash = new HashMap<>();

    if (sfileHash.containsKey(filename))
      return sfileHash.get(filename);

    try {
      EsriShapefileRenderer sfile = new EsriShapefileRenderer(filename);
      sfileHash.put(filename, sfile);
      return sfile;
    } catch (Exception ex) {
      //System.err.println("EsriShapefileRenderer failed on " + filename + "\n" + ex);
      //ex.printStackTrace();
      return null;
    }
  }

  public static EsriShapefileRenderer factory(String key, InputStream stream) {
    if (sfileHash == null)
      sfileHash = new HashMap<>();

    if (sfileHash.containsKey(key))
      return sfileHash.get(key);

    try {
      EsriShapefileRenderer sfile = new EsriShapefileRenderer(stream);
      sfileHash.put(key, sfile);
      return sfile;
    } catch (Exception ex) {
      // System.err.println("EsriShapefileRenderer failed on " + stream + "\n" + ex);
      return null;
    }
  }

  ////////////////////////////////////////
  private EsriShapefile esri;
  private ProjectionImpl dataProject = new LatLonProjection("Cylindrical Equidistant");

  private EsriShapefileRenderer(InputStream stream) throws IOException {
    esri = new EsriShapefile(stream, null, defaultCoarseness);

    double avgD = getStats(esri.getFeatures().iterator());
    createFeatureSet(avgD);
    createFeatureSet(2 * avgD);
    createFeatureSet(3 * avgD);
    createFeatureSet(5 * avgD);
    createFeatureSet(8 * avgD);
  }

  private EsriShapefileRenderer(String filename) throws IOException {
    this(filename, defaultCoarseness);
  }

  private EsriShapefileRenderer(String filename, double coarseness) throws IOException {
    esri = new EsriShapefile(filename, coarseness);

    double avgD = getStats(esri.getFeatures().iterator());
    createFeatureSet(2 * avgD);
    createFeatureSet(4 * avgD);
    createFeatureSet(8 * avgD);
    createFeatureSet(16 * avgD);
  }

  public LatLonRect getPreferredArea() {
    Rectangle2D bb = esri.getBoundingBox();
    return new LatLonRect(new LatLonPointImpl(bb.getMinY(), bb.getMinX()), bb.getHeight(), bb.getWidth());
  }

  protected java.util.List getFeatures() {
    return esri.getFeatures();
  }

  protected ProjectionImpl getDataProjection() {
    return dataProject;
  }

}