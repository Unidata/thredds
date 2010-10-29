/*
 * Copyright (c) 2010 The University of Reading
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

package thredds.server.wms.config;

import org.jdom.Element;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.util.Ranges;

/**
 * Simple Java bean encapsulating the settings (allowFeatureInfo,
 * defaultColorScaleRange, defaultPaletteName and logScaling) at a particular
 * part of the config XML document.  A Null value for a field implies that
 * that field has not been set in the document and a default should be used.
 * @author Jon
 */
public class LayerSettings
{
    private Boolean allowFeatureInfo = null;
    private Range<Float> defaultColorScaleRange = null;
    private String defaultPaletteName =  null;
    private Boolean logScaling = null;
    private Integer defaultNumColorBands = null;

    LayerSettings(Element parentElement) throws WmsConfigException
    {
        if (parentElement == null) return; // Create a set of layer settings with all-null fields
        this.allowFeatureInfo = getBoolean(parentElement, "allowFeatureInfo");
        this.defaultColorScaleRange = getRange(parentElement, "defaultColorScaleRange");
        this.defaultPaletteName = parentElement.getChildTextTrim("defaultPaletteName");
        // If the default palette name tag is used, it must be populated
        // TODO: can we check this against the installed palettes?
        if (this.defaultPaletteName != null && this.defaultPaletteName.isEmpty())
        {
            throw new WmsConfigException("defaultPaletteName must contain a value");
        }
        this.defaultNumColorBands = getInteger(parentElement, "defaultNumColorBands",
                Ranges.newRange(5, ColorPalette.MAX_NUM_COLOURS));
        this.logScaling = getBoolean(parentElement, "logScaling");
    }

    /** Package-private constructor, sets all fields to null */
    LayerSettings() {}

    private static Boolean getBoolean(Element parentElement, String childName)
            throws WmsConfigException
    {
        String str = parentElement.getChildTextTrim(childName);
        if (str == null) return null;
        if (str.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (str.equalsIgnoreCase("false")) return Boolean.FALSE;
        throw new WmsConfigException("Value of " + childName + " must be true or false");
    }

    private static Integer getInteger(Element parentElement, String childName, Range<Integer> validRange)
            throws WmsConfigException
    {
        String str = parentElement.getChildTextTrim(childName);
        if (str == null) return null;
        int val;
        try
        {
            val = Integer.parseInt(str);
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsConfigException(nfe);
        }
        if (val < validRange.getMinimum()) return validRange.getMinimum();
        else if (val > validRange.getMaximum()) return validRange.getMaximum();
        else return val;
    }

    private static Range<Float> getRange(Element parentElement, String childName)
            throws WmsConfigException
    {
        String str = parentElement.getChildTextTrim(childName);
        if (str == null) return null;
        String[] els = str.split(" ");
        if (els.length != 2)
        {
            throw new WmsConfigException("Invalid range format");
        }
        try
        {
            float min = Float.parseFloat(els[0]);
            float max = Float.parseFloat(els[1]);
            return Ranges.newRange(min, max);
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsConfigException("Invalid floating-point value in range");
        }
    }

    public Boolean isAllowFeatureInfo() {
        return allowFeatureInfo;
    }

    public Range<Float> getDefaultColorScaleRange() {
        return defaultColorScaleRange;
    }

    public String getDefaultPaletteName() {
        return defaultPaletteName;
    }

    public Boolean isLogScaling() {
        return logScaling;
    }

    public Integer getDefaultNumColorBands() {
        return defaultNumColorBands;
    }

    /**
     * Replaces all unset values in this object with values from the given
     * LayerSettings object.
     */
    void replaceNullValues(LayerSettings newSettings)
    {
        if (this.allowFeatureInfo == null) this.allowFeatureInfo = newSettings.allowFeatureInfo;
        if (this.defaultColorScaleRange == null) this.defaultColorScaleRange = newSettings.defaultColorScaleRange;
        if (this.defaultPaletteName == null) this.defaultPaletteName = newSettings.defaultPaletteName;
        if (this.logScaling == null) this.logScaling = newSettings.logScaling;
        if (this.defaultNumColorBands == null) this.defaultNumColorBands = newSettings.defaultNumColorBands;
    }

    void setDefaultColorScaleRange(Range<Float> defaultColorScaleRange)
    {
        this.defaultColorScaleRange = defaultColorScaleRange;
    }

    @Override
    public String toString()
    {
        return String.format("allowFeatureInfo = %s, defaultColorScaleRange = %s, defaultPaletteName = %s, defaultNumColorBands = %s, logScaling = %s",
            this.allowFeatureInfo , this.defaultColorScaleRange, this.defaultPaletteName, this.defaultNumColorBands, this.logScaling);
    }
}
