/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis;


/**
 * This adapts a Gisfeature into a subclass of AbstractGisFeature.
 * Part of te ADT middleware pattern.
 *
 * @author John Caron
 */

public class GisFeatureAdapter extends AbstractGisFeature  {
  private GisFeature gisFeature; // adaptee

  public GisFeatureAdapter( GisFeature gisFeature) {
    this.gisFeature = gisFeature;
  }

    /**
     * Get the bounding box for this feature.
     *
     * @return rectangle bounding this feature
     */
    public java.awt.geom.Rectangle2D getBounds2D() { return gisFeature.getBounds2D(); }

    /**
     * Get total number of points in all parts of this feature.
     *
     * @return total number of points in all parts of this feature.
     */
    public int getNumPoints(){ return gisFeature.getNumPoints(); }

    /**
     * Get number of parts comprising this feature.
     *
     * @return number of parts comprising this feature.
     */
    public int getNumParts(){ return gisFeature.getNumParts(); }

    /**
     * Get the parts of this feature, in the form of an iterator.
     *
     * @return the iterator over the parts of this feature.  Each part
     * is a GisPart.
     */
    public java.util.Iterator getGisParts(){ return gisFeature.getGisParts(); }

} // GisFeatureAdapter