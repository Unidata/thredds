/*
 * Copyright (c) 2008 The University of Reading
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

package uk.ac.rdg.resc.ncwms.controller;

import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

/**
 * The colour scale range that the WMS client has set, describing the data values
 * that correspond with the extremes of the colour palette.  Instances of this
 * class are immutable.
 * @author Jon
 */
public final class ColorScaleRange
{
    private boolean isDefault = false;
    private boolean isAuto = false;
    private float scaleMin = Float.NaN;
    private float scaleMax = Float.NaN;
    
    /**
     * Parses the given string and constructs a ColorScaleRange object.
     * @param str If this is a comma-separated pair of floating-point numbers,
     * with the first number being less than the second, then these are
     * converted to the scale range. If str is "default" or null then this is a signal
     * to use the default scale range for the layer. If str is "auto" then we
     * will automatically generate a scale for the image that is being requested,
     * maximizing the contrast of the scale.
     * @throws WmsException if none of the above conditions is met.
     */
    public ColorScaleRange(String str) throws WmsException
    {
        if (str == null)
        {
            this.isDefault = true;
            return;
        }
        str = str.trim();
        if (str.equalsIgnoreCase("default")) this.isDefault = true;
        else if (str.equalsIgnoreCase("auto")) this.isAuto = true;
        else
        {
            try
            {
                String[] scaleEls = str.split(",");
                if (scaleEls.length != 2) throw new Exception();
                this.scaleMin = Float.parseFloat(scaleEls[0]);
                this.scaleMax = Float.parseFloat(scaleEls[1]);
                if (this.scaleMin > this.scaleMax) throw new Exception();
            }
            catch(Exception e)
            {
                throw new WmsException("Invalid format for COLORSCALERANGE");
            }
        }
    }

    /**
     * @return true if we need to auto-scale the image, i.e. generate a colour
     * scale for maximum contrast of the image.
     */
    public boolean isAuto() {
        return isAuto;
    }

    /**
     * @return true if we are to use the default scale range for the image,
     * as defined by Layer.getScaleMin/Max()
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * @return The maximum value of the colour scale, or Float.NaN if we are
     * using an automatic or default scale.
     */
    public float getScaleMax() {
        return scaleMax;
    }

    /**
     * @return The maximum value of the colour scale, or Float.NaN if we are
     * using an automatic or default scale.
     */
    public float getScaleMin() {
        return scaleMin;
    }

}
