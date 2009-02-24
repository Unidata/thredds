/*
 * Copyright (c) 2006 The University of Reading
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

import com.sleepycat.persist.model.Persistent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.metadata.projection.HorizontalProjection;
import uk.ac.rdg.resc.ncwms.styles.Style;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;
import ucar.nc2.dt.GridDataset;

/**
 * Concrete implementation of the Layer interface.  Stores the metadata for
 * a layer in the WMS
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Persistent // Berkeley DB code uses this
public class LayerImpl implements Layer
{

    protected String id;
    protected String title = null;
    protected String abstr = null; // "abstract" is a reserved word
    protected String units;
    protected String zUnits;
    protected double[] zValues;
    protected boolean zPositive;
    protected double[] bbox = new double[]{-180.0, -90.0, 180.0, 90.0}; // Bounding box : minx, miny, maxx, maxy
    protected float scaleMin;
    protected float scaleMax;
    protected CoordAxis xaxis;
    protected CoordAxis yaxis;
    private HorizontalProjection horizProj = HorizontalProjection.LON_LAT_PROJECTION;
    protected transient GridDataset dataset; // Not stored in the metadata database
    // Sorted in ascending order of time
    protected List<TimestepInfo> timesteps = new ArrayList<TimestepInfo>();
    // Stores the keys of the styles that this variable supports
    protected List<Style> supportedStyles = new ArrayList<Style>();
    
    /**
     * Creates a new Layer using a default bounding box (covering the whole 
     * earth) and with a default boxfill style
     */
    public LayerImpl()
    {
        this.supportedStyles.add(Style.BOXFILL);
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getAbstract()
    {
        return abstr;
    }

    public void setAbstract(String abstr)
    {
        this.abstr = abstr;
    }

    public String getZunits()
    {
        return zUnits;
    }

    public void setZunits(String zUnits)
    {
        this.zUnits = zUnits;
    }

    public double[] getZvalues()
    {
        return zValues;
    }

    public void setZvalues(double[] zValues)
    {
        this.zValues = zValues;
    }

    /**
     * @return array of timestep values in milliseconds since the epoch
     */
    public synchronized long[] getTvalues()
    {
        long[] tVals = new long[this.timesteps.size()];
        int i = 0;
        for (TimestepInfo tInfo : timesteps)
        {
            tVals[i] = tInfo.getDate().getTime();
            i++;
        }
        return tVals;
    }

    public double[] getBbox()
    {
        return bbox;
    }

    public void setBbox(double[] bbox)
    {
        this.bbox = bbox;
    }

    public boolean isZpositive()
    {
        return zPositive;
    }

    public void setZpositive(boolean zPositive)
    {
        this.zPositive = zPositive;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public float getScaleMin()
    {
        return scaleMin;
    }

    public void setScaleMin(float scaleMin)
    {
        this.scaleMin = scaleMin;
    }

    public float getScaleMax()
    {
        return scaleMax;
    }

    public void setScaleMax(float scaleMax)
    {
        this.scaleMax = scaleMax;
    }
    
    public float[] getScaleRange()
    {
        return new float[]{this.scaleMin, this.scaleMax};
    }

    public String getUnits()
    {
        return units;
    }

    public void setUnits(String units)
    {
        this.units = units;
    }

    public CoordAxis getXaxis()
    {
        return xaxis;
    }

    public void setXaxis(CoordAxis xaxis)
    {
        this.xaxis = xaxis;
    }

    public CoordAxis getYaxis()
    {
        return yaxis;
    }

    public void setYaxis(CoordAxis yaxis)
    {
        this.yaxis = yaxis;
    }

    public GridDataset getDataset()
    {
        return this.dataset;
    }

    public void setDataset(GridDataset dataset)
    {
        this.dataset = dataset;
    }
    
    /**
     * Adds a new TimestepInfo to this metadata object.  If a TimestepInfo object
     * already exists for this timestep, the TimestepInfo object with the lower
     * indexInFile value is chosen (this is most likely to be the result of a
     * shorter forecast lead time and therefore more accurate).
     */
    public synchronized void addTimestepInfo(TimestepInfo tInfo)
    {
        // See if we already have a TimestepInfo object for this date
        int tIndex = this.findTIndex(tInfo.getDate());
        if (tIndex < 0)
        {
            // We don't have an info for this date, so we add the new info
            // and make sure the List is sorted correctly (TODO: could do a
            // simple insertion into the correct locaion?)
            this.timesteps.add(tInfo);
            Collections.sort(this.timesteps);
        }
        else
        {
            // We already have a timestep for this time
            TimestepInfo existingTStep = this.timesteps.get(tIndex);
            if (tInfo.getIndexInFile() < existingTStep.getIndexInFile())
            {
                // The new info probably has a shorter forecast time and so we
                // replace the existing version with this one
                existingTStep = tInfo;
            }
        }
    }
    
    /**
     * @return the index of the TimestepInfo object corresponding with the given
     * date, or -1 if there is no TimestepInfo object corresponding with the
     * given date.  Uses binary search for efficiency.
     * @todo replace with Arrays.binarySearch()?
     */
    private int findTIndex(Date target)
    {
        if (this.timesteps.size() == 0) return -1;
        // Check that the point is within range
        if (target.before(this.timesteps.get(0).getDate()) ||
            target.after(this.timesteps.get(this.timesteps.size()  - 1).getDate()))
        {
            return -1;
        }
        
        // do a binary search to find the nearest index
        int low = 0;
        int high = this.timesteps.size() - 1;
        while (low <= high)
        {
            int mid = (low + high) >> 1;
            Date midVal = this.timesteps.get(mid).getDate();
            if (midVal.equals(target)) return mid;
            else if (midVal.before(target)) low = mid + 1;
            else high = mid - 1;
        }
        
        // If we've got this far we have to decide between values[low]
        // and values[high]
        if (this.timesteps.get(low).getDate().equals(target)) return low;
        else if (this.timesteps.get(high).getDate().equals(target)) return high;
        // The given time doesn't match any axis value
        return -1;
    }
    
    /**
     * @return the index of the TimestepInfo object corresponding with the given
     * ISO8601 time string. Uses binary search for efficiency.
     * @throws InvalidDimensionValueException if there is no corresponding
     * TimestepInfo object, or if the given ISO8601 string is not valid.  
     */
    public int findTIndex(String isoDateTime) throws InvalidDimensionValueException
    {
        if (isoDateTime.equals("current"))
        {
            // Return the last timestep
            // TODO: should be the index of the timestep closest to now
            return this.getLastTIndex();
        }
        Date target = WmsUtils.iso8601ToDate(isoDateTime);
        if (target == null)
        {
            throw new InvalidDimensionValueException("time", isoDateTime);
        }
        int index = findTIndex(target);
        if (index < 0)
        {
            throw new InvalidDimensionValueException("time", isoDateTime);
        }
        return index;
    }
    
    /**
     * Gets a List of integers representing indices along the time axis
     * starting from isoDateTimeStart and ending at isoDateTimeEnd, inclusive.
     * @param isoDateTimeStart ISO8601-formatted String representing the start time
     * @param isoDateTimeEnd ISO8601-formatted String representing the start time
     * @return List of Integer indices
     * @throws InvalidDimensionValueException if either of the start or end
     * values were not found in the axis, or if they are not valid ISO8601 times.
     */
    public List<Integer> findTIndices(String isoDateTimeStart,
        String isoDateTimeEnd) throws InvalidDimensionValueException
    {
        int startIndex = this.findTIndex(isoDateTimeStart);
        int endIndex = this.findTIndex(isoDateTimeEnd);
        if (startIndex > endIndex)
        {
            throw new InvalidDimensionValueException("time",
                isoDateTimeStart + "/" + isoDateTimeEnd);
        }
        List<Integer> tIndices = new ArrayList<Integer>();
        for (int i = startIndex; i <= endIndex; i++)
        {
            tIndices.add(i);
        }
        return tIndices;
    }
    
    /**
     * Finds the index of a certain z value by brute-force search.  We can afford
     * to be inefficient here because z axes are not likely to be large.
     * @param targetVal Value to search for
     * @return the z index corresponding with the given targetVal
     * @throws InvalidDimensionValueException if targetVal could not be found
     * within zValues
     */
    public int findZIndex(String targetVal) throws InvalidDimensionValueException
    {
        try
        {
            float zVal = Float.parseFloat(targetVal);
            for (int i = 0; i < this.zValues.length; i++)
            {
                // The fuzzy comparison fails for zVal == 0.0 so we do a direct
                // comparison too
                if (this.zValues[i] == zVal || Math.abs((this.zValues[i] - zVal) / zVal) < 1e-5)
                {
                    return i;
                }
            }
            throw new InvalidDimensionValueException("elevation", targetVal);
        }
        catch(NumberFormatException nfe)
        {
            throw new InvalidDimensionValueException("elevation", targetVal);
        }
    }

    /**
     * @return List of styles that this layer can be rendered in.
     */
    public List<Style> getSupportedStyles()
    {
        return this.supportedStyles;
    }
    
    /**
     * @return the key of the default style for this Variable.  Exactly 
     * equivalent to getSupportedStyles().get(0)
     */
    public Style getDefaultStyle()
    {
        // Could be an IndexOutOfBoundsException here, but would be a programming
        // error if so
        return this.supportedStyles.get(0);
    }
    
    /**
     * @return true if this Variable can be rendered in the style with the 
     * given name, false otherwise.
     */
    public boolean supportsStyle(String styleName)
    {
        return this.supportedStyles.contains(styleName.trim());
    }
    
    /**
     * @return true if this variable has a depth/elevation axis
     */
    public boolean isZaxisPresent()
    {
        return this.zValues != null && this.zValues.length > 0;
    }
    
    /**
     * @return true if this variable has a time axis
     */
    public boolean isTaxisPresent()
    {
        return this.getTimesteps() != null && this.getTimesteps().size() > 0;
    }
    
    /**
     * @return the index of the default value on the z axis (i.e. the index of
     * the z value that will be used if the user does not specify an explicit
     * z value in a GetMap request).
     */
    public int getDefaultZIndex()
    {
        return 0;
    }
    
    /**
     * @return the default value of the z axis (i.e. the z value that will be
     * used if the user does not specify an explicit z value in a GetMap request).
     */
    public final double getDefaultZValue()
    {
        return this.zValues[this.getDefaultZIndex()];
    }
    
    /**
     * @return the last index on the t axis
     */
    public int getLastTIndex()
    {
        return this.timesteps.size() - 1;
    }
    
    /**
     * @return the index of the default value of the t axis (i.e. the t value that will be
     * used if the user does not specify an explicit t value in a GetMap request),
     * as a TimestepInfo object.  This currently returns the last value along
     * the time axis, but should probably return the value closest to now.
     */
    public final int getDefaultTIndex()
    {
        return this.getLastTIndex();
    }
    
    /**
     * @return the default value of the t axis (i.e. the t value that will be
     * used if the user does not specify an explicit t value in a GetMap request),
     * in milliseconds since the epoch.  This currently returns the last value along
     * the time axis, but should probably return the value closest to now.
     */
    public final long getDefaultTValue()
    {
        return this.getTvalues()[this.getLastTIndex()];
    }
    
    /**
     * @return a unique identifier string for this LayerImpl object (used
     * in the display of Layers in a Capabilities document).
     */
    public String getLayerName()
    {
        return this.id;
    }
    
    /**
     * @return true if this variable can be queried through the GetFeatureInfo
     * function.  Delegates to Dataset.isQueryable().
     */
    public boolean isQueryable()
    {
        //return this.dataset.isQueryable();
        return true; //this is a bit dumb... sorry :P -Pauline.
    }
    
    /**
     * @return all the timesteps in this variable
     */
    public List<TimestepInfo> getTimesteps()
    {
        return timesteps;
    }

    /**
     * @return the projection for the data, as stored in the source files.  Defaults
     * to the lon-lat projection unless explicitly set.
     */
    public HorizontalProjection getHorizontalProjection()
    {
        return this.horizProj;
    }

    public void setHorizontalProjection(HorizontalProjection horizProj)
    {
        this.horizProj = horizProj;
    }

    /**
     * Gets the copyright statement for this layer, replacing ${year} as 
     * appropriate with the year range that this layer covers.
     * @return The copyright statement, or the empty string if no copyright
     * statement has been set.
     */
    public String getCopyrightStatement()
    {
        return "Copyright statement in the works..";
        /*String copyright = this.dataset.getCopyrightStatement();
        if (copyright == null || copyright.trim().equals(""))
        {
            return "";
        }
        else if (!this.isTaxisPresent())
        {
            return copyright;
        }
        // We (might) need to use the year range of the layer to generate
        // the final copyright statement.
        // This method shows just how horrible Java's Calendar/Date architecture is...
        // Joda-time would be a better alternative...
        Calendar cal = Calendar.getInstance(WmsUtils.GMT);
        cal.setTime(this.timesteps.get(0).getDate());
        int startYear = cal.get(Calendar.YEAR);
        cal.setTime(this.timesteps.get(this.timesteps.size() - 1).getDate());
        int endYear = cal.get(Calendar.YEAR);
        String yearStr = "" + startYear;
        if (startYear != endYear)
        {
            yearStr += "-" + endYear;
        }
        // Don't forget to escape dollar signs and backslashes in the regexp
        return copyright.replaceAll("\\$\\{year\\}", yearStr);*/
    }
}
