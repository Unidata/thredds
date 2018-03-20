/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

import java.io.IOException;

import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

/**
 * A transformation to a vertical reference coordinate system,
 * such as height or pressure.
 *
 * @author  Unidata Development Team
 */

public interface VerticalTransform {

    /**
     * Get the 3D vertical coordinate array for this time step.
     * Must be in "canonical order" : z, y, x.
     *
     * @param timeIndex the time index. Ignored if !isTimeDependent().
     *
     * @return  vertical coordinate array
     *
     * @throws java.io.IOException problem reading the data
     * @throws ucar.ma2.InvalidRangeException timeIndex out of bounds
     */
    ucar.ma2.ArrayDouble.D3 getCoordinateArray(int timeIndex) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    
    /**
     * Get the 1D vertical coordinate array for this time step and
     * the specified X,Y index for Lat-Lon point.
     *
     * @param timeIndex the time index. Ignored if !isTimeDependent().
     * @param xIndex    the x index
     * @param yIndex    the y index
     * @return vertical coordinate array
     * @throws java.io.IOException problem reading data
     * @throws ucar.ma2.InvalidRangeException _more_
     */
   ArrayDouble.D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) throws IOException, InvalidRangeException;
    

    /**
     * Get the unit string for the vertical coordinate.
     * @return unit string
     */
    String getUnitString();

    /**
     * Get whether this coordinate is time dependent.
     * @return true if time dependent
     */
    boolean isTimeDependent();

    /**
     *  Create a VerticalTransform as a section of an existing VerticalTransform.
     * 
     *  @param t_range subset the time dimension, or null if you want all of it
     *  @param z_range subset the vertical dimension, or null if you want all of it
     *  @param y_range subset the y dimension, or null if you want all of it
     *  @param x_range subset the x dimension, or null if you want all of it
     *  @return a new VerticalTransform for the given subset
     *  @throws ucar.ma2.InvalidRangeException if any Range is incompatible with the existing VerticalTransform
     */
    VerticalTransform subset(Range t_range, Range z_range, Range y_range, Range x_range) throws ucar.ma2.InvalidRangeException;
}

