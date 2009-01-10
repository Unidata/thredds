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

import com.sleepycat.persist.model.Persistent;
import uk.ac.rdg.resc.ncwms.styles.Style;

/**
 * Implementation of a VectorLayer.  TODO: what to do about the read() methods?
 * As it stands they will read only from the eastward component.  Should they
 * return the magnitude of the layer?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Persistent
public class VectorLayerImpl extends LayerImpl implements VectorLayer
{
    private Layer northwardComponent;
    private Layer eastwardComponent;
    
    public VectorLayerImpl(String title, Layer eastwardComponent, Layer northwardComponent)
    {   
        // Copy the metadata from the eastward component
        // TODO: check that the two components match
        this.title = title;
        this.abstr = "Automatically-generated vector field, composed of the fields "
            + eastwardComponent.getTitle() + " and " + northwardComponent.getTitle();
        this.zUnits = eastwardComponent.getZunits();
        this.zValues = eastwardComponent.getZvalues();
        this.bbox = eastwardComponent.getBbox();
        this.xaxis = eastwardComponent.getXaxis();
        this.yaxis = eastwardComponent.getYaxis();
        this.dataset = eastwardComponent.getDataset();
        this.units = eastwardComponent.getUnits();
        this.timesteps = eastwardComponent.getTimesteps(); // Only used for metadata:
                                                  // have to be careful reading
                                                  // data as some datasets might
                                                  // store different variables in
                                                  // different files.
        
        // Vector is the default style, so we make sure it's at the head of the list
        this.supportedStyles.add(0, Style.VECTOR);
        
        this.eastwardComponent = eastwardComponent;
        this.northwardComponent = northwardComponent;
    }
    
    /**
     * Default constructor (used by Berkeley DB).  This can still be private
     * and apparently the Berkeley DB will get around this (we don't need public
     * setters for the fields for the same reason).
     */
    private VectorLayerImpl() {}

    public Layer getNorthwardComponent()
    {
        return this.northwardComponent;
    }

    public Layer getEastwardComponent()
    {
        return this.eastwardComponent;
    }
    
}
