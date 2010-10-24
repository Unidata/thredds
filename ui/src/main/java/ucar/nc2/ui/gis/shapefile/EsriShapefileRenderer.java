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
  private static java.util.Map sfileHash = new HashMap(); // map of (filename -> EsriShapefileRenderer)
  private static double defaultCoarseness = 0.0; // expose later?

  /**
   * Use factory to obtain a EsriShapefileRenderer.  This caches the EsriShapefile for reuse.
   * <p>
   * Implementation note: should switch to weak references.
   */
  static public EsriShapefileRenderer factory(String filename) {
    if (sfileHash.containsKey(filename))
      return (EsriShapefileRenderer) sfileHash.get(filename);

    try {
      EsriShapefileRenderer sfile = new EsriShapefileRenderer(filename);
      sfileHash.put(filename, sfile);
      return sfile;
    } catch (Exception ex) {
      System.err.println("EsriShapefileRenderer failed on " + filename+"\n"+ex);
      //ex.printStackTrace();
      return null;
    }
  }

  static public EsriShapefileRenderer factory(String key, InputStream stream) {
    if (sfileHash.containsKey(key))
      return (EsriShapefileRenderer) sfileHash.get(key);

    try {
      EsriShapefileRenderer sfile = new EsriShapefileRenderer(stream);
      sfileHash.put(key, sfile);
      return sfile;
    } catch (Exception ex) {
      System.err.println("EsriShapefileRenderer failed on " + stream+"\n"+ex);
      return null;
    }
  }

  ////////////////////////////////////////
  private EsriShapefile esri = null;
  private ProjectionImpl dataProject = new LatLonProjection ("Cylindrical Equidistant");

  private EsriShapefileRenderer(InputStream stream) throws IOException {
    super();
    esri = new EsriShapefile(stream, null, defaultCoarseness);

    double avgD = getStats(esri.getFeatures().iterator());
    createFeatureSet(avgD);
    createFeatureSet(2*avgD);
    createFeatureSet(3*avgD);
    createFeatureSet(5*avgD);
    createFeatureSet(8*avgD);
  }

  private EsriShapefileRenderer(String filename) throws IOException {
    this(filename, defaultCoarseness);
  }

  private EsriShapefileRenderer(String filename, double coarseness) throws IOException {
    super();
    esri = new EsriShapefile(filename, coarseness);

    double avgD = getStats(esri.getFeatures().iterator());
    createFeatureSet(2*avgD);
    createFeatureSet(4*avgD);
    createFeatureSet(8*avgD);
    createFeatureSet(16*avgD);
  }

  public LatLonRect getPreferredArea() {
    Rectangle2D bb = esri.getBoundingBox();
    return new LatLonRect(new LatLonPointImpl(bb.getMinY(), bb.getMinX()), bb.getHeight(), bb.getWidth());
  }

  protected java.util.List getFeatures() {
    return esri.getFeatures();
  }

  protected ProjectionImpl getDataProjection() { return dataProject; }

}