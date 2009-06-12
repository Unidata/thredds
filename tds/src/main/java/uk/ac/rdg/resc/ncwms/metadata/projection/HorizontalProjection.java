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

package uk.ac.rdg.resc.ncwms.metadata.projection;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;

/**
 * Abstract class describing a horizontal projection from lat-lon to projection
 * coordinates (i.e. the coordinates that are understood by the coordinate axes,
 * which are subclasses of CoordAxis).
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class HorizontalProjection
{
    /**
     * Singleton object representing the longitude-latitude projection
     */
    public static HorizontalProjection LON_LAT_PROJECTION = new HorizontalProjection()
        {
            public ProjectionPoint latLonToProj(LatLonPoint point)
            {
                return new ProjectionPointImpl(point.getLongitude(), point.getLatitude());
            }
            
            public boolean isLatLon()
            {
                return true;
            }
        };
    
    /**
     * Convenience static factory method to create a HorizontalProjection
     * from a ProjectionImpl object that is obtained from the Java NetCDF
     * library.
     */
    public static HorizontalProjection create(final ProjectionImpl proj)
    {
        return new HorizontalProjection()
        {
            public ProjectionPoint latLonToProj(LatLonPoint point)
            {
                // (Thanks to Marcos Hermida of Meteogalicia for helping to
                // sort out thread safety issues here.)
                // Apparently ProjectionImpls are not always thread-safe.
                synchronized(proj)
                {
                    // We need to create a new object each time because
                    // this is not guaranteed by proj.latLonToProj().
                    // now OK jc 06/11/09
                    return proj.latLonToProj(point);
                }
            }
            
            public boolean isLatLon()
            {
                return proj.isLatLon();
            }
        };
    }
    
    /**
     * Converts a latitude-longitude point to projection coordinates.
     * This is used by PixelMap to find the projection
     * coordinates before finding axis indices.  A new object must be
     * returned with each invocation to ensure thread safety.
     */
    public abstract ProjectionPoint latLonToProj(LatLonPoint point);
    
    /**
     * @return true if this is the lat-lon projection
     */
    public abstract boolean isLatLon();
}
