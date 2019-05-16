/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis;


/**
 * An interface for simple GIS parts, (analogous to ESRI Shapefile parts).
 *
 * @author Russ Rew
 */

public interface GisPart  {

    /**
     * Get number of points in this part.
     *
     * @return number of points in this part.
     */
    int getNumPoints();

    /**
     * Get x coordinates for this part.
     *
     * @return array of x coordinates.
     */
    double[] getX();


    /**
     * Get y coordinates for this part.
     *
     * @return array of y coordinates.
     */
    double[] getY();

} // GisPart

