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

package uk.ac.rdg.resc.ncwms.controller;

import java.awt.Color;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

/**
 * Contains those portions of a GetMap request that pertain to styling and
 * image generation.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class GetMapStyleRequest
{
    private String[] styles;
    private String imageFormat;
    private boolean transparent;
    private Color backgroundColour;
    private int opacity; // Opacity of the image in the range [0,100]
    private int numColourBands; // Number of colour bands to use in the image
    private boolean logarithmic; // True if we're using a log scale
    // These are the data values that correspond with the extremes of the
    // colour scale
    private ColorScaleRange colorScaleRange;
    
    /**
     * Creates a new instance of GetMapStyleRequest from the given parameters
     * @throws WmsException if the request is invalid
     */
    public GetMapStyleRequest(RequestParams params) throws WmsException
    {
        // RequestParser replaces pluses with spaces: we must change back
        // to parse the format correctly
        String stylesStr = params.getMandatoryString("styles");
        if (stylesStr.trim().equals("")) this.styles = new String[0]; 
        else this.styles = stylesStr.split(",");
        
        this.imageFormat = params.getMandatoryString("format").replaceAll(" ", "+");
        
        String trans = params.getString("transparent", "false").toLowerCase();
        if (trans.equals("false")) this.transparent = false;
        else if (trans.equals("true")) this.transparent = true;
        else throw new WmsException("The value of TRANSPARENT must be \"TRUE\" or \"FALSE\"");
        
        try
        {
            String bgc = params.getString("bgcolor", "0xFFFFFF");
            if (bgc.length() != 8 || !bgc.startsWith("0x")) throw new Exception();
            // Parse the hexadecimal string, ignoring the "0x" prefix
            this.backgroundColour = new Color(Integer.parseInt(bgc.substring(2), 16));
        }
        catch(Exception e)
        {
            throw new WmsException("Invalid format for BGCOLOR");
        }
        
        this.opacity = params.getPositiveInt("opacity", 100);
        if (this.opacity > 100) this.opacity = 100;
        
        this.colorScaleRange = getColorScaleRange(params);
        this.numColourBands = getNumColourBands(params);
        this.logarithmic = isLogScale(params);
    }
    
    /**
     * Gets the ColorScaleRange object requested by the client
     */
    public static ColorScaleRange getColorScaleRange(RequestParams params) throws WmsException
    {
        return new ColorScaleRange(params.getString("colorscalerange"));
    }

    /**
     * Gets the number of colour bands requested by the client, or 254 if none
     * has been set or the requested number was bigger than 254.
     * @param params The RequestParams object from the client.
     * @return the requested number of colour bands, or 254 if none has been
     * set or the requested number was bigger than 254.
     * @throws WmsException if the client requested a negative number of colour
     * bands
     */
    public static int getNumColourBands(RequestParams params) throws WmsException
    {
        int numColourBands = params.getPositiveInt("numcolorbands", 254);
        // 254 is the maximum number of colours we can support in a palette.
        // One would be hard pushed to distinguish more colours than this in a
        // typical scenario anyway.
        if (numColourBands > 254) numColourBands = 254;
        return numColourBands;
    }
    
    /**
     * Returns true if the client has requested a logarithmic scale, false
     * otherwise.
     */
    public static boolean isLogScale(RequestParams params)
    {
        return params.getString("logscale", "false").equalsIgnoreCase("true");
    }

    /**
     * @return array of style names, or an empty array if the user specified
     * "STYLES="
     */
    public String[] getStyles()
    {
        return styles;
    }

    public String getImageFormat()
    {
        return imageFormat;
    }

    public boolean isScaleLogarithmic()
    {
        return this.logarithmic;
    }

    public boolean isTransparent()
    {
        return transparent;
    }

    public Color getBackgroundColour()
    {
        return backgroundColour;
    }

    public int getOpacity()
    {
        return opacity;
    }

    public ColorScaleRange getColorScaleRange()
    {
        return this.colorScaleRange;
    }


    public int getNumColourBands()
    {
        return numColourBands;
    }
}
