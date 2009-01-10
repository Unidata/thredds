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

import ucar.nc2.constants.AxisType;
import ucar.unidata.geoloc.ProjectionPoint;

/**
 * A Two-dimensional coordinate axis
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class TwoDCoordAxis extends CoordAxis
{
    
    /** Creates a new instance of TwoDCoordAxis */
    public TwoDCoordAxis(AxisType type)
    {
        super(type);
    }
    
    /**
     * Gets the index on this axis of the given point, expressed in the coordinate
     * system of the layer's horizontal projection (may equate to lon and lat).
     * @see uk.ac.rdg.resc.ncwms.datareader.PixelMap PixelMap
     */
    public int getIndex(ProjectionPoint point)
    {
        return this.getIndex(point.getX(), point.getY());
    }
    
    /**
     * Gets the index on this axis of the given point, expressed in the coordinate
     * system of the layer's horizontal projection (may equate to lon and lat).
     * @see uk.ac.rdg.resc.ncwms.datareader.PixelMap PixelMap
     */
    public abstract int getIndex(double x, double y);
    
}
