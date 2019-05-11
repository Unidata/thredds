/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis;


/**
 * An interface for GIS features, (analogous to ESRI Shapefile shapes).
 *
 * Created: Sat Feb 20 16:44:29 1999
 *
 * @author Russ Rew
 */

public interface GisFeature  {

    /**
     * Get the bounding box for this feature.
     *
     * @return rectangle bounding this feature
     */
    java.awt.geom.Rectangle2D getBounds2D();

    /**
     * Get total number of points in all parts of this feature.
     *
     * @return total number of points in all parts of this feature.
     */
    int getNumPoints();

    /**
     * Get number of parts comprising this feature.
     *
     * @return number of parts comprising this feature.
     */
    int getNumParts();

    /**
     * Get the parts of this feature, in the form of an iterator.
     *
     * @return the iterator over the parts of this feature.  Each part
     * is a GisPart.
     */
    java.util.Iterator getGisParts();

} // GisFeature
