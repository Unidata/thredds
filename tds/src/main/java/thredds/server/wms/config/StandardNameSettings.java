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
import ucar.nc2.units.SimpleUnit;
import ucar.units.ConversionException;
import uk.ac.rdg.resc.edal.util.Range;

/**
 * Encapsulates the setting per standard name
 * @author Jon
 */
class StandardNameSettings {

    private final String standardName;
    private final String units;
    private final LayerSettings settings;
    private final SimpleUnit nativeUnits;

    public StandardNameSettings(Element el) throws WmsConfigException
    {
        this.standardName = el.getAttributeValue("name");
        this.units = el.getAttributeValue("units");
        this.nativeUnits = SimpleUnit.factory(this.units);
        this.settings = new LayerSettings(el);
    }

    public String getStandardName() {
        return standardName;
    }

    /** Gets the settings as they are in the XML file */
    public LayerSettings getSettings() {
        return settings;
    }

    public String getUnits() {
        return units;
    }

}
