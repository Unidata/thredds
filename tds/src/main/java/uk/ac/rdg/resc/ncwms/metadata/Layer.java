/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.metadata;

import java.util.List;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.metadata.projection.HorizontalProjection;
import uk.ac.rdg.resc.ncwms.styles.Style;
import ucar.nc2.dt.GridDataset;

/**
 * Interface describing the methods that must be implemented by a  displayable
 * Layer in this WMS.
 * @author Jon
 */
public interface Layer
{
    /**
     * @return the index of the TimestepInfo object corresponding with the given
     * ISO8601 time string. Uses binary search for efficiency.
     * @throws InvalidDimensionValueException if there is no corresponding
     * TimestepInfo object, or if the given ISO8601 string is not valid.
     */
    int findTIndex(String isoDateTime) throws InvalidDimensionValueException;

    /**
     * Gets a List of integers representing indices along the time axis
     * starting from isoDateTimeStart and ending at isoDateTimeEnd, inclusive.
     * 
     * @param isoDateTimeStart ISO8601-formatted String representing the start time
     * @param isoDateTimeEnd ISO8601-formatted String representing the start time
     * @return List of Integer indices
     * @throws InvalidDimensionValueException if either of the start or end
     * values were not found in the axis, or if they are not valid ISO8601 times.
     */
    List<Integer> findTIndices(String isoDateTimeStart, String isoDateTimeEnd) throws InvalidDimensionValueException;

    /**
     * Finds the index of a certain z value by brute-force search.  We can afford
     * to be inefficient here because z axes are not likely to be large.
     * 
     * @param targetVal Value to search for
     * @return the z index corresponding with the given targetVal
     * @throws InvalidDimensionValueException if targetVal could not be found
     * within zValues
     */
    int findZIndex(String targetVal) throws InvalidDimensionValueException;

    String getAbstract();

    double[] getBbox();

    GridDataset getDataset();

    /**
     * @return the key of the default style for this Variable
     */
    Style getDefaultStyle();

    /**
     * @return the index of the default value of the t axis (i.e. the t value that will be
     * used if the user does not specify an explicit t value in a GetMap request),
     * as a TimestepInfo object.  This currently returns the last value along
     * the time axis, but should probably return the value closest to now.
     */
    int getDefaultTIndex();

    /**
     * @return the default value of the t axis (i.e. the t value that will be
     * used if the user does not specify an explicit t value in a GetMap request),
     * in milliseconds since the epoch.  This currently returns the last value along
     * the time axis, but should probably return the value closest to now.
     */
    long getDefaultTValue();

    /**
     * @return the index of the default value on the z axis (i.e. the index of
     * the z value that will be used if the user does not specify an explicit
     * z value in a GetMap request).
     */
    int getDefaultZIndex();

    /**
     * @return the default value of the z axis (i.e. the z value that will be
     * used if the user does not specify an explicit z value in a GetMap request).
     */
    double getDefaultZValue();

    String getId();

    /**
     * @return the last index on the t axis
     */
    int getLastTIndex();

    /**
     * @return a unique identifier string for this Layer object (used
     * in the display of Layers in a Capabilities document).
     */
    String getLayerName();

    /**
     * @return List of Styles that this layer can be rendered in.
     */
    List<Style> getSupportedStyles();

    /**
     * @return all the timesteps in this variable
     */
    List<TimestepInfo> getTimesteps();

    String getTitle();

    /**
     * @return array of timestep values in milliseconds since the epoch
     */
    long[] getTvalues();

    String getUnits();

    /**
     * @return the max value of the suggested scale range.  Note that this is not the
     * same as a "valid_max" for the dataset.  This is simply a hint to visualization
     * tools.
     */
    float getScaleMax();

    /**
     * @return the min value of the suggested scale range.  Note that this is not the
     * same as a "valid_min" for the dataset.  This is simply a hint to visualization
     * tools.
     */
    float getScaleMin();
    
    /**
     * @return array of two doubles, representing the min and max of the scale range
     * (i.e. new float[]{getScaleMin(), getScaleMax()})
     */
    float[] getScaleRange();

    CoordAxis getXaxis();

    CoordAxis getYaxis();

    /**
     * @return the projection for the data, as stored in the source files.
     */
    HorizontalProjection getHorizontalProjection();
    
    String getZunits();

    double[] getZvalues();

    /**
     * @return true if this variable can be queried through the GetFeatureInfo
     * function.  Delegates to Dataset.isQueryable().
     */
    boolean isQueryable();

    /**
     * @return true if this Layer has a time axis
     */
    boolean isTaxisPresent();

    /**
     * @return true if this Layer has a depth/elevation axis
     */
    boolean isZaxisPresent();

    boolean isZpositive();
    
    String getCopyrightStatement();
    
    /**
     * @return true if this Layer can be rendered in the style with the 
     * given name, false otherwise.
     */
    public boolean supportsStyle(String styleName);
    
}
