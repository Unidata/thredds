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
import ucar.nc2.constants.AxisType;
import ucar.unidata.geoloc.LatLonPoint;
import uk.ac.rdg.resc.ncwms.metadata.projection.HorizontalProjection;

/**
 * A regular, one-dimensional coordinate axis, whose values obey the rule
 * val(i) = start + stride * i, i.e. i = (val - start) / stride;
 *
 * @todo This class doesn't know anything about projections - is that OK from
 * the point of view of metadata storage?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Persistent
public class Regular1DCoordAxis extends OneDCoordAxis
{
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Regular1DCoordAxis.class);

    private double start;  // The first value along the axis
    private double stride; // The stride length along the axis
    private double maxValue; // The maximum value along the axis
    private boolean wraps; // True if this is a longitude axis that wraps the globe
    
    /**
     * Creates a Regular1DCoordAxis 
     */
    public Regular1DCoordAxis(double start, double stride, int count, AxisType axisType)
    {
        super(axisType, count);
        this.start = start;
        this.stride = stride;
        
        this.maxValue = this.start + this.stride * (this.size - 1);
        this.wraps = false;
        if (this.isLongitude())
        {
            Longitude st = new Longitude(this.start);
            Longitude mid = new Longitude(this.start + this.stride * this.size / 2);
            // Find the longitude of the point that is just off the end of the axis
            Longitude end = new Longitude(this.start + this.stride * this.size);
            logger.debug("Longitudes: st = {}, mid = {}, end = {}",
                new Object[]{st.getValue(), mid.getValue(), end.getValue()});
            // In some cases the end point might be past the original start point
            if (st.equals(end) || st.getClockwiseDistanceTo(mid) > st.getClockwiseDistanceTo(end))
            {
                this.wraps = true;
            }
        }
        logger.debug("Created regular {} axis, wraps = {}", this.getAxisType(), this.wraps);
    }
    
    /**
     * Gets the index of the given point. Uses index = (value - start) / stride,
     * hence this is faster than an exhaustive search.
     * @param coordValue The value along this coordinate axis
     * @return the index that is nearest to this point, or -1 if the point is
     * out of range for the axis
     */
    public int getIndex(double coordValue)
    {
        if (this.isLongitude())
        {
            logger.debug("Finding value for longitude {}", coordValue);
            Longitude lon = new Longitude(coordValue);
            if (this.wraps || lon.isBetween(this.start, this.maxValue))
            {
                Longitude startLon = new Longitude(this.start);
                double distance = startLon.getClockwiseDistanceTo(lon);
                double exactNumSteps = distance / this.stride;
                // This axis might wrap, so we make sure that the returned index
                // is within range
                int index = ((int)Math.round(exactNumSteps)) % this.size; 
                logger.debug("returning {}", index);
                return index;              
            }
            else
            {
                logger.debug("out of range: returning -1");
                return -1;
            }
        }
        else
        {
            if (logger.isDebugEnabled()) logger.debug("Finding value for {}, {}", this.getAxisType(), coordValue);
            // this is a latitude axis
            double distance = coordValue - this.start;
            double exactNumSteps = distance / this.stride;
            int index = (int)Math.round(exactNumSteps);
            if (logger.isDebugEnabled()) logger.debug("index = {}, count = {}", index, this.size);
            if (index < 0 || index >= this.size)
            {
                if (logger.isDebugEnabled()) logger.debug("returning -1");
                return -1;
            }
            if (logger.isDebugEnabled()) logger.debug("returning {}", index);
            return index;
        }
    }
    
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!(obj instanceof Regular1DCoordAxis)) return false;
        Regular1DCoordAxis otherAxis = (Regular1DCoordAxis)obj;
        
        return this.start == otherAxis.start &&
               this.stride == otherAxis.stride &&
               this.size == otherAxis.size &&
               this.getAxisType() == otherAxis.getAxisType();
    }
}
