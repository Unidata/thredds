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

import uk.ac.rdg.resc.ncwms.utils.WmsUtils;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

/**
 * Object representing a request to the GetFeatureInfo operation.  This simply parses
 * the request and only does very basic sanity checking on the parameters 
 * (e.g. checking for valid integers).
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class GetFeatureInfoRequest
{
    private String wmsVersion;
    private GetFeatureInfoDataRequest dataRequest;
    private String outputFormat;
    
    /**
     * Creates a new instance of GetMapRequest from the given RequestParams
     * @throws WmsException if the request is invalid
     */
    public GetFeatureInfoRequest(RequestParams params) throws WmsException
    {
        this.wmsVersion = params.getMandatoryWmsVersion();
        if (!WmsUtils.SUPPORTED_VERSIONS.contains(this.wmsVersion))
        {
            throw new WmsException("VERSION " + this.wmsVersion + " not supported");
        }
        
        // TODO: deal with the EXCEPTIONS parameter
        this.dataRequest = new GetFeatureInfoDataRequest(params, this.wmsVersion);
        this.outputFormat = params.getMandatoryString("info_format");
    }

    /**
     * @return the portion of the GetMap request that pertains to the data
     * extraction, i.e. independent of styling concerns
     */
    public GetFeatureInfoDataRequest getDataRequest()
    {
        return dataRequest;
    }

    public String getOutputFormat()
    {
        return outputFormat;
    }

    public String getWmsVersion()
    {
        return wmsVersion;
    }
    
}
